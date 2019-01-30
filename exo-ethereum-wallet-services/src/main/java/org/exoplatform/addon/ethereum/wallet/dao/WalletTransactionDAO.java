package org.exoplatform.addon.ethereum.wallet.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.ethereum.wallet.entity.TransactionEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletTransactionDAO extends GenericDAOJPAImpl<TransactionEntity, Long> {

  private static final String NETWORK_ID_PARAM = "networkId";

  public List<TransactionEntity> getContractTransactions(long networkId, String contractAddress, int limit) {
    TypedQuery<TransactionEntity> query = getEntityManager().createNamedQuery("WalletTransaction.getContractTransactions",
                                                                              TransactionEntity.class);
    query.setParameter(NETWORK_ID_PARAM, networkId);
    query.setParameter("contractAddress", contractAddress.toLowerCase());
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<TransactionEntity> getWalletTransactions(long networkId,
                                                       String address,
                                                       String contractAddress,
                                                       int limit,
                                                       boolean pending,
                                                       boolean administration) {

    StringBuilder queryString = new StringBuilder("SELECT tx FROM WalletTransaction tx WHERE tx.networkId = ");
    queryString.append(networkId);

    if (!administration) {
      queryString.append(" AND tx.isAdminOperation = FALSE");
    }

    queryString.append(" AND (tx.fromAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.toAddress = '");
    queryString.append(address);
    queryString.append("' OR tx.byAddress = '");
    queryString.append(address);
    queryString.append("')");

    if (pending) {
      queryString.append(" AND tx.isPending = TRUE");
    }

    if (StringUtils.isNotBlank(contractAddress)) {
      queryString.append(" AND tx.contractAddress = '");
      queryString.append(contractAddress);
      queryString.append("' ");
    }

    queryString.append(" ORDER BY tx.createdDate DESC");
    TypedQuery<TransactionEntity> query = getEntityManager().createQuery(queryString.toString(), TransactionEntity.class);
    if (limit > 0) {
      query.setMaxResults(limit);
    }
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
