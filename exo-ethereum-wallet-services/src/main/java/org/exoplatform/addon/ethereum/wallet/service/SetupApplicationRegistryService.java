package org.exoplatform.addon.ethereum.wallet.service;

import java.util.*;

import org.picocontainer.Startable;

import org.exoplatform.application.registry.*;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.ApplicationType;

/**
 * This Service installs application registry category and application for
 * EthereumSpaceWallet portlet if the application registry was already populated
 */
public class SetupApplicationRegistryService implements Startable {

  private static final String        WALLET_CATEGORY_NAME = "EthereumWallet";

  private ExoContainer               container;

  private ApplicationRegistryService applicationRegistryService;

  public SetupApplicationRegistryService(ExoContainer container, ApplicationRegistryService applicationRegistryService) {
    this.container = container;
    this.applicationRegistryService = applicationRegistryService;
  }

  @Override
  public void start() {
    RequestLifeCycle.begin(container);
    try {
      ApplicationCategory applicationCategory = applicationRegistryService.getApplicationCategory(WALLET_CATEGORY_NAME);
      if (applicationCategory == null) {
        applicationCategory = new ApplicationCategory();
        applicationCategory.setAccessPermissions(Arrays.asList(new String[] { "Everyone" }));
        applicationCategory.setName(WALLET_CATEGORY_NAME);
        applicationCategory.setDescription("Ethereum Wallet");
        applicationCategory.setDisplayName("Ethereum Wallet");
        Application application = new Application();
        applicationCategory.setApplications(Collections.singletonList(application));
        application.setAccessPermissions(new ArrayList<String>(Arrays.asList(new String[] { "Everyone" })));
        application.setDisplayName("Ethereum Space Wallet");
        application.setDescription("Ethereum Space Wallet");
        application.setApplicationName("EthereumSpaceWallet");
        application.setCategoryName(WALLET_CATEGORY_NAME);
        application.setContentId("exo-ethereum-wallet/EthereumSpaceWallet");
        application.setType(ApplicationType.PORTLET);

        applicationRegistryService.save(applicationCategory);
        applicationRegistryService.save(applicationCategory, application);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
  }
}
