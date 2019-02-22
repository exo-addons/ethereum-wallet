package org.exoplatform.addon.ethereum.wallet.task.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.addon.ethereum.wallet.task.entity.WalletTaskEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class WalletTaskDAO extends GenericDAOJPAImpl<WalletTaskEntity, Long> {

  public List<WalletTaskEntity> listTasks(long identityId) {
    TypedQuery<WalletTaskEntity> query = getEntityManager().createNamedQuery("WalletTask.getUserTasks",
                                                                             WalletTaskEntity.class);
    query.setParameter("assignee", identityId);
    return query.getResultList();
  }

}
