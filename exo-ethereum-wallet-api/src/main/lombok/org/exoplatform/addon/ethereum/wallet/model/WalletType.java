package org.exoplatform.addon.ethereum.wallet.model;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;

public enum WalletType {
  USER,
  SPACE;

  private String id;

  private String providerId;

  private WalletType() {
    this.id = this.name().toLowerCase();
    this.providerId = this.ordinal() == 0 ? OrganizationIdentityProvider.NAME : SpaceIdentityProvider.NAME;
  }

  public String getId() {
    return id;
  }

  public String getProviderId() {
    return providerId;
  }

  public boolean isSpace() {
    return this == SPACE;
  }

  public boolean isUser() {
    return this == USER;
  }

  public static WalletType getType(String type) {
    return StringUtils.equals(SpaceIdentityProvider.NAME, type) || SPACE.name().equalsIgnoreCase(type) ? SPACE : USER;
  }

  public static boolean isSpace(String type) {
    return StringUtils.equals(SpaceIdentityProvider.NAME, type) || SPACE.name().equalsIgnoreCase(type);
  }

  public static boolean isUser(String type) {
    return StringUtils.equals(OrganizationIdentityProvider.NAME, type) || USER.name().equalsIgnoreCase(type);
  }

}
