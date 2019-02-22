package org.exoplatform.addon.ethereum.wallet.task.storage;

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.task.dao.WalletTaskDAO;
import org.exoplatform.addon.ethereum.wallet.task.entity.WalletTaskEntity;
import org.exoplatform.addon.ethereum.wallet.task.model.WalletAdminTask;

public class WalletTaskStorage {

  private WalletTaskDAO walletTaskDAO;

  public WalletTaskStorage(WalletTaskDAO walletTaskDAO) {
    this.walletTaskDAO = walletTaskDAO;
  }

  public Set<WalletAdminTask> listTasks(long identityId) {
    List<WalletTaskEntity> listTasks = walletTaskDAO.listTasks(identityId);
    return listTasks == null ? Collections.emptySet() : listTasks.stream().map(this::fromEntity).collect(Collectors.toSet());
  }

  private WalletAdminTask fromEntity(WalletTaskEntity entity) {
    WalletAdminTask walletAdminTask = new WalletAdminTask();
    walletAdminTask.setId(entity.getId());
    walletAdminTask.setMessage(entity.getMessage());
    walletAdminTask.setParameters(entity.getParameters());
    walletAdminTask.setCompleted(entity.getCompleted());
    walletAdminTask.setType(entity.getType());
    walletAdminTask.setMessage(entity.getMessage());
    walletAdminTask.setLink(entity.getLink());
    return walletAdminTask;
  }

}
