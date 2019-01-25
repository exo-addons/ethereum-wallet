package org.exoplatform.addon.ethereum.wallet.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.ethereum.wallet.entity.WalletEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletAccountDAO extends GenericDAOJPAImpl<WalletEntity, Long> {

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(List<WalletEntity> entities) {
    throw new UnsupportedOperationException();
  }

  public WalletEntity findByAddress(String address) {
    TypedQuery<WalletEntity> query = getEntityManager().createNamedQuery("Wallet.findByAddress",
                                                                         WalletEntity.class);
    query.setParameter("address", address);
    List<WalletEntity> resultList = query.getResultList();
    return resultList == null || resultList.isEmpty() ? null : resultList.get(0);
  }

}
