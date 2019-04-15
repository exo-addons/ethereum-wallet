package org.exoplatform.addon.ethereum.wallet.task.listener;

import static org.exoplatform.addon.ethereum.wallet.utils.WalletUtils.*;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.task.model.WalletAdminTask;
import org.exoplatform.addon.ethereum.wallet.task.service.WalletTaskService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A listener to creates/updates admin task when a new wallet is associated
 */
@Asynchronous
public class WalletAddressListener extends Listener<Wallet, Wallet> {

  private static final Log    LOG                                  = ExoLogger.getLogger(WalletAddressListener.class);

  private static final String WALLET_TASK_ADDRESS_PARAMETER_PREFIX = "address:";

  private PortalContainer     container;

  private WalletTaskService   walletTaskService;

  public WalletAddressListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Wallet, Wallet> event) throws Exception {
    Wallet wallet = event.getData();
    String addressParameter = WALLET_TASK_ADDRESS_PARAMETER_PREFIX + StringUtils.lowerCase(wallet.getAddress());

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String taskType = StringUtils.equals(event.getEventName(), NEW_ADDRESS_ASSOCIATED_EVENT) ? NEW_WALLET_TASK_TYPE
                                                                                               : MODIFY_WALLET_TASK_TYPE;
      Set<WalletAdminTask> adminTasks = getWalletTaskService().getTasksByType(taskType);
      WalletAdminTask adminTask = null;
      if (adminTasks != null && !adminTasks.isEmpty()) {
        adminTask = adminTasks.iterator().next();
        if (adminTasks.size() > 1) {
          LOG.warn("More than one task for type {} is retrieved from database", taskType);
        }
      }
      if (adminTask == null) {
        adminTask = new WalletAdminTask();
        adminTask.setType(taskType);
      }
      List<String> parameters = adminTask.getParameters();
      if (parameters == null) {
        parameters = new ArrayList<>();
        adminTask.setParameters(parameters);
      } else if (parameters.contains(addressParameter)) {
        // The address is already added in parameters
        return;
      }

      // Clear list of addresses if was marked as completed before
      if (adminTask.isCompleted()) {
        parameters.clear();
      }
      adminTask.setCompleted(false);

      // Add wallet address to the task to-do list
      parameters.add(addressParameter);

      getWalletTaskService().save(adminTask, null);
    } finally {
      RequestLifeCycle.end();
    }
  }

  public WalletTaskService getWalletTaskService() {
    if (walletTaskService == null) {
      walletTaskService = CommonsUtils.getService(WalletTaskService.class);
    }
    return walletTaskService;
  }

}
