package org.exoplatform.addon.ethereum.wallet.storage;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.formatTransactionHash;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.entity.TransactionEntity;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;

public class TransactionStorage {

  private static final long    MINIMUM_CREATED_DATE_MILLIS = ZonedDateTime
                                                                          .of(2018,
                                                                              1,
                                                                              1,
                                                                              0,
                                                                              0,
                                                                              0,
                                                                              0,
                                                                              ZoneId.systemDefault().normalized())
                                                                          .toEpochSecond()
      * 1000;

  private WalletTransactionDAO walletTransactionDAO;

  public TransactionStorage(WalletTransactionDAO walletTransactionDAO) {
    this.walletTransactionDAO = walletTransactionDAO;
  }

  public List<TransactionDetail> getPendingTransactions(long networkId) {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingTransactions(networkId);
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

  public List<TransactionDetail> getContractTransactions(long networkId, String contractAddress, int limit) {
    List<TransactionEntity> transactions = walletTransactionDAO.getContractTransactions(networkId,
                                                                                        StringUtils.lowerCase(contractAddress),
                                                                                        limit);
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

  public List<TransactionDetail> getWalletTransactions(long networkId,
                                                       String address,
                                                       String contractAddress,
                                                       String hash,
                                                       int limit,
                                                       boolean pending,
                                                       boolean administration) {

    address = StringUtils.lowerCase(address);
    List<TransactionEntity> transactions = walletTransactionDAO.getWalletTransactions(networkId,
                                                                                      address,
                                                                                      contractAddress,
                                                                                      limit,
                                                                                      pending,
                                                                                      administration);
    boolean limitNotReached = transactions != null && transactions.size() == limit;
    if (StringUtils.isNotBlank(hash) && limitNotReached
        && transactions.stream().noneMatch(transaction -> StringUtils.equalsIgnoreCase(transaction.getHash(), hash))) {
      return getWalletTransactions(networkId, address, contractAddress, hash, limit * 2, pending, administration);
    }
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());

  }

  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    TransactionEntity transactionEntity = toEntity(transactionDetail);
    if (transactionEntity.getId() == 0) {
      transactionEntity = walletTransactionDAO.create(transactionEntity);
      transactionDetail.setId(transactionEntity.getId());
    } else {
      walletTransactionDAO.update(transactionEntity);
    }
  }

  public TransactionDetail getAddressLastPendingTransactionSent(long networkId, String address) {
    TransactionEntity transactionEntity = walletTransactionDAO.getAddressLastPendingTransactionSent(networkId, address);
    return fromEntity(transactionEntity);
  }

  public TransactionDetail getTransactionByHash(String hash) {
    hash = formatTransactionHash(hash);
    TransactionEntity transactionEntity = walletTransactionDAO.getTransactionByHash(hash);
    return fromEntity(transactionEntity);
  }

  private TransactionDetail fromEntity(TransactionEntity entity) {
    if (entity == null) {
      return null;
    }
    TransactionDetail detail = new TransactionDetail();
    detail.setId(entity.getId());
    detail.setAdminOperation(entity.isAdminOperation());
    detail.setContractAddress(entity.getContractAddress());
    detail.setContractAmount(entity.getContractAmount());
    detail.setContractMethodName(entity.getContractMethodName());
    detail.setTimestamp(entity.getCreatedDate());

    // Workaround for old bug when adding timestamp in seconds
    if (entity.getCreatedDate() > 0 && entity.getCreatedDate() < MINIMUM_CREATED_DATE_MILLIS) {
      detail.setTimestamp(entity.getCreatedDate() * 1000);
    }

    detail.setHash(entity.getHash());
    detail.setFrom(entity.getFromAddress());
    detail.setTo(entity.getToAddress());
    detail.setBy(entity.getByAddress());
    detail.setLabel(entity.getLabel());
    detail.setMessage(entity.getMessage());
    detail.setNetworkId(entity.getNetworkId());
    detail.setPending(entity.isPending());
    detail.setSucceeded(entity.isSuccess());
    detail.setValue(entity.getValue());
    return detail;
  }

  private TransactionEntity toEntity(TransactionDetail transactionDetail) {
    TransactionEntity transactionEntity = new TransactionEntity();
    if (transactionDetail.getId() > 0) {
      transactionEntity.setId(transactionDetail.getId());
    }
    transactionEntity.setNetworkId(transactionDetail.getNetworkId());
    transactionEntity.setHash(formatTransactionHash(transactionDetail.getHash()));
    transactionEntity.setFromAddress(StringUtils.lowerCase(transactionDetail.getFrom()));
    transactionEntity.setToAddress(StringUtils.lowerCase(transactionDetail.getTo()));
    transactionEntity.setByAddress(StringUtils.lowerCase(transactionDetail.getBy()));
    transactionEntity.setContractAddress(StringUtils.lowerCase(transactionDetail.getContractAddress()));
    transactionEntity.setContractAmount(transactionDetail.getContractAmount());
    transactionEntity.setContractMethodName(transactionDetail.getContractMethodName());
    transactionEntity.setAdminOperation(transactionDetail.isAdminOperation());
    transactionEntity.setLabel(transactionDetail.getLabel());
    transactionEntity.setMessage(transactionDetail.getMessage());
    transactionEntity.setPending(transactionDetail.isPending());
    transactionEntity.setSuccess(transactionDetail.isSucceeded());
    transactionEntity.setValue(transactionDetail.getValue());
    transactionEntity.setCreatedDate(transactionDetail.getTimestamp());
    return transactionEntity;
  }

}
