package org.exoplatform.addon.ethereum.wallet.migration;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletAccountService;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.addon.ethereum.wallet.storage.TransactionStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MigrationService implements Startable {

  private static final Log                LOG                 = ExoLogger.getLogger(MigrationService.class);

  private static final int                GLOBAL_DATA_VERSION = 3;

  private DeprecatedEthereumWalletService deprecatedEthereumWalletService;

  private EthereumWalletService           ethereumWalletService;

  private EthereumWalletAccountService    accountService;

  private TransactionStorage              transactionStorage;

  private EthereumTransactionDecoder      transactionDecoder;

  private UserACL                         userACL;

  private ExoContainer                    container;

  private ExecutorService                 migrationExecutorService;

  private Set<String>                     transactionHashes   = new HashSet<>();

  public MigrationService(ExoContainer container,
                          EthereumWalletService ethereumWalletService,
                          EthereumWalletAccountService accountService,
                          TransactionStorage transactionStorage,
                          EthereumTransactionDecoder transactionDecoder,
                          UserACL userACL) {
    this.userACL = userACL;
    this.container = container;
    this.ethereumWalletService = ethereumWalletService;
    this.transactionDecoder = transactionDecoder;
    this.transactionStorage = transactionStorage;
    this.accountService = accountService;
  }

  @Override
  public void start() {
    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Ethereum-migration-%d").build();
    migrationExecutorService = Executors.newSingleThreadExecutor(namedThreadFactory);
    // Transactions Queue processing
    migrationExecutorService.execute(() -> {
      GlobalSettings settings = getEthereumWalletService().getSettings();

      Integer dataVersion = settings.getDataVersion();
      if (dataVersion != null && dataVersion >= GLOBAL_DATA_VERSION) {
        return;
      }

      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(this.container);
      try {
        String superUser = this.userACL.getSuperUser();
        Set<Wallet> listUserWallets = null;
        int migratedWallets = 0;
        boolean hasWalletMigrationErrors = false;
        LOG.debug("Check list of user wallets to migrate");
        try {
          listUserWallets = getDeprecatedWalletService().listUserWallets();
          LOG.info("{} user wallets to migrate", listUserWallets.size());
          migratedWallets = migrateWallets(listUserWallets, superUser);
          hasWalletMigrationErrors = migratedWallets != listUserWallets.size();
        } catch (Exception e) {
          LOG.error("Error while migrating users wallets", e);
          hasWalletMigrationErrors = true;
        }

        Set<Wallet> listSpaceWallets = null;
        LOG.debug("Check list of space wallets to migrate");
        try {
          listSpaceWallets = getDeprecatedWalletService().listSpacesWallets();
          LOG.info("{} space wallets to migrate", listSpaceWallets.size());
          migratedWallets = migrateWallets(listSpaceWallets, superUser);
          hasWalletMigrationErrors |= migratedWallets != listUserWallets.size();
        } catch (Exception e) {
          LOG.error("Error while migrating spaces wallets", e);
          hasWalletMigrationErrors = true;
        }
        LOG.info("{}/{} space wallets are migrated", migratedWallets, listSpaceWallets.size());

        Long networkId = settings.getDefaultNetworkId();

        LOG.info("Migrate user wallet transactions");
        hasWalletMigrationErrors |= migrateTransactions(listUserWallets, networkId);

        LOG.info("Migrate space wallets transactions");
        hasWalletMigrationErrors |= migrateTransactions(listSpaceWallets, networkId);

        String principalContractAddress = settings.getDefaultPrincipalAccount();
        if (StringUtils.isNotBlank(principalContractAddress)) {
          hasWalletMigrationErrors |= migrateContractTransactions(principalContractAddress, networkId);
        }

        hasWalletMigrationErrors |= !retrieveTransactionDetailsFromBlockchain();

        if (hasWalletMigrationErrors) {
          LOG.info("Wallet and Transactions migration seems to have some errors, it will be reattempted again next startup");
        } else {
          LOG.info("Wallet and Transactions migration finished susccessfully");
        }
        ethereumWalletService.saveSettings(settings, GLOBAL_DATA_VERSION);

        Set<Wallet> allWallets = listUserWallets;
        allWallets.addAll(listSpaceWallets);

      } finally {
        RequestLifeCycle.end();
      }
    });
  }

  private boolean retrieveTransactionDetailsFromBlockchain() {
    int computedTransactions = 0;
    int errorComputedTransactions = 0;
    LOG.debug("Compute {} saved transactions details from blockchain", transactionHashes.size());
    for (String hash : transactionHashes) {
      TransactionDetail transactionDetail = transactionStorage.getTransactionByHash(hash, false);
      try {
        transactionDecoder.computeTransactionDetail(transactionDetail);
        transactionStorage.saveTransactionDetail(transactionDetail);
      } catch (InterruptedException | ExecutionException e) { // NOSONAR
        // Reattempt to compute details
        try {
          transactionDecoder.computeTransactionDetail(transactionDetail);
          transactionStorage.saveTransactionDetail(transactionDetail);
        } catch (InterruptedException | ExecutionException e1) { // NOSONAR
          errorComputedTransactions++;
          LOG.warn("Can't compute transaction details with hash: " + transactionDetail.getHash());
        }
      }
      computedTransactions++;
    }
    LOG.info("{} transactions has been computed from blockchain with treatment errors = {}",
             computedTransactions,
             errorComputedTransactions);
    return errorComputedTransactions > 0;
  }

  private boolean migrateContractTransactions(String principalContractAddress, Long networkId) {
    LOG.info("Migrate contract {} transactions", principalContractAddress);
    List<TransactionDetail> contractTransactions = getDeprecatedWalletService().getAccountTransactions(networkId,
                                                                                                       principalContractAddress);

    boolean migrationHasErrors = false;
    for (TransactionDetail transactionDetail : contractTransactions) {
      String hash = transactionDetail.getHash();
      LOG.debug("Migrate contract transaction {}", hash);
      try {
        transactionDetail.setContractAddress(principalContractAddress);
        migrateTransaction(principalContractAddress, transactionDetail);
      } catch (Exception e) {
        migrationHasErrors = true;
        LOG.error("Error while migrating principal contract transaction {}", hash, e);
      }
    }
    return migrationHasErrors;
  }

  private boolean migrateTransactions(Set<Wallet> listWallets, Long networkId) {
    boolean migrationHasErrors = false;
    for (Wallet wallet : listWallets) {
      try {
        List<TransactionDetail> accountTransactions = getDeprecatedWalletService().getAccountTransactions(networkId,
                                                                                                          wallet.getAddress());
        LOG.debug("    -- migrate wallet {} transactions of {} {}", accountTransactions.size(), wallet.getType(), wallet.getId());
        for (TransactionDetail transaction : accountTransactions) {
          try { // NOSONAR
            migrateTransaction(wallet.getAddress(), transaction);
          } catch (Exception e) {
            LOG.warn("Error while migrating address {} transaction {}", wallet.getAddress(), transaction.getHash(), e);
            migrationHasErrors = true;
          }
        }
      } catch (Exception e) {
        LOG.warn("Error while migrating {} {} transactions", wallet.getType(), wallet.getId(), e);
        migrationHasErrors = true;
      }
    }
    return migrationHasErrors;
  }

  private void migrateTransaction(String address, TransactionDetail transaction) {
    String hash = transaction.getHash();
    transactionHashes.add(transaction.getHash().toLowerCase());
    TransactionDetail migratedTransaction = getTransactionStorage().getTransactionByHash(hash, false);
    if (migratedTransaction == null) {
      transaction.setFrom(address);
      getTransactionStorage().saveTransactionDetail(transaction);
    } else {
      if (transaction.getTo() == null) {
        transaction.setTo(address);
      }
      if (StringUtils.isBlank(migratedTransaction.getLabel())) {
        migratedTransaction.setLabel(transaction.getLabel());
      }
      if (StringUtils.isBlank(migratedTransaction.getMessage())) {
        migratedTransaction.setMessage(transaction.getMessage());
      }
      getTransactionStorage().saveTransactionDetail(migratedTransaction);
    }
  }

  private int migrateWallets(Set<Wallet> listWallets, String superUser) {
    int migratedWallets = 0;
    for (Wallet wallet : listWallets) {
      if (StringUtils.isBlank(wallet.getAddress())) {
        LOG.warn("Wallet address of {} / {} wasn't found, skip", wallet.getType(), wallet.getId());
        continue;
      }
      try {
        LOG.debug("    -- migrate wallet of {} {}", wallet.getType(), wallet.getId());
        getAccountService().saveWallet(wallet, superUser, false);
        migratedWallets++;
      } catch (Exception e) {
        LOG.warn("Error while migrating wallet {}", wallet, e);
      }
    }
    return migratedWallets;
  }

  @Override
  public void stop() {
    if (migrationExecutorService != null) {
      migrationExecutorService.shutdownNow();
    }
  }

  public EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }

  public EthereumWalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(EthereumWalletAccountService.class);
    }
    return accountService;
  }

  public TransactionStorage getTransactionStorage() {
    if (transactionStorage == null) {
      transactionStorage = CommonsUtils.getService(TransactionStorage.class);
    }
    return transactionStorage;
  }

  public DeprecatedEthereumWalletService getDeprecatedWalletService() {
    if (deprecatedEthereumWalletService == null) {
      deprecatedEthereumWalletService = CommonsUtils.getService(DeprecatedEthereumWalletService.class);
    }
    return deprecatedEthereumWalletService;
  }
}
