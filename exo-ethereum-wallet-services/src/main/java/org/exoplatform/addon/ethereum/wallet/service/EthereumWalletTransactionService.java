package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.storage.TransactionStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class EthereumWalletTransactionService {

  private static final Log              LOG = ExoLogger.getLogger(EthereumWalletTransactionService.class);

  private TransactionStorage            walletTransactionStorage;

  private EthereumWalletAccountService  walletAccountService;

  private EthereumWalletContractService contractService;

  private SpaceService                  spaceService;

  private ListenerService               listenerService;

  private long                          knownTreatedTransactionsCount;

  public EthereumWalletTransactionService(EthereumWalletAccountService walletAccountService,
                                          TransactionStorage walletTransactionStorage,
                                          EthereumWalletContractService contractService) {
    this.walletTransactionStorage = walletTransactionStorage;
    this.walletAccountService = walletAccountService;
    this.contractService = contractService;
  }

  public List<TransactionDetail> getPendingTransactions() {
    return walletTransactionStorage.getPendingTransactions();
  }

  public List<TransactionDetail> getTransactions(long networkId,
                                                 String address,
                                                 String contractAddress,
                                                 String hash,
                                                 int limit,
                                                 boolean pending,
                                                 boolean administration,
                                                 String accessor) throws IllegalAccessException {
    if (contractService.isContract(address, networkId)) {
      return getContractTransactions(networkId, address, limit, accessor);
    } else {
      return getWalletTransactions(networkId, address, contractAddress, hash, limit, pending, administration, accessor);
    }
  }

  public TransactionDetail getTransactionByHash(String hash, boolean onlyPending) {
    return walletTransactionStorage.getTransactionByHash(hash, onlyPending);
  }

  /**
   * Save temporary transaction label and message and save transaction hash in
   * sender and receiver account
   *
   * @param transactionDetail
   * @param modifierUsername
   * @param transactionConfirmed
   * @throws IllegalAccessException
   */
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    String modifierUsername,
                                    boolean transactionConfirmed) throws IllegalAccessException {
    if (!transactionConfirmed) {
      String senderAddress = StringUtils.isBlank(transactionDetail.getBy()) ? transactionDetail.getFrom()
                                                                            : transactionDetail.getBy();
      Wallet senderWallet = walletAccountService.getWalletByAddress(senderAddress);
      if (senderWallet != null) {
        walletAccountService.checkCanSaveWallet(senderWallet, senderWallet, modifierUsername);
      }
    }
    walletTransactionStorage.saveTransactionDetail(transactionDetail);
    if (transactionConfirmed) {
      broadcastNewTransactionEvent(transactionDetail);
    }
  }

  public long getKnownTreatedTransactionsCount() {
    return knownTreatedTransactionsCount;
  }

  /**
   * Get list of transactions for a contract
   * 
   * @param networkId
   * @param contractAddress
   * @param limit
   * @param accessor
   * @return
   */
  private List<TransactionDetail> getContractTransactions(Long networkId,
                                                          String contractAddress,
                                                          int limit,
                                                          String accessor) throws IllegalAccessException {
    ContractDetail contractDetail = contractService.getContractDetail(contractAddress, networkId);
    if (contractDetail == null) {
      throw new IllegalStateException("Can't find contract with address " + contractAddress);
    }

    if (!isUserAdmin(accessor) && !isUserRewardingAdmin(accessor)) {
      throw new IllegalAccessException("User " + accessor + " attempts to access contract transactions with address "
          + contractAddress);
    }

    List<TransactionDetail> transactionDetails = walletTransactionStorage.getContractTransactions(networkId,
                                                                                                  contractAddress,
                                                                                                  limit);
    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, accessor));
    return transactionDetails;
  }

  /**
   * Get list of transactions for a wallet designated by an address
   * 
   * @param networkId
   * @param address
   * @param accessor
   * @param limit
   * @param hash
   * @param isAdministration
   * @param accessor2
   * @param administration
   * @return
   */
  private List<TransactionDetail> getWalletTransactions(long networkId,
                                                        String address,
                                                        String contractAddress,
                                                        String hash,
                                                        int limit,
                                                        boolean pending,
                                                        boolean administration,
                                                        String accessor) throws IllegalAccessException {
    Wallet wallet = walletAccountService.getWalletByAddress(address);
    if (wallet == null) {
      return Collections.emptyList();
    }
    if (!walletAccountService.canAccessWallet(wallet, accessor)) {
      throw new IllegalAccessException("Can't access wallet with address " + address);
    }

    List<TransactionDetail> transactionDetails = walletTransactionStorage.getWalletTransactions(networkId,
                                                                                                address,
                                                                                                contractAddress,
                                                                                                hash,
                                                                                                limit,
                                                                                                pending,
                                                                                                administration);

    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, accessor));
    return transactionDetails;
  }

  private void retrieveWalletsDetails(TransactionDetail transactionDetail, String accessor) {
    Wallet senderWallet = walletAccountService.getWalletByAddress(transactionDetail.getFrom());
    transactionDetail.setFromWallet(senderWallet);
    if (senderWallet != null) {
      senderWallet.setPassPhrase(null);
    }
    if (StringUtils.isNotBlank(transactionDetail.getTo())) {
      Wallet receiverWallet = walletAccountService.getWalletByAddress(transactionDetail.getTo());
      if (receiverWallet != null) {
        receiverWallet.setPassPhrase(null);
      }
      transactionDetail.setToWallet(receiverWallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getBy())) {
      Wallet senderWalletBy = walletAccountService.getWalletByAddress(transactionDetail.getBy());
      if (senderWalletBy != null) {
        senderWalletBy.setPassPhrase(null);
      }
      transactionDetail.setByWallet(senderWalletBy);
      if (!displayTransactionsLabel(senderWalletBy, accessor)) {
        transactionDetail.setLabel(null);
      }
    } else if (!displayTransactionsLabel(senderWallet, accessor)) {
      transactionDetail.setLabel(null);
    }
  }

  private boolean displayTransactionsLabel(Wallet senderWallet, String currentUserId) {
    if (senderWallet == null) {
      return isUserAdmin(currentUserId);
    }
    String accountId = senderWallet.getId();
    String accountType = senderWallet.getType();
    if (StringUtils.isBlank(accountId) || StringUtils.isBlank(accountType)) {
      return isUserAdmin(currentUserId);
    }

    if (WalletType.isSpace(senderWallet.getType())) {
      if (getSpaceService().isSuperManager(currentUserId)) {
        return true;
      }
      Space space = getSpace(accountId);
      return space != null && getSpaceService().isManager(space, currentUserId);
    } else {
      return StringUtils.equalsIgnoreCase(accountId, currentUserId);
    }
  }

  private void broadcastNewTransactionEvent(TransactionDetail transactionDetail) {
    try {
      JSONObject transaction = new JSONObject();
      transaction.put("hash", transactionDetail.getHash());
      transaction.put("address", transactionDetail.getFrom());
      transaction.put("status", transactionDetail.isSucceeded());
      getListenerService().broadcast(KNOWN_TRANSACTION_MINED_EVENT, null, transaction);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting transaction mined event: {}", transactionDetail, e);
    }
    this.knownTreatedTransactionsCount++;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = CommonsUtils.getService(SpaceService.class);
    }
    return spaceService;
  }

  private ListenerService getListenerService() {
    if (listenerService == null) {
      listenerService = CommonsUtils.getService(ListenerService.class);
    }
    return listenerService;
  }

}
