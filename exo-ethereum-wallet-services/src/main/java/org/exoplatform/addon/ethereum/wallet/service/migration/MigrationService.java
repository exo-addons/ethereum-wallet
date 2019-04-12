package org.exoplatform.addon.ethereum.wallet.service.migration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MigrationService implements Startable {

  private static final int              ETHER_TO_WEI_DECIMALS = 18;

  private static final Log              LOG                   = ExoLogger.getLogger(MigrationService.class);

  private static final int              GLOBAL_DATA_VERSION   = 6;

  private ExecutorService               migrationExecutorService;

  private PortalContainer               container;

  private WalletTransactionService      transactionService;

  private WalletTokenTransactionService tokenTransactionService;

  private WalletAccountService          accountService;

  private EthereumWalletService         ethereumWalletService;

  public MigrationService(PortalContainer container) {
    this.container = container;
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
      LOG.info("Migrate ether transactions values");
      boolean hasErrors = false;
      RequestLifeCycle.begin(this.container);
      try {
        List<TransactionDetail> etherTransactions = getTransactionService().getEtherTransactions();
        if (etherTransactions != null && !etherTransactions.isEmpty()) {
          for (TransactionDetail transactionDetail : etherTransactions) {
            if (transactionDetail.getValue() > 0) {
              transactionDetail.setValueDecimal(BigDecimal.valueOf(transactionDetail.getValue()).toBigInteger(),
                                                ETHER_TO_WEI_DECIMALS);
              getTransactionService().saveTransactionDetail(transactionDetail, false);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Error while migrating ether transactions values", e);
        hasErrors = true;
      } finally {
        RequestLifeCycle.end();
      }
      LOG.info("Ether transactions migration finished successfully");

      LOG.info("Migrate wallet initialization flags");
      RequestLifeCycle.begin(this.container);
      try {
        Set<Wallet> listWallets = getAccountService().listWallets();
        if (listWallets != null && !listWallets.isEmpty()) {
          for (Wallet wallet : listWallets) {
            if (wallet == null || StringUtils.isBlank(wallet.getAddress())) {
              continue;
            }
            if (getTokenTransactionService().isApprovedAccount(wallet.getAddress())) {
              getAccountService().setInitializationStatus(wallet.getAddress(), WalletInitializationState.INITIALIZED);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Error while migrating wallet initialization flags", e);
        hasErrors = true;
      } finally {
        RequestLifeCycle.end();
      }
      LOG.info("Wallet initialization flags migration finished successfully");

      if (hasErrors) {
        LOG.info("Ether wallets and transactions migration seems to have some errors, it will be reattempted again next startup");
      } else {
        LOG.info("Ether wallets and transactions migration has finished susccessfully");
        RequestLifeCycle.begin(this.container);
        try {
          ethereumWalletService.saveSettings(settings, GLOBAL_DATA_VERSION);
        } finally {
          RequestLifeCycle.end();
        }
      }
    });
  }

  @Override
  public void stop() {
    migrationExecutorService.shutdown();
  }

  private EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }

  private WalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(WalletAccountService.class);
    }
    return accountService;
  }

  private WalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(WalletTransactionService.class);
    }
    return transactionService;
  }

  public WalletTokenTransactionService getTokenTransactionService() {
    if (tokenTransactionService == null) {
      tokenTransactionService = CommonsUtils.getService(WalletTokenTransactionService.class);
    }
    return tokenTransactionService;
  }
}
