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
  private static final Log                               LOG                                   = ExoLogger.getLogger(Utils.class);

  public static final String                             SPACE_ACCOUNT_TYPE                    = "space";

  public static final String                             USER_ACCOUNT_TYPE                     = "user";

  public static final String                             GLOAL_SETTINGS_CHANGED_EVENT          =
                                                                                      "exo.addon.wallet.settings.changed";

  public static final String                             NEW_ADDRESS_ASSOCIATED_EVENT          =
                                                                                      "exo.addon.wallet.addressAssociation.new";

  public static final String                             MODIFY_ADDRESS_ASSOCIATED_EVENT       =
                                                                                         "exo.addon.wallet.addressAssociation.modification";

  public static final String                             UPGRADE_NOTIFICATION_SETTINGS         =
                                                                                       "exo.addon.wallet.notification.upgrade";

  public static final String                             NEW_TRANSACTION_EVENT                 =
                                                                               "exo.addon.wallet.transaction.loaded";

  public static final String                             NEW_BLOCK_EVENT                       = "exo.addon.wallet.block.loaded";

  public static final String                             WALLET_SENDER_NOTIFICATION_ID         = "EtherSenderNotificationPlugin";

  public static final String                             WALLET_RECEIVER_NOTIFICATION_ID       =
                                                                                         "EtherReceiverNotificationPlugin";

  public static final String                             DEPRECATED_SENDER_NOTIFICATION_ID     =
                                                                                           "ContractSenderNotificationPlugin";

  public static final String                             DEPRECATED_RECEIVER_NOTIFICATION_ID   =
                                                                                             "ContractReceiverNotificationPlugin";

  public static final String                             FUNDS_REQUEST_NOTIFICATION_ID         = "FundsRequestNotificationPlugin";

  public static final String                             FUNDS_REQUEST_SENT                    = "sent";

  public static final String                             AMOUNT                                = "amount";

  public static final String                             SYMBOL                                = "symbol";

  public static final String                             MESSAGE                               = "message";

  public static final String                             HASH                                  = "hash";

  public static final String                             ACCOUNT_TYPE                          = "account_type";

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
        }
      }
    }
    return space;
  }

  public static String getAbsoluteMyWalletLink() {
    return CommonsUtils.getCurrentDomain() + getMyWalletLink();
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

  public static List<String> jsonArrayToList(JSONObject jsonObject, String key) throws JSONException {
    List<String> list = null;
    if (jsonObject.has(key)) {
      list = new ArrayList<String>();
      JSONArray arrayValue = jsonObject.getJSONArray(key);
      for (int i = 0; i < arrayValue.length(); i++) {
        list.add(arrayValue.getString(i));
      }
    }
    return list;
  }
}
