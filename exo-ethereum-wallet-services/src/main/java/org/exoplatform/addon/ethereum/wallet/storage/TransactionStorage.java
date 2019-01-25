package org.exoplatform.addon.ethereum.wallet.storage;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.entity.TransactionEntity;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;

public class TransactionStorage {

  private WalletTransactionDAO walletTransactionDAO;

  public TransactionStorage(WalletTransactionDAO walletTransactionDAO) {
    this.walletTransactionDAO = walletTransactionDAO;
  }

  public List<TransactionDetail> getPendingTransactions() {
    List<TransactionEntity> transactions = walletTransactionDAO.getPendingTransactions();
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

  public List<TransactionDetail> getContractTransactions(long networkId, String contractAddress) {
    List<TransactionEntity> transactions = walletTransactionDAO.getContractTransactions(networkId, contractAddress);
    return transactions == null ? Collections.emptyList()
                                : transactions.stream().map(this::fromEntity).collect(Collectors.toList());
  }

  public List<TransactionDetail> getWalletTransactions(Long networkId, String address) {
    List<TransactionEntity> transactions = walletTransactionDAO.getWalletTransactions(networkId, address);
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

  public TransactionDetail getTransactionByHash(String hash, boolean onlyPending) { // NOSONAR
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
    detail.setHash(entity.getHash());
    detail.setFrom(entity.getFromAddress());
    detail.setTo(entity.getToAddress());
    detail.setLabel(entity.getLabel());
    detail.setMessage(entity.getMessage());
    detail.setNetworkId(entity.getNetworkId());
    detail.setPending(entity.isPending());
    detail.setValue(entity.getValue());
    return detail;
  }

  private TransactionEntity toEntity(TransactionDetail transactionDetail) {
    TransactionEntity transactionEntity = new TransactionEntity();
    if (transactionDetail.getId() > 0) {
      transactionEntity.setId(transactionDetail.getId());
    }
    transactionEntity.setAdminOperation(transactionDetail.isAdminOperation());
    transactionEntity.setContractAddress(StringUtils.lowerCase(transactionDetail.getContractAddress()));
    transactionEntity.setContractAmount(transactionDetail.getContractAmount());
    transactionEntity.setContractMethodName(transactionDetail.getContractMethodName());
    transactionEntity.setCreatedDate(transactionDetail.getTimestamp());
    transactionEntity.setFromAddress(StringUtils.lowerCase(transactionDetail.getFrom()));
    transactionEntity.setToAddress(StringUtils.lowerCase(transactionDetail.getTo()));
    transactionEntity.setHash(StringUtils.lowerCase(transactionDetail.getHash()));
    transactionEntity.setLabel(transactionDetail.getLabel());
    transactionEntity.setMessage(transactionDetail.getMessage());
    transactionEntity.setNetworkId(transactionDetail.getNetworkId());
    transactionEntity.setPending(transactionDetail.isPending());
    transactionEntity.setValue(transactionDetail.getValue());
    return transactionEntity;
  }

}
