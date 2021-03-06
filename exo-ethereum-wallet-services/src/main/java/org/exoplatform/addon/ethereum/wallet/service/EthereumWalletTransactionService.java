package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.utils.WalletUtils.*;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.storage.TransactionStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class EthereumWalletTransactionService implements WalletTransactionService {

  private static final Log      LOG = ExoLogger.getLogger(EthereumWalletTransactionService.class);

  private WalletAccountService  accountService;

  private WalletContractService contractService;

  private TransactionStorage    transactionStorage;

  private SpaceService          spaceService;

  private ListenerService       listenerService;

  private long                  watchedTreatedTransactionsCount;

  private long                  pendingTransactionMaxDays;

  public EthereumWalletTransactionService(WalletAccountService accountService,
                                          TransactionStorage transactionStorage,
                                          WalletContractService contractService,
                                          InitParams params) {
    this.transactionStorage = transactionStorage;
    this.accountService = accountService;
    this.contractService = contractService;

    if (params != null && params.containsKey(TRANSACTION_PENDING_MAX_DAYS)) {
      String value = params.getValueParam(TRANSACTION_PENDING_MAX_DAYS).getValue();
      this.pendingTransactionMaxDays = Long.parseLong(value);
    }
  }

  @Override
  public List<TransactionDetail> getPendingTransactions(long networkId) {
    return transactionStorage.getPendingTransactions(networkId);
  }

  @Override
  public Set<String> getPendingTransactionHashes(long networkId) {
    List<TransactionDetail> pendingTransactions = getPendingTransactions(networkId);
    if (pendingTransactions == null || pendingTransactions.isEmpty()) {
      return Collections.emptySet();
    }
    return pendingTransactions.stream().map(transactionDetail -> transactionDetail.getHash()).collect(Collectors.toSet());
  }

  @Override
  public List<TransactionDetail> getEtherTransactions() {
    return transactionStorage.getEtherTransactions();
  }

  @Override
  public List<TransactionDetail> getTransactions(long networkId,
                                                 String address,
                                                 String contractAddress,
                                                 String hash,
                                                 int limit,
                                                 boolean onlyPending,
                                                 boolean administration,
                                                 String currentUser) throws IllegalAccessException {
    if (contractService.isContract(address, networkId)) {
      return getContractTransactions(networkId, address, limit, currentUser);
    } else {
      return getWalletTransactions(networkId, address, contractAddress, hash, limit, onlyPending, administration, currentUser);
    }
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash) {
    return transactionStorage.getTransactionByHash(hash);
  }

  @Override
  public TransactionDetail getAddressLastPendingTransactionSent(long networkId,
                                                                String address,
                                                                String currentUser) throws IllegalAccessException {
    Wallet wallet = accountService.getWalletByAddress(address);
    if (wallet == null) {
      return null;
    }
    if (!canAccessWallet(wallet, currentUser)) {
      throw new IllegalAccessException("Can't access wallet with address " + address);
    }
    return transactionStorage.getAddressLastPendingTransactionSent(networkId, address);
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    boolean broadcastMinedTransaction) {
    transactionStorage.saveTransactionDetail(transactionDetail);
    if (broadcastMinedTransaction) {
      broadcastTransactionMinedEvent(transactionDetail);
    }
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail,
                                    String currentUser,
                                    boolean broadcastMinedTransaction) throws IllegalAccessException {
    if (!broadcastMinedTransaction) {
      String senderAddress = StringUtils.isBlank(transactionDetail.getBy()) ? transactionDetail.getFrom()
                                                                            : transactionDetail.getBy();
      Wallet senderWallet = accountService.getWalletByAddress(senderAddress);
      if (senderWallet != null) {
        accountService.checkCanSaveWallet(senderWallet, senderWallet, currentUser);
      }
    }
    transactionStorage.saveTransactionDetail(transactionDetail);
    if (broadcastMinedTransaction) {
      broadcastTransactionMinedEvent(transactionDetail);
    }
  }

  @Override
  public long getWatchedTreatedTransactionsCount() {
    return watchedTreatedTransactionsCount;
  }

  @Override
  public long getPendingTransactionMaxDays() {
    return pendingTransactionMaxDays;
  }

  private List<TransactionDetail> getContractTransactions(Long networkId,
                                                          String contractAddress,
                                                          int limit,
                                                          String currentUser) throws IllegalAccessException {
    ContractDetail contractDetail = contractService.getContractDetail(contractAddress, networkId);
    if (contractDetail == null) {
      throw new IllegalStateException("Can't find contract with address " + contractAddress);
    }

    if (!isUserAdmin(currentUser) && !isUserRewardingAdmin(currentUser)) {
      throw new IllegalAccessException("User " + currentUser + " attempts to access contract transactions with address "
          + contractAddress);
    }

    List<TransactionDetail> transactionDetails = transactionStorage.getContractTransactions(networkId,
                                                                                            contractAddress,
                                                                                            limit);
    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, currentUser));
    return transactionDetails;
  }

  private List<TransactionDetail> getWalletTransactions(long networkId,
                                                        String address,
                                                        String contractAddress,
                                                        String hash,
                                                        int limit,
                                                        boolean pending,
                                                        boolean administration,
                                                        String currentUser) throws IllegalAccessException {
    Wallet wallet = accountService.getWalletByAddress(address);
    if (wallet == null) {
      return Collections.emptyList();
    }
    if (!canAccessWallet(wallet, currentUser)) {
      throw new IllegalAccessException("Can't access wallet with address " + address);
    }

    List<TransactionDetail> transactionDetails = transactionStorage.getWalletTransactions(networkId,
                                                                                          address,
                                                                                          contractAddress,
                                                                                          hash,
                                                                                          limit,
                                                                                          pending,
                                                                                          administration);

    transactionDetails.stream().forEach(transactionDetail -> retrieveWalletsDetails(transactionDetail, currentUser));
    return transactionDetails;
  }

  private void retrieveWalletsDetails(TransactionDetail transactionDetail, String currentUser) {
    Wallet senderWallet = accountService.getWalletByAddress(transactionDetail.getFrom());
    transactionDetail.setFromWallet(senderWallet);
    hideWalletOwnerPrivateInformation(senderWallet);
    if (StringUtils.isNotBlank(transactionDetail.getTo())) {
      Wallet receiverWallet = accountService.getWalletByAddress(transactionDetail.getTo());
      hideWalletOwnerPrivateInformation(receiverWallet);
      transactionDetail.setToWallet(receiverWallet);
    }
    if (StringUtils.isNotBlank(transactionDetail.getBy())) {
      Wallet senderWalletBy = accountService.getWalletByAddress(transactionDetail.getBy());
      hideWalletOwnerPrivateInformation(senderWalletBy);
      transactionDetail.setByWallet(senderWalletBy);
      if (!displayTransactionsLabel(senderWalletBy, currentUser)) {
        transactionDetail.setLabel(null);
      }
    } else if (!displayTransactionsLabel(senderWallet, currentUser)) {
      transactionDetail.setLabel(null);
    }
    if (transactionDetail.getIssuerId() > 0 && (isUserAdmin(currentUser) || isUserRewardingAdmin(currentUser))) {
      Wallet issuerWallet = accountService.getWalletByIdentityId(transactionDetail.getIssuerId());
      transactionDetail.setIssuer(issuerWallet);
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

  private void broadcastTransactionMinedEvent(TransactionDetail transactionDetail) {
    try {
      JSONObject transaction = new JSONObject();
      transaction.put("hash", transactionDetail.getHash());
      transaction.put("address", transactionDetail.getFrom());
      transaction.put("status", transactionDetail.isSucceeded());
      getListenerService().broadcast(KNOWN_TRANSACTION_MINED_EVENT, null, transaction);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting transaction mined event: {}", transactionDetail, e);
    }
    this.watchedTreatedTransactionsCount++;
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
