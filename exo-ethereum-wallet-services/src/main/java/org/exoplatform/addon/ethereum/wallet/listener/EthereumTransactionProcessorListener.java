package org.exoplatform.addon.ethereum.wallet.listener;

import java.util.List;

import org.web3j.protocol.core.methods.response.Transaction;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletNotificationService;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * A listener to process newly detected transactions coming from configured
 * network
 */
public class EthereumTransactionProcessorListener extends Listener<Object, Transaction> {
  private static final Log                  LOG = ExoLogger.getLogger(EthereumTransactionProcessorListener.class);

  private EthereumWalletStorage             ethereumWalletStorage;

  private EthereumWalletNotificationService walletNotificationService;

  public EthereumTransactionProcessorListener(EthereumWalletNotificationService walletNotificationService,
                                              EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
    this.walletNotificationService = walletNotificationService;
  }

  @Override
  public void onEvent(Event<Object, Transaction> event) throws Exception {
    Transaction transaction = event.getData();
    if (transaction == null) {
      return;
    }

    int amount = transaction.getValue().intValue();
    String receiverAddress = transaction.getTo();
    String senderAddress = transaction.getFrom();
    if (amount == 0) {
      // Contract transaction
      if (receiverAddress != null) {
        // Search if the contract address is recognized
        GlobalSettings settings = ethereumWalletStorage.getSettings(null, null);
        List<String> defaultContracts = settings.getDefaultContractsToDisplay();
        if (defaultContracts != null && defaultContracts.contains(receiverAddress)) {
          // Default contract transaction, need to check receiver address if
          // he's recognized
          LOG.info("Detected Transaction on a defaultContract: {} from wallet address {}", receiverAddress, senderAddress);

          if (senderAddress != null) {
            AccountDetail fromUserDetails = ethereumWalletStorage.getAccountDetailsByAddress(senderAddress);
            if (fromUserDetails != null) {
              // Contract transaction made by recognized user/space wallet
              LOG.info("Detected Transaction on a defaultContract: {} from wallet of {}", receiverAddress, fromUserDetails.getName());
              walletNotificationService.sendNotification(fromUserDetails, TransactionStatus.CONTRACT_SENDER, amount);
            }
          }
        }
      }
    } else {
      // Ether transaction
      if (receiverAddress != null) {
        AccountDetail toUserDetails = ethereumWalletStorage.getAccountDetailsByAddress(receiverAddress);
        if (toUserDetails != null) {
          // Ether Transaction for a recognized address, so send him a
          // notification
          LOG.info("Detected Transaction to wallet of {}", toUserDetails.getName());
          walletNotificationService.sendNotification(toUserDetails, TransactionStatus.RECEIVER, amount);
        }
      }

      if (senderAddress != null) {
        AccountDetail fromUserDetails = ethereumWalletStorage.getAccountDetailsByAddress(senderAddress);
        if (fromUserDetails != null) {
          LOG.info("Detected Transaction from wallet of {}", fromUserDetails.getName());
          walletNotificationService.sendNotification(fromUserDetails, TransactionStatus.SENDER, amount);
        }
      }
    }
  }
}
