/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.addon.ethereum.wallet.service.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.*;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Utils class to provide common tools and constants
 */
public class Utils {
  private static final Log LOG = ExoLogger.getLogger(Utils.class);

  private Utils() {
  }

  @SuppressWarnings("all")
  public static final char[]                             SIMPLE_CHARS                          = new char[] { 'A', 'B', 'C', 'D',
      'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c',
      'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
      '2', '3', '4', '5', '6', '7', '8', '9' };

  public static final String                             DEFAULT_NETWORK_ID                    = "defaultNetworkId";

  public static final String                             DEFAULT_NETWORK_URL                   = "defaultNetworkURL";

  public static final String                             DEFAULT_NETWORK_WS_URL                = "defaultNetworkWSURL";

  public static final String                             DEFAULT_ACCESS_PERMISSION             = "defaultAccessPermission";

  public static final String                             DEFAULT_GAS                           = "defaultGas";

  public static final String                             MIN_GAS_PRICE                         = "minGasPrice";

  public static final String                             NORMAL_GAS_PRICE                      = "normalGasPrice";

  public static final String                             MAX_GAS_PRICE                         = "maxGasPrice";

  public static final String                             DEFAULT_CONTRACTS_ADDRESSES           = "defaultContractAddresses";

  public static final String                             SCOPE_NAME                            = "ADDONS_ETHEREUM_WALLET";

  public static final String                             EXT_SCOPE_NAME                        = "ADDONS_ETHEREUM_WALLET_EXT";

  public static final String                             GLOBAL_SETTINGS_KEY_NAME              = "GLOBAL_SETTINGS";

  public static final String                             EXT_GAMIFICATION_SETTINGS_KEY_NAME    = "GAMIFICATION_SETTINGS";

  public static final String                             EXT_TOKENS_KUDOS_BUDGET               = "KUDOS_BUDGET";

  public static final String                             EXT_KUDOS_CONTRACT_ADDRESS_KEY_NAME   = "KUDOS_CONTRACT_ADDRESS";

  public static final String                             ADDRESS_KEY_NAME                      = "ADDONS_ETHEREUM_WALLET_ADDRESS";

  public static final String                             LAST_BLOCK_NUMBER_KEY_NAME            =
                                                                                    "ADDONS_ETHEREUM_LAST_BLOCK_NUMBER";

  public static final String                             SETTINGS_KEY_NAME                     =
                                                                           "ADDONS_ETHEREUM_WALLET_SETTINGS";

  public static final Context                            WALLET_CONTEXT                        = Context.GLOBAL;

  public static final Context                            EXT_WALLET_CONTEXT                    = Context.GLOBAL;

  public static final Scope                              WALLET_SCOPE                          = Scope.APPLICATION.id(SCOPE_NAME);

  public static final Scope                              EXT_WALLET_SCOPE                      =
                                                                          Scope.APPLICATION.id(EXT_SCOPE_NAME);

  public static final String                             WALLET_DEFAULT_CONTRACTS_NAME         = "WALLET_DEFAULT_CONTRACTS";

  public static final String                             WALLET_USER_TRANSACTION_NAME          = "WALLET_USER_TRANSACTION";

  public static final String                             WALLET_BROWSER_PHRASE_NAME            = "WALLET_BROWSER_PHRASE";

  public static final String                             ABI_PATH_PARAMETER                    = "contract.abi.path";

  public static final String                             BIN_PATH_PARAMETER                    = "contract.bin.path";

  public static final int                                GLOBAL_DATA_VERSION                   = 2;

  public static final int                                USER_DATA_VERSION                     = 1;

  public static final int                                DEFAULT_GAS_UPGRADE_VERSION           = 1;

  public static final int                                DEFAULT_GAS_PRICE_UPGRADE_VERSION     = 1;

  public static final String                             ADMINISTRATORS_GROUP                  = "/platform/administrators";

  public static final String                             SPACE_ACCOUNT_TYPE                    = "space";

  public static final String                             USER_ACCOUNT_TYPE                     = "user";

  public static final String                             GLOAL_SETTINGS_CHANGED_EVENT          =
                                                                                      "exo.addon.wallet.settings.changed";

  public static final String                             NEW_ADDRESS_ASSOCIATED_EVENT          =
                                                                                      "exo.addon.wallet.addressAssociation.new";

  public static final String                             MODIFY_ADDRESS_ASSOCIATED_EVENT       =
                                                                                         "exo.addon.wallet.addressAssociation.modification";

  public static final String                             KNOWN_TRANSACTION_MINED_EVENT         =
                                                                                       "exo.addon.wallet.transaction.mined";

  public static final String                             UPGRADE_NOTIFICATION_SETTINGS         =
                                                                                       "exo.addon.wallet.notification.upgrade";

  public static final String                             NEW_TRANSACTION_EVENT                 =
                                                                               "exo.addon.wallet.transaction.loaded";

  public static final String                             NEW_BLOCK_EVENT                       = "exo.addon.wallet.block.loaded";

  public static final String                             WALLET_SENDER_NOTIFICATION_ID         = "EtherSenderNotificationPlugin";

  public static final String                             WALLET_RECEIVER_NOTIFICATION_ID       =
                                                                                         "EtherReceiverNotificationPlugin";

  public static final String                             FUNDS_REQUEST_NOTIFICATION_ID         = "FundsRequestNotificationPlugin";

  public static final String                             FUNDS_REQUEST_SENT                    = "sent";

  public static final String                             CONTRACT_ADDRESS                      = "contractAddress";

  public static final String                             AMOUNT                                = "amount";

  public static final String                             SYMBOL                                = "symbol";

  public static final String                             MESSAGE                               = "message";

  public static final String                             HASH                                  = "hash";

  public static final String                             ACCOUNT_TYPE                          = "account_type";

  public static final String                             RECEIVER_TYPE                         = "receiver_type";

  public static final String                             AVATAR                                = "avatar";

  public static final String                             SENDER                                = "sender";

  public static final String                             USER                                  = "userFullname";

  public static final String                             USER_URL                              = "userUrl";

  public static final String                             SENDER_URL                            = "senderUrl";

  public static final String                             RECEIVER                              = "receiver";

  public static final String                             RECEIVER_URL                          = "receiverUrl";

  public static final String                             FUNDS_ACCEPT_URL                      = "fundsAcceptUrl";

  public static final ArgumentLiteral<AccountDetail>     FUNDS_REQUEST_SENDER_DETAIL_PARAMETER =
                                                                                               new ArgumentLiteral<>(AccountDetail.class,
                                                                                                                     "senderFullName");

  public static final ArgumentLiteral<AccountDetail>     SENDER_ACCOUNT_DETAIL_PARAMETER       =
                                                                                         new ArgumentLiteral<>(AccountDetail.class,
                                                                                                               "senderAccountDetail");

  public static final ArgumentLiteral<AccountDetail>     RECEIVER_ACCOUNT_DETAIL_PARAMETER     =
                                                                                           new ArgumentLiteral<>(AccountDetail.class,
                                                                                                                 "receiverAccountDetail");

  public static final ArgumentLiteral<FundsRequest>      FUNDS_REQUEST_PARAMETER               =
                                                                                 new ArgumentLiteral<>(FundsRequest.class,
                                                                                                       "fundsRequest");

  public static final ArgumentLiteral<TransactionStatus> TRANSACTION_STATUS_PARAMETER          =
                                                                                      new ArgumentLiteral<>(TransactionStatus.class,
                                                                                                            "transactionStatus");

  public static final ArgumentLiteral<ContractDetail>    CONTRACT_DETAILS_PARAMETER            =
                                                                                    new ArgumentLiteral<>(ContractDetail.class,
                                                                                                          "contractDetails");

  public static final ArgumentLiteral<Double>            AMOUNT_PARAMETER                      =
                                                                          new ArgumentLiteral<>(Double.class, AMOUNT);

  public static final ArgumentLiteral<String>            MESSAGE_PARAMETER                     =
                                                                           new ArgumentLiteral<>(String.class, MESSAGE);

  public static final ArgumentLiteral<String>            HASH_PARAMETER                        =
                                                                        new ArgumentLiteral<>(String.class, HASH);

  public static final ArgumentLiteral<String>            SYMBOL_PARAMETER                      =
                                                                          new ArgumentLiteral<>(String.class, SYMBOL);

  public static final ArgumentLiteral<String>            CONTRACT_ADDRESS_PARAMETER            =
                                                                                    new ArgumentLiteral<>(String.class,
                                                                                                          CONTRACT_ADDRESS);

  public static final Event                              CONTRACT_TRANSFER_EVENT               =
                                                                                 new Event("Transfer",
                                                                                           Arrays.asList(new TypeReference<Address>(true) {
                                                                                                                                                                                        },
                                                                                                         new TypeReference<Address>(true) {
                                                                                                         },
                                                                                                         new TypeReference<Uint256>() {
                                                                                                         }));

  public static final Event                              CONTRACT_APPROVE_EVENT                =
                                                                                new Event("Approval",
                                                                                          Arrays.asList(new TypeReference<Address>(true) {
                                                                                                                                                                                       },
                                                                                                        new TypeReference<Address>(true) {
                                                                                                        },
                                                                                                        new TypeReference<Uint256>() {
                                                                                                        }));

  public static final String getCurrentUserId() {
    if (ConversationState.getCurrent() != null && ConversationState.getCurrent().getIdentity() != null) {
      return ConversationState.getCurrent().getIdentity().getUserId();
    }
    return null;
  }

  public static Event approvalEvent() {
    return new Event("Approval", Arrays.asList(new TypeReference<Address>(true) {
    }, new TypeReference<Address>(true) {
    }, new TypeReference<Uint256>() {
    }));
  }

  public static List<String> getNotificationReceiversUsers(AccountDetail toAccount, String excludedId) {
    if (SPACE_ACCOUNT_TYPE.equals(toAccount.getType())) {
      Space space = getSpace(toAccount.getId());
      if (space == null) {
        return Collections.singletonList(toAccount.getId());
      } else {
        String[] members = space.getMembers();
        if (members == null || members.length == 0) {
          return Collections.emptyList();
        } else if (StringUtils.isBlank(excludedId)) {
          return Arrays.asList(members);
        } else {
          return Arrays.stream(members).filter(member -> !excludedId.equals(member)).collect(Collectors.toList());
        }
      }
    } else {
      return Collections.singletonList(toAccount.getId());
    }
  }

  @SuppressWarnings("all")
  public static String getPermanentLink(AccountDetail account) {
    String profileLink = null;
    try {
      profileLink = account.getId() == null
          || account.getType() == null ? account.getName()
                                       : USER_ACCOUNT_TYPE.equals(account.getType()) ? LinkProvider.getProfileLink(account.getId())
                                                                                     : getPermanentLink(getSpacePrettyName(account.getId()),
                                                                                                        account.getName());
    } catch (Exception e) {
      LOG.error("Error getting profile link of space", e);
    }
    return profileLink;
  }

  public static String getSpacePrettyName(String id) {
    Space space = getSpace(id);
    return space == null ? id : space.getPrettyName();
  }

  public static Space getSpace(String id) {
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
    if (id.indexOf("/spaces/") >= 0) {
      return spaceService.getSpaceByGroupId(id);
    }
    Space space = spaceService.getSpaceByPrettyName(id);
    if (space == null) {
      space = spaceService.getSpaceByGroupId("/spaces/" + id);
      if (space == null) {
        space = spaceService.getSpaceByDisplayName(id);
        if (space == null) {
          space = spaceService.getSpaceByUrl(id);
          if (space == null) {
            space = spaceService.getSpaceById(id);
          }
        }
      }
    }
    return space;
  }

  public static String getSpaceId(Space space) {
    return space.getGroupId().split("/")[2];
  }

  public static String getWalletLink(String receiverType, String receiverId) {
    if (receiverType == null || receiverId == null || USER_ACCOUNT_TYPE.equals(receiverType)) {
      return CommonsUtils.getCurrentDomain() + getMyWalletLink();
    } else {
      Space space = getSpace(receiverId);
      if (space == null) {
        return CommonsUtils.getCurrentDomain() + getMyWalletLink();
      } else {
        return CommonsUtils.getCurrentDomain() + LinkProvider.getSpaceUri(space.getPrettyName()) + "/EthereumSpaceWallet";
      }
    }
  }

  public static String getMyWalletLink() {
    UserPortalConfigService userPortalConfigService = CommonsUtils.getService(UserPortalConfigService.class);
    return "/" + PortalContainer.getInstance().getName() + "/" + userPortalConfigService.getDefaultPortal() + "/wallet";
  }

  public static String getPermanentLink(String prettyName, String name) {
    WebAppController webAppController = CommonsUtils.getService(WebAppController.class);
    ControllerContext controllerContext = new ControllerContext(webAppController,
                                                                webAppController.getRouter(),
                                                                new FakeHTTPServletResponse(),
                                                                new FakeHTTPServletRequest(),
                                                                null);
    PortalApplication application = webAppController.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
    PortalRequestContext requestContext = new PortalRequestContext(application,
                                                                   controllerContext,
                                                                   org.exoplatform.portal.mop.SiteType.PORTAL.toString(),
                                                                   "",
                                                                   "",
                                                                   null);
    WebuiRequestContext.setCurrentInstance(requestContext);
    try {
      String spaceUrl = LinkProvider.getSpaceUri(prettyName);
      if (StringUtils.isBlank(spaceUrl)) {
        return CommonsUtils.getCurrentDomain();
      }

      spaceUrl = CommonsUtils.getCurrentDomain() + spaceUrl;
      return new StringBuilder("<a href=\"").append(spaceUrl)
                                            .append("\" target=\"_parent\">")
                                            .append(StringEscapeUtils.escapeHtml(name))
                                            .append("</a>")
                                            .toString();
    } finally {
      WebuiRequestContext.setCurrentInstance(null);
    }
  }

  public static Set<String> jsonArrayToList(JSONObject jsonObject, String key) throws JSONException {
    Set<String> set = null;
    if (jsonObject.has(key)) {
      set = new HashSet<>();
      JSONArray arrayValue = jsonObject.getJSONArray(key);
      for (int i = 0; i < arrayValue.length(); i++) {
        set.add(arrayValue.getString(i));
      }
    }
    return set;
  }

  public static String encodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLEncoder.encode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }

  public static String decodeString(String content) {
    try {
      return StringUtils.isBlank(content) ? "" : URLDecoder.decode(content.trim(), "UTF-8");
    } catch (Exception e) {
      return content;
    }
  }
}
