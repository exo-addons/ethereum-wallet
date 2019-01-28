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
package org.exoplatform.addon.ethereum.wallet.migration;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * A storage service to save/load information used by users and spaces wallets
 */
public class DeprecatedEthereumWalletService {

  private static final Log    LOG                                    =
                                  ExoLogger.getLogger(DeprecatedEthereumWalletService.class);

  private static final String SPACE_ACCOUNT_TYPE                     = "space";

  private static final String USER_ACCOUNT_TYPE                      = "user";

  private static final String SPACE_WITH_ID_MESSAGE                  =
                                                    "Space with id '";

  private static final String IS_NOT_FOUND_MESSAGE                   =
                                                   "' is not found";

  private static final String ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE =
                                                                     "address parameter is mandatory";

  private SettingService      settingService;

  private IdentityManager     identityManager;

  private SpaceService        spaceService;

  public Set<Wallet> listSpacesWallets() throws Exception {
    Set<Wallet> wallets = new HashSet<>();
    Set<String> spaces = getListOfWalletsOfType(SPACE_ACCOUNT_TYPE);
    for (String spacePrettyName : spaces) {
      Wallet wallet = getSpaceDetails(spacePrettyName);
      if (wallet == null) {
        continue;
      }
      wallets.add(wallet);
    }
    return wallets;
  }

  public Set<Wallet> listUserWallets() throws Exception {
    Set<Wallet> wallets = new HashSet<>();
    Set<String> usernames = getListOfWalletsOfType(USER_ACCOUNT_TYPE);
    for (String username : usernames) {
      Wallet wallet = getUserDetails(username);
      if (wallet == null) {
        continue;
      }
      wallets.add(wallet);
    }
    return wallets;
  }

  public List<TransactionDetail> getAccountTransactions(Long networkId, String address) {
    String addressTransactionsParamName = WALLET_USER_TRANSACTION_NAME + address + networkId;
    SettingValue<?> addressTransactionsValue =
                                             getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, addressTransactionsParamName);
    String addressTransactions = addressTransactionsValue == null ? "" : addressTransactionsValue.getValue().toString();
    String[] addressTransactionsArray = addressTransactions.isEmpty() ? new String[0] : addressTransactions.split(",");
    return Arrays.stream(addressTransactionsArray).map(transaction -> fromStoredValue(transaction)).collect(Collectors.toList());
  }

  public void removeAccountByAddress(String address) {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_PARAMETER_IS_MANDATORY_MESSAGE);
    }
    Wallet accountDetails = getWalletByAddress(address.toLowerCase());
    if (accountDetails == null || accountDetails.getAddress() == null) {
      throw new IllegalStateException("Can't find an associated account to address " + address);
    }
    getSettingService().remove(WALLET_CONTEXT, WALLET_SCOPE, address);
    if (StringUtils.equals(accountDetails.getType(), SPACE_ACCOUNT_TYPE)) {
      LOG.info("Deleting wallet address association {} of space {}", address, accountDetails.getId());
      getSettingService().remove(WALLET_CONTEXT, WALLET_SCOPE, accountDetails.getId());
    } else {
      LOG.info("Deleting wallet address association {} of user {}", address, accountDetails.getId());
      getSettingService().remove(Context.USER.id(accountDetails.getId()),
                                 WALLET_SCOPE,
                                 ADDRESS_KEY_NAME);
    }
  }

  private Set<String> getListOfWalletsOfType(String walletType) throws Exception {
    if (StringUtils.isBlank(walletType) || !(USER_ACCOUNT_TYPE.equals(walletType) || SPACE_ACCOUNT_TYPE.equals(walletType))) {
      throw new IllegalArgumentException("Unrecognized wallet type: " + walletType);
    }
    Set<String> names = new HashSet<>();
    if (USER_ACCOUNT_TYPE.equals(walletType)) {
      int pageSize = 100;
      int current = 0;
      List<Context> contexts = null;
      do {
        contexts = getSettingService().getContextsByTypeAndScopeAndSettingName(Context.USER.getName(),
                                                                               WALLET_SCOPE.getName(),
                                                                               WALLET_SCOPE.getId(),
                                                                               ADDRESS_KEY_NAME,
                                                                               current,
                                                                               pageSize);
        if (contexts != null && !contexts.isEmpty()) {
          List<String> usernames = contexts.stream().map(context -> context.getId()).collect(Collectors.toList());
          names.addAll(usernames);
        }
        current += pageSize;
      } while (contexts != null && contexts.size() == pageSize);
    } else {
      int pageSize = 100;
      int current = 0;
      Space[] spaces = null;
      do {
        ListAccess<Space> spacesListAccress = getSpaceService().getAllSpacesWithListAccess();
        spaces = spacesListAccress.load(current, pageSize);
        if (spaces != null && spaces.length > 0) {
          for (Space space : spaces) {
            String spaceAddress = getSpaceAddress(space.getPrettyName());
            if (StringUtils.isNotBlank(spaceAddress)) {
              names.add(space.getPrettyName());
            }
          }
        }
        current += pageSize;
      } while (spaces != null && spaces.length == pageSize);
    }
    return names;
  }

  private Wallet getSpaceDetails(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }

    Space space = getSpace(id);
    if (space == null) {
      throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + id + IS_NOT_FOUND_MESSAGE);
    }
    id = getSpaceId(space);

    return getWalletByKey(SPACE_ACCOUNT_TYPE, id);
  }

  private Wallet getUserDetails(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id parameter is mandatory");
    }
    return getWalletByKey(USER_ACCOUNT_TYPE, id);
  }

  private Wallet getWalletByAddress(String address) {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException("wallet addres is mandatory");
    }
    Wallet accountDetail = null;
    SettingValue<?> walletAddressValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, address);
    if (walletAddressValue != null && walletAddressValue.getValue() != null) {
      String idAndType = walletAddressValue.getValue().toString();
      String id = null;
      if (idAndType.startsWith(USER_ACCOUNT_TYPE)) {
        id = idAndType.replaceFirst(USER_ACCOUNT_TYPE, "");
        accountDetail = getUserDetails(id);
      } else if (idAndType.startsWith(SPACE_ACCOUNT_TYPE)) {
        id = idAndType.replaceFirst(SPACE_ACCOUNT_TYPE, "");
        accountDetail = getSpaceDetails(id);
      }
      if (accountDetail == null) {
        LOG.info("Can't find the user/space with id {} associated to address {}", id, address);
      } else {
        accountDetail.setAddress(address);
      }
    }

    return accountDetail;
  }

  private Wallet getWalletByKey(String type, String remoteId) {
    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("wallet type is mandatory");
    }
    if (StringUtils.isBlank(remoteId)) {
      throw new IllegalArgumentException("wallet id is mandatory");
    }
    Wallet accountDetail = null;
    if (USER_ACCOUNT_TYPE.equals(type)) {
      Identity identity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
      if (identity == null || identity.getProfile() == null) {
        return null;
      }

      String avatarUrl = identity.getProfile().getAvatarUrl();
      if (StringUtils.isBlank(avatarUrl)) {
        avatarUrl = "/rest/v1/social/users/" + remoteId + "/avatar";
      }
      accountDetail = new Wallet();
      accountDetail.setId(remoteId);
      accountDetail.setTechnicalId(Long.parseLong(identity.getId()));
      accountDetail.setType(USER_ACCOUNT_TYPE);
      accountDetail.setName(identity.getProfile().getFullName());
      accountDetail.setAddress(getUserAddress(remoteId));
      accountDetail.setPassPhrase(getUserPhrase(remoteId));
      accountDetail.setSpaceAdministrator(false);
      accountDetail.setEnabled(identity.isEnable());
      accountDetail.setAvatar(avatarUrl);
    } else if (SPACE_ACCOUNT_TYPE.equals(type)) {
      Space space = getSpace(remoteId);
      if (space == null) {
        return null;
      }
      remoteId = getSpaceId(space);

      String prettyName = space.getPrettyName();
      Identity identity = getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, prettyName, true);
      String spaceAddress = getSpaceAddress(remoteId);

      accountDetail = new Wallet();
      accountDetail.setId(prettyName);
      accountDetail.setTechnicalId(Long.parseLong(identity.getId()));
      accountDetail.setType(SPACE_ACCOUNT_TYPE);
      accountDetail.setAddress(spaceAddress);
      accountDetail.setPassPhrase(getSpacePhrase(remoteId));
      accountDetail.setEnabled(true);
      return accountDetail;
    }
    return accountDetail;
  }

  private String getUserPhrase(String username) {
    SettingValue<?> browserWalletPhraseValue = getSettingService().get(Context.USER.id(username),
                                                                       WALLET_SCOPE,
                                                                       WALLET_BROWSER_PHRASE_NAME);
    if (browserWalletPhraseValue != null && browserWalletPhraseValue.getValue() != null) {
      return browserWalletPhraseValue.getValue().toString();
    }
    return null;
  }

  private String getSpacePhrase(String spaceId) {
    SettingValue<?> browserWalletPhraseValue = getSettingService().get(WALLET_CONTEXT,
                                                                       WALLET_SCOPE,
                                                                       WALLET_BROWSER_PHRASE_NAME + spaceId);
    if (browserWalletPhraseValue != null && browserWalletPhraseValue.getValue() != null) {
      return browserWalletPhraseValue.getValue().toString();
    }
    return null;
  }

  private String getUserAddress(String id) {
    SettingValue<?> userWalletAddressValue = getSettingService().get(Context.USER.id(id), WALLET_SCOPE, ADDRESS_KEY_NAME);
    if (userWalletAddressValue != null && userWalletAddressValue.getValue() != null) {
      return userWalletAddressValue.getValue().toString().toLowerCase();
    }
    return null;
  }

  private String getSpaceAddress(String id) {
    Space space = getSpace(id);
    if (space == null) {
      throw new IllegalArgumentException(SPACE_WITH_ID_MESSAGE + id + IS_NOT_FOUND_MESSAGE);
    }

    SettingValue<?> spaceWalletAddressValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, space.getPrettyName());
    if (spaceWalletAddressValue == null || spaceWalletAddressValue.getValue() == null) {
      id = getSpaceId(space);
      spaceWalletAddressValue = getSettingService().get(WALLET_CONTEXT, WALLET_SCOPE, id);
    }
    if (spaceWalletAddressValue != null && spaceWalletAddressValue.getValue() != null) {
      return spaceWalletAddressValue.getValue().toString().toLowerCase();
    }
    return null;
  }

  public static TransactionDetail fromStoredValue(String storedTransactionDetails) {
    TransactionDetail transactionMessage = new TransactionDetail();
    if (StringUtils.isNotBlank(storedTransactionDetails)) {
      String[] transactionDetailsArray = storedTransactionDetails.split(";");
      transactionMessage.setHash(transactionDetailsArray[0]);
      transactionMessage.setLabel(transactionDetailsArray.length > 1 ? decodeString(transactionDetailsArray[1]) : null);
      transactionMessage.setMessage(transactionDetailsArray.length > 2 ? decodeString(transactionDetailsArray[2]) : null);
    }
    return transactionMessage;
  }

  public static String getSpaceId(Space space) {
    return space.getGroupId().split("/")[2];
  }

  public void removeWallets(Set<Wallet> allWallets) {
    // Not used for now
  }

  public void removeTransaction(Set<Wallet> allWallets) {
    // Not used for now
  }

  private SettingService getSettingService() {
    if (settingService == null) {
      settingService = CommonsUtils.getService(SettingService.class);
    }
    return settingService;
  }

  private IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = CommonsUtils.getService(IdentityManager.class);
    }
    return identityManager;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = CommonsUtils.getService(SpaceService.class);
    }
    return spaceService;
  }
}
