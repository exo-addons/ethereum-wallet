package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.SPACE_ACCOUNT_TYPE;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.getSpaceId;

import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;

public class SpaceListener extends SpaceListenerPlugin {

  private EthereumWalletService ethereumWalletService;

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    getEthereumWalletService().removeFromCache(SPACE_ACCOUNT_TYPE, space.getPrettyName());
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    getEthereumWalletService().removeFromCache(SPACE_ACCOUNT_TYPE, getSpaceId(space));
    getEthereumWalletService().removeFromCache(SPACE_ACCOUNT_TYPE, space.getPrettyName());
    // Populate cache
    getEthereumWalletService().getSpaceAddress(space.getPrettyName());
  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void applicationActivated(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void left(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void spaceDescriptionEdited(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  @Override
  public void spaceBannerEdited(SpaceLifeCycleEvent event) {
    // No implementation is required here
  }

  public EthereumWalletService getEthereumWalletService() {
    if (ethereumWalletService == null) {
      ethereumWalletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return ethereumWalletService;
  }

}
