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
    return fromEntity(listTasks);
  }

  public void save(WalletAdminTask task, long assigneeIdentityId) {
    WalletTaskEntity taskEntity = new WalletTaskEntity();
    if (task.getId() <= 0) {
      taskEntity.setId(null);
    } else {
      taskEntity.setId(task.getId());
    }
    taskEntity.setCompleted(task.isCompleted());
    taskEntity.setParameters(task.getParameters());
    taskEntity.setLink(task.getLink());
    taskEntity.setType(task.getType());
    taskEntity.setAssignee(assigneeIdentityId);
    if (taskEntity.getId() == null) {
      walletTaskDAO.create(taskEntity);
    } else {
      walletTaskDAO.update(taskEntity);
    }
  }

  public Set<WalletAdminTask> getTasksByType(String taskType) {
    List<WalletTaskEntity> listTasks = walletTaskDAO.getTasksByType(taskType);
    return fromEntity(listTasks);
  }

  public void markCompleted(long taskId) {
    WalletTaskEntity task = walletTaskDAO.find(taskId);
    if (task == null) {
      throw new IllegalStateException("Task with id " + taskId + " wasn't found");
    }
    task.setCompleted(true);
    walletTaskDAO.update(task);
  }

  private Set<WalletAdminTask> fromEntity(List<WalletTaskEntity> listTasks) {
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
