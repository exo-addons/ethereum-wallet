package org.exoplatform.addon.ethereum.wallet.storage;

import static org.exoplatform.addon.ethereum.wallet.utils.WalletUtils.formatTransactionHash;

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

  @Deprecated
  public List<TransactionDetail> getEtherTransactions() {
    List<TransactionEntity> transactions = walletTransactionDAO.getEtherTransactions();
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
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
    detail.setIssuerId(entity.getIssuerIdentityId());

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

  private TransactionEntity toEntity(TransactionDetail detail) {
    TransactionEntity entity = new TransactionEntity();
    if (detail.getId() > 0) {
      entity.setId(detail.getId());
    }
    entity.setNetworkId(detail.getNetworkId());
    entity.setHash(formatTransactionHash(detail.getHash()));
    entity.setFromAddress(StringUtils.lowerCase(detail.getFrom()));
    entity.setToAddress(StringUtils.lowerCase(detail.getTo()));
    entity.setByAddress(StringUtils.lowerCase(detail.getBy()));
    entity.setContractAddress(StringUtils.lowerCase(detail.getContractAddress()));
    entity.setContractAmount(detail.getContractAmount());
    entity.setContractMethodName(detail.getContractMethodName());
    entity.setAdminOperation(detail.isAdminOperation());
    entity.setLabel(detail.getLabel());
    entity.setMessage(detail.getMessage());
    entity.setPending(detail.isPending());
    entity.setSuccess(detail.isSucceeded());
    entity.setValue(detail.getValue());
    entity.setCreatedDate(detail.getTimestamp());
    if (detail.getIssuer() != null) {
      entity.setIssuerIdentityId(detail.getIssuer().getTechnicalId());
    } else if (detail.getIssuerId() > 0) {
      entity.setIssuerIdentityId(detail.getIssuerId());
    }
    return entity;
  }

}
