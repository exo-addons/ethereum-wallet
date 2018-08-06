package org.exoplatform.addon.ethereum.wallet;

import java.io.Serializable;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
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

  public static final Log     LOG               = ExoLogger.getLogger(EthereumWalletAccountREST.class);

  public static final String  SCOPE_NAME        = "ADDONS_ETHEREUM_WALLET";

  public static final String  ADDRESS_KEY_NAME  = "ADDONS_ETHEREUM_WALLET_ADDRESS";

  public static final String  SETTINGS_KEY_NAME = "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context WALLET_CONTEXT    = Context.GLOBAL;

  public static final Scope   WALLET_SCOPE      = Scope.APPLICATION.id(SCOPE_NAME);

  private SettingService      settingService;

  private IdentityManager     identityManager;

  private SpaceService        spaceService;

  public EthereumWalletAccountREST(SpaceService spaceService, SettingService settingService, IdentityManager identityManager) {
    this.settingService = settingService;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
  }

  /**
   * Retrieves the list of default contracts displayed in all users wallet
   * 
   * @return
   * @throws Exception
   */
  @Path("detailsById")
  @GET()
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAccountByTypeAndID(@QueryParam("id") String id, @QueryParam("type") String type) throws Exception {
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type) || !StringUtils.equals(type, "user")
        || !StringUtils.equals(type, "space")) {
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
    LOG.warn("Bad request sent to server with id '{}' and type '{}'", id, type);
    return Response.status(400).build();
  }

  /**
   * Retrieves the list of default contracts displayed in all users wallet
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

    return Response.status(400).build();
  }

  /**
   * Save a new contract address to display it in wallet of all users
   * 
   * @param address
   * @return
   */
  @POST
  @Path("save")
  @SuppressWarnings("deprecation")
  public Response saveAddress(@FormParam("type") String type, @FormParam("id") String id, @FormParam("address") String address) {
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type) || StringUtils.isBlank(address) || !StringUtils.equals(type, "user")
        || !StringUtils.equals(type, "space")) {
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
      if (!StringUtils.equals(space.getCreator(), currentUserId)) {
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
   * Save a new contract address to display it in wallet of all users
   * 
   * @param address
   * @return
   * @throws Exception 
   */
  @POST
  @Path("save")
  public Response savePreferences(@FormParam("defaultGas") String defaultGas) throws Exception {
    if (StringUtils.isBlank(defaultGas)) {
      LOG.warn("Bad request sent to server with preferenes defaultGas '{}'", defaultGas);
      return Response.status(400).build();
    }
    String currentUserId = getCurrentUserId();

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("defaultGas", defaultGas);

    settingService.set(Context.USER.id(currentUserId),
                       WALLET_SCOPE,
                       SETTINGS_KEY_NAME,
                       SettingValue.create(jsonObject.toString()));
    return Response.ok().build();
  }

  @SuppressWarnings("deprecation")
  private AccountDetail getSpaceDetails(String id) {
    Space space = getSpace(id);
    if (space == null) {
      return null;
    }
    AccountDetail accountDetail = new AccountDetail(id,
                                                    "space",
                                                    space.getDisplayName(),
                                                    null,
                                                    space.getCreator(),
                                                    space.getAvatarUrl());
    return accountDetail;
  }

  private String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
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
    if (identity == null) {
      return null;
    }

    AccountDetail accountDetail = new AccountDetail(id,
                                                    "user",
                                                    identity.getProfile().getFullName(),
                                                    null,
                                                    null,
                                                    identity.getProfile().getAvatarUrl());
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

  public static final class AccountDetail implements Serializable {

    private static final long serialVersionUID = 8011288624609310945L;

    private String            id;

    private String            type;

    private String            name;

    private String            address;

    private String            creator;

    private String            avatar;

    public AccountDetail() {
    }

    public AccountDetail(String id, String type, String name, String address, String creator, String avatar) {
      super();
      this.id = id;
      this.type = type;
      this.name = name;
      this.address = address;
      this.creator = creator;
      this.avatar = avatar;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public String getCreator() {
      return creator;
    }

    public void setCreator(String creator) {
      this.creator = creator;
    }

    public String getAvatar() {
      return avatar;
    }

    public void setAvatar(String avatar) {
      this.avatar = avatar;
    }
  }
}
