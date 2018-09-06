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
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionStatus;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.portal.application.PortalRequestContext;
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

  public static final String                             SPACE_ACCOUNT_TYPE                            = "space";

  public static final String                             USER_ACCOUNT_TYPE                             = "user";

  public static final String                             GLOAL_SETTINGS_CHANGED_EVENT                  =
                                                                                      "exo.addon.wallet.settings.changed";

  public static final String                             NEW_TRANSACTION_EVENT                         =
                                                                               "exo.addon.wallet.transaction.loaded";

  public static final String                             NEW_BLOCK_EVENT                               =
                                                                         "exo.addon.wallet.block.loaded";

  public static final String                             TRANSACTION_SENDER_NOTIFICATION_ID            =
                                                                                            "EtherSenderNotificationPlugin";

  public static final String                             TRANSACTION_RECEIVER_NOTIFICATION_ID          =
                                                                                              "EtherReceiverNotificationPlugin";

  public static final String                             TRANSACTION_CONTRACT_SENDER_NOTIFICATION_ID   =
                                                                                                     "ContractSenderNotificationPlugin";

  public static final String                             TRANSACTION_CONTRACT_RECEIVER_NOTIFICATION_ID =
                                                                                                       "ContractReceiverNotificationPlugin";

  public static final String                             AMOUNT                                        = "amount";

  public static final String                             ACCOUNT_TYPE                                  = "account_type";

  public static final String                             AVATAR                                        = "avatar";

  public static final String                             CONTRACT                                      = "contract";

  public static final String                             SENDER                                        = "sender";

  public static final String                             SENDER_URL                                    = "sender_url";

  public static final String                             RECEIVER                                      = "receiver";

  public static final String                             RECEIVER_URL                                  = "receiver_url";

  public static final ArgumentLiteral<AccountDetail>     SENDER_ACCOUNT_DETAIL_PARAMETER               =
                                                                                         new ArgumentLiteral<AccountDetail>(AccountDetail.class,
                                                                                                                            "senderAccountDetail");

  public static final ArgumentLiteral<AccountDetail>     RECEIVER_ACCOUNT_DETAIL_PARAMETER             =
                                                                                           new ArgumentLiteral<AccountDetail>(AccountDetail.class,
                                                                                                                              "receiverAccountDetail");

  public static final ArgumentLiteral<TransactionStatus> TRANSACTION_STATUS_PARAMETER                  =
                                                                                      new ArgumentLiteral<TransactionStatus>(TransactionStatus.class,
                                                                                                                             "transactionStatus");

  public static final ArgumentLiteral<String>            CONTRACT_PARAMETER                            =
                                                                            new ArgumentLiteral<String>(String.class, CONTRACT);

  public static final ArgumentLiteral<Double>            AMOUNT_PARAMETER                              =
                                                                          new ArgumentLiteral<Double>(Double.class, AMOUNT);

  public static final Event                              CONTRACT_TRANSFER_EVENT                       =
                                                                                 new Event("Transfer",
                                                                                           Arrays.asList(new TypeReference<Address>(true) {
                                                                                                                                                                                                },
                                                                                                         new TypeReference<Address>(true) {
                                                                                                         },
                                                                                                         new TypeReference<Uint256>() {
                                                                                                         }));

  public static final Event                              CONTRACT_APPROVE_EVENT                        =
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
      Space space = CommonsUtils.getService(SpaceService.class).getSpaceByPrettyName(toAccount.getId());
      String[] members = space.getMembers();
      if (members == null || members.length == 0) {
        return Collections.emptyList();
      } else if (StringUtils.isBlank(excludedId)) {
        return Arrays.asList(members);
      } else {
        return Arrays.stream(members).filter(member -> !excludedId.equals(member)).collect(Collectors.toList());
      }
    } else {
      return Collections.singletonList(toAccount.getId());
    }
  }

  public static String getPermanentLink(AccountDetail account) {
    String profileLink = account.getId() == null
        || account.getType() == null ? account.getName()
                                     : USER_ACCOUNT_TYPE.equals(account.getType()) ? LinkProvider.getProfileLink(account.getId())
                                                                                   : getPermanentLink(account.getId(),
                                                                                                      account.getName());
    return profileLink;
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
}
