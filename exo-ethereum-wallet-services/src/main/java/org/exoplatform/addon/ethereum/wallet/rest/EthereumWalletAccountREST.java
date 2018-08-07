package org.exoplatform.addon.ethereum.wallet.rest;

import static org.exoplatform.addon.ethereum.wallet.rest.Utils.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.UserPreferences;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * This class provide a REST endpoint to retrieve detailed inormation about
 * users and spaces with the associated Ethereum account addresses
 */
@Path("/wallet/api/account")
@RolesAllowed("users")
public class EthereumWalletAccountREST implements ResourceContainer {

  public static final Log LOG = ExoLogger.getLogger(EthereumWalletAccountREST.class);

  private SettingService  settingService;

  private IdentityManager identityManager;

  private SpaceService    spaceService;

  public EthereumWalletAccountREST(SpaceService spaceService, SettingService settingService, IdentityManager identityManager) {
    this.settingService = settingService;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
  }

  /**
   * Retrieves the user or space details by username or spacePrettyName
   * 
   * @return
   * @throws Exception
   */
  @Path("detailsById")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccountByTypeAndID(@QueryParam("id") String id, @QueryParam("type") String type) throws Exception {
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)
        || !(StringUtils.equals(type, "user") || StringUtils.equals(type, "space"))) {
      LOG.warn("Bad request sent to server with id '{}' and type '{}'", id, type);
      return Response.status(400).build();
    }
    if (StringUtils.equals(type, "user")) {
      AccountDetail accountDetail = getUserDetails(id);
      if (accountDetail == null) {
        LOG.warn("User not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(getUserAddress(id));
      return Response.ok(accountDetail).build();
    } else if (StringUtils.equals(type, "space")) {
      AccountDetail accountDetail = getSpaceDetails(id);
      if (accountDetail == null) {
        LOG.warn("Space not found with id '{}'", id);
        return Response.status(400).build();
      }
      accountDetail.setAddress(getSpaceAddress(id));
      return Response.ok(accountDetail).build();
    }

    return Response.ok("{}").build();
  }

  /**
   * Retrieves the user or space details associated to an address
   * 
   * @return
   * @throws Exception
   */
  @Path("detailsByAddress")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccountByAddress(@QueryParam("address") String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      LOG.warn("Bad request sent to server with empty address", address);
      return Response.status(400).build();
    }

    SettingValue<?> walletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, address);
    if (walletAddressValue != null && walletAddressValue.getValue() != null) {
      String idAndType = walletAddressValue.getValue().toString();
      String id = null;
      AccountDetail accountDetail = null;
      if (idAndType.startsWith("user")) {
        id = idAndType.replace("user", "");
        accountDetail = getUserDetails(id);
      } else if (idAndType.startsWith("space")) {
        id = idAndType.replace("space", "");
        accountDetail = getSpaceDetails(id);
      }
      if (accountDetail == null) {
        return Response.status(400).build();
      }
      accountDetail.setAddress(address);
      return Response.ok(accountDetail).build();
    }

    return Response.ok("{}").build();
  }

  /**
   * Save address a user or space associated address
   * 
   * @param address
   * @return
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("saveAddress")
  public Response saveAddress(AccountDetail accountDetail) {
    if (accountDetail == null) {
      LOG.warn("Bad request sent to server with empty data");
      return Response.status(400).build();
    }

    String id = accountDetail.getId();
    String type = accountDetail.getType();
    String address = accountDetail.getAddress();

    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)
        || !(StringUtils.equals(type, "user") || StringUtils.equals(type, "space"))) {
      LOG.warn("Bad request sent to server with id '{}', type '{}' and address '{}'", id, type, address);
      return Response.status(400).build();
    }

    String currentUserId = getCurrentUserId();
    if (StringUtils.equals(type, "user")) {
      if (!StringUtils.equals(currentUserId, id)) {
        LOG.error("User '{}' attempts to modify wallet address of user '{}'", currentUserId, id);
        return Response.status(403).build();
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      String oldAddress = getUserAddress(id);
      if (oldAddress != null) {
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }
      settingService.set(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME, SettingValue.create(address));
    } else if (StringUtils.equals(type, "space")) {
      Space space = getSpace(id);
      if (space == null) {
        LOG.warn("Space not found with id '{}'", id);
        return Response.status(400).build();
      }
      if (!spaceService.isManager(space, currentUserId) && !spaceService.isSuperManager(currentUserId)) {
        LOG.error("User '{}' attempts to modify wallet address of space '{}'", currentUserId, space.getDisplayName());
        return Response.status(403).build();
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, address, SettingValue.create(type + id));
      String oldAddress = getSpaceAddress(id);
      if (oldAddress != null) {
        // Remove old address mapping
        settingService.remove(WALLET_CONTEXT, WALLET_SCOPE, oldAddress);
      }
      settingService.set(WALLET_CONTEXT, WALLET_SCOPE, id, SettingValue.create(address));
    }
    return Response.ok().build();
  }

  /**
   * Save user preferences of Wallet
   * 
   * @param defaultGas default gas limit to use in transactions
   * @return
   * @throws Exception
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("savePreferences")
  public Response savePreferences(UserPreferences userPreferences) throws Exception {
    if (userPreferences == null) {
      LOG.warn("Bad request sent to server with empty preferenes");
      return Response.status(400).build();
    }
    Integer defaultGas = userPreferences.getDefaultGas();
    if (defaultGas == null || defaultGas == 0) {
      LOG.warn("Bad request sent to server with invalid preferenes defaultGas '{}'", defaultGas);
      return Response.status(400).build();
    }
    String currentUserId = getCurrentUserId();

    settingService.set(Context.USER.id(currentUserId),
                       WALLET_SCOPE,
                       SETTINGS_KEY_NAME,
                       SettingValue.create(userPreferences.toJSONString()));
    return Response.ok().build();
  }

  private AccountDetail getSpaceDetails(String id) {
    Space space = getSpace(id);
    if (space == null) {
      return null;
    }
    AccountDetail accountDetail = new AccountDetail(id,
                                                    "space",
                                                    space.getDisplayName(),
                                                    null,
                                                    space.getManagers(),
                                                    space.getAvatarUrl());
    return accountDetail;
  }

  private Space getSpace(String id) {
    Space space = spaceService.getSpaceByPrettyName(id);
    if (space == null) {
      space = spaceService.getSpaceByUrl(id);
      if (space == null) {
        space = spaceService.getSpaceByDisplayName(id);
      }
    }
    if (space == null) {
      return null;
    }
    return space;
  }

  private AccountDetail getUserDetails(String id) {
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
    if (identity == null || identity.getProfile() == null) {
      return null;
    }

    String avatarUrl = identity.getProfile().getAvatarUrl();
    if (StringUtils.isBlank(avatarUrl)) {
      avatarUrl = "/rest/v1/social/users/" + id + "/avatar";
    }
    AccountDetail accountDetail = new AccountDetail(id, "user", identity.getProfile().getFullName(), null, null, avatarUrl);
    return accountDetail;
  }

  private String getSpaceAddress(String id) {
    SettingValue<?> spaceWalletAddressValue = settingService.get(WALLET_CONTEXT, WALLET_SCOPE, id);
    String address = null;
    if (spaceWalletAddressValue != null && spaceWalletAddressValue.getValue() != null) {
      address = spaceWalletAddressValue.getValue().toString();
    }
    return address;
  }

  private String getUserAddress(String id) {
    SettingValue<?> userWalletAddressValue = settingService.get(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME);
    String address = null;
    if (userWalletAddressValue != null && userWalletAddressValue.getValue() != null) {
      address = userWalletAddressValue.getValue().toString();
    }
    return address;
  }
}
