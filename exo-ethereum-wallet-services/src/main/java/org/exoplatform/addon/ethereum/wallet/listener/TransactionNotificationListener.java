/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.*;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.*;
import org.exoplatform.social.core.service.LinkProvider;

/**
 * A listener that is triggered when a watched transaction by the addon is mined
 * on blockchain. This will mark the transaction as not pending in internal
 * database and send notifications.
 */
@Asynchronous
public class TransactionNotificationListener extends Listener<Object, JSONObject> {

  private ExoContainer                     container;

  private EthereumWalletService            walletService;

  private EthereumWalletTransactionService transactionService;

  private EthereumWalletAccountService     walletAccountService;

  private EthereumWalletContractService    contractService;

  public TransactionNotificationListener(ExoContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<Object, JSONObject> event) throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    try {
      String transactionHash = event.getData().getString("hash");
      if (StringUtils.isBlank(transactionHash)) {
        return;
      }
      TransactionDetail transactionDetail = getTransactionService().getTransactionByHash(transactionHash);
      if (transactionDetail == null || !transactionDetail.isSucceeded() || transactionDetail.isAdminOperation()) {
        // No notification for admin operation or not watched transaction or not
        // succeeded transaction
        return;
      }
      Wallet senderWallet = null;
      String senderAddress = transactionDetail.getFrom();
      String principalContractAdminAddress = getWalletService().getSettings().getPrincipalContractAdminAddress();
      if (StringUtils.isNotBlank(senderAddress)) {
        senderWallet = getWalletAccountService().getWalletByAddress(senderAddress);
        if (senderWallet == null) {
          senderWallet = new Wallet();
          senderWallet.setAddress(senderAddress);
          senderWallet.setAvatar(LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
          if (StringUtils.isNotBlank(principalContractAdminAddress)
              && StringUtils.equalsIgnoreCase(principalContractAdminAddress, senderAddress)) {
            senderWallet.setName("Admin");
          } else {
            senderWallet.setName(senderAddress);
          }
        }
      }

      Wallet receiverWallet = null;
      String receiverAddress = transactionDetail.getTo();
      if (StringUtils.isNotBlank(receiverAddress)) {
        receiverWallet = getWalletAccountService().getWalletByAddress(receiverAddress);
        if (receiverWallet == null) {
          receiverWallet = new Wallet();
          receiverWallet.setAddress(receiverAddress);
          receiverWallet.setAvatar(LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
          if (StringUtils.isNotBlank(principalContractAdminAddress)
              && StringUtils.equalsIgnoreCase(principalContractAdminAddress, receiverAddress)) {
            receiverWallet.setName("Admin");
          } else {
            receiverWallet.setName(receiverAddress);
          }
        }
      }

      if (senderWallet != null && senderWallet.getTechnicalId() > 0 && senderWallet.isEnabled()) {
        sendNotification(transactionDetail, TransactionNotificationType.SENDER, senderWallet, receiverWallet);
      }
      if (receiverWallet != null && receiverWallet.getTechnicalId() > 0 && receiverWallet.isEnabled()) {
        sendNotification(transactionDetail, TransactionNotificationType.RECEIVER, senderWallet, receiverWallet);
      }
    } finally {
      RequestLifeCycle.end();
    }
  }

  private void sendNotification(TransactionDetail transactionDetail,
                                TransactionNotificationType transactionStatus,
                                Wallet senderWallet,
                                Wallet receiverWallet) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(HASH_PARAMETER, transactionDetail.getHash());
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderWallet);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverWallet);
    ctx.append(MESSAGE_PARAMETER, transactionDetail.getMessage() == null ? "" : transactionDetail.getMessage());

    if (StringUtils.isBlank(transactionDetail.getContractAddress())) {
      ctx.append(SYMBOL_PARAMETER, "ether");
      ctx.append(CONTRACT_ADDRESS_PARAMETER, "");
      ctx.append(AMOUNT_PARAMETER, transactionDetail.getValue() / Math.pow(10, 18));
    } else {
      ContractDetail contractDetails = getContractService().getContractDetail(transactionDetail.getContractAddress(),
                                                                              transactionDetail.getNetworkId());
      ctx.append(SYMBOL_PARAMETER, contractDetails.getSymbol());
      ctx.append(CONTRACT_ADDRESS_PARAMETER, contractDetails.getAddress());
      ctx.append(AMOUNT_PARAMETER, transactionDetail.getContractAmount());
    }

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private EthereumWalletService getWalletService() {
    if (walletService == null) {
      walletService = CommonsUtils.getService(EthereumWalletService.class);
    }
    return walletService;
  }

  private EthereumWalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(EthereumWalletTransactionService.class);
    }
    return transactionService;
  }

  private EthereumWalletAccountService getWalletAccountService() {
    if (walletAccountService == null) {
      walletAccountService = CommonsUtils.getService(EthereumWalletAccountService.class);
    }
    return walletAccountService;
  }

  private EthereumWalletContractService getContractService() {
    if (contractService == null) {
      contractService = CommonsUtils.getService(EthereumWalletContractService.class);
    }
    return contractService;
  }

}
