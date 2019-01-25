package org.exoplatform.addon.ethereum.wallet.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.ethereum.wallet.entity.TransactionEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletTransactionDAO extends GenericDAOJPAImpl<TransactionEntity, Long> {

  public List<TransactionEntity> getContractTransactions(long networkId, String contractAddress) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getContractTransactions",
                                                                              TransactionEntity.class);
    query.setParameter("networkId", networkId);
    query.setParameter("contractAddress", contractAddress.toLowerCase());
    return query.getResultList();
  }

  public List<TransactionEntity> getWalletTransactions(Long networkId, String address) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getWalletTransactions",
                                                                              TransactionEntity.class);
    query.setParameter("networkId", networkId);
    query.setParameter("address", address.toLowerCase());
    return query.getResultList();
  }

  public List<TransactionEntity> getPendingTransactions() {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getPendingTransactions",
                                                                              TransactionEntity.class);
    return query.getResultList();
  }

  public TransactionEntity getTransactionByHash(String hash) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getTransactionByHash",
                                                                              TransactionEntity.class);
    query.setParameter("hash", hash.toLowerCase());
    List<TransactionEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

}
