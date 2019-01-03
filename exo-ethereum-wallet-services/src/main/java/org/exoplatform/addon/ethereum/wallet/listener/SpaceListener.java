package org.exoplatform.addon.ethereum.wallet.listener;
import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleListener;

public class SpaceListener implements SpaceLifeCycleListener {

  private EthereumWalletService ethereumWalletService;

  public SpaceListener(EthereumWalletService ethereumWalletService) {
    this.ethereumWalletService = ethereumWalletService;
  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    this.ethereumWalletService.removeFromCache(SPACE_ACCOUNT_TYPE, space.getPrettyName());
  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    this.ethereumWalletService.removeFromCache(SPACE_ACCOUNT_TYPE, getSpaceId(space));
    this.ethereumWalletService.removeFromCache(SPACE_ACCOUNT_TYPE, space.getPrettyName());
    // Populate cache
    this.ethereumWalletService.getSpaceAddress(space.getPrettyName());
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

}
