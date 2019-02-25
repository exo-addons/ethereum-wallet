/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.ethereum.wallet.task.service;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.getIdentityByTypeAndId;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.WalletType;
import org.exoplatform.addon.ethereum.wallet.task.model.WalletAdminTask;
import org.exoplatform.addon.ethereum.wallet.task.storage.WalletTaskStorage;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * A storage service to save/load wallet admin tasks
 */
public class WalletTaskServiceImpl implements WalletTaskService {

  private WalletTaskStorage walletTaskStorage;

  public WalletTaskServiceImpl(WalletTaskStorage walletTaskStorage) {
    this.walletTaskStorage = walletTaskStorage;
  }

  @Override
  public Set<WalletAdminTask> listTasks(String userId) {
    long identityId = getUserIdentityId(userId);
    return walletTaskStorage.listTasks(identityId);
  }

  @Override
  public Set<WalletAdminTask> getTasksByType(String taskType) {
    return walletTaskStorage.getTasksByType(taskType);
  }

  @Override
  public void save(WalletAdminTask task, String assignee) {
    long assigneeIdentityId = 0;
    if (StringUtils.isNotBlank(assignee)) {
      assigneeIdentityId = getUserIdentityId(assignee);
    }
    walletTaskStorage.save(task, assigneeIdentityId);
  }

  @Override
  public void markCompleted(long taskId) {
    walletTaskStorage.markCompleted(taskId);
  }

  private long getUserIdentityId(String userId) {
    Identity identity = getIdentityByTypeAndId(WalletType.USER, userId);
    return Long.parseLong(identity.getId());
  }

}
