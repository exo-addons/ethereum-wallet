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
package org.exoplatform.addon.ethereum.wallet.listener;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import java.math.BigInteger;
import java.util.List;

import org.web3j.abi.EventValues;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import org.exoplatform.addon.ethereum.wallet.fork.ContractUtils;
import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumClientConnector;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;

/**
 * A listener to process newly detected transactions coming from configured
 * network
 */
public class EthereumTransactionProcessorListener extends Listener<Transaction, TransactionReceipt> {
  private static final Log        LOG = ExoLogger.getLogger(EthereumTransactionProcessorListener.class);

  private EthereumWalletService   ethereumWalletService;

  private EthereumClientConnector ethereumClientConnector;

  public EthereumTransactionProcessorListener(EthereumWalletService ethereumWalletService,
                                              EthereumClientConnector ethereumClientConnector) {
    this.ethereumWalletService = ethereumWalletService;
    this.ethereumClientConnector = ethereumClientConnector;
  }

  @Override
  public void onEvent(Event<Transaction, TransactionReceipt> event) throws Exception {
    Transaction transaction = event.getSource();
    if (transaction == null) {
      return;
    }

    String senderAddress = transaction.getFrom();
    String receiverAddress = null;

    String contractAddress = null;

    AccountDetail sender = null;
    AccountDetail receiver = null;
    ContractDetail contractDetails = null;

    BigInteger amountBigInteger = transaction.getValue();
    double amount = Convert.fromWei(amountBigInteger.toString(), Convert.Unit.ETHER).doubleValue();
    boolean isContractTransaction = amount == 0;
    boolean sendNotification = true;

    GlobalSettings settings = ethereumWalletService.getSettings();

    // Compute receiver address switch transaction type
    if (isContractTransaction) {
      contractAddress = transaction.getTo();
      if (contractAddress == null) {
        // Contract address not found
        sendNotification = false;
      } else {
        try {
          ContractTransactionDetail contractTransactionDetail = getReceiverAddressFromContractData(settings, transaction);
          if (contractTransactionDetail == null) {
            // The contract information couldn't be parsed
            sendNotification = false;
          } else {
            amount = contractTransactionDetail.getAmount();
            receiverAddress = contractTransactionDetail.getReceiver();
          }
        } catch (IllegalStateException e) {
          isContractTransaction = false;
        }
      }
    }

    if (!isContractTransaction) {
      receiverAddress = transaction.getTo();
    }

    // Retrieve associated user/space names using addresses
    if (senderAddress != null) {
      sender = getAccountDetail(senderAddress);
    }
    if (receiverAddress != null) {
      receiver = getAccountDetail(receiverAddress);
    }

    if (contractAddress != null && isContractTransaction) {
      contractDetails = ethereumWalletService.getDefaultContractDetail(contractAddress, settings.getDefaultNetworkId());
      if (contractDetails == null) {
        contractDetails = new ContractDetail(settings.getDefaultNetworkId(), contractAddress, contractAddress, contractAddress);
      }
    }

    if (sendNotification && senderAddress != null && receiverAddress != null
        && (sender.getId() != null || receiver.getId() != null)) {
      if (isContractTransaction) {
        LOG.info("Sending notification for contract {} transaction from {} to {} with amount {}",
                 contractAddress,
                 sender.getId() != null ? sender.getId() : senderAddress,
                 receiver.getId() != null ? receiver.getId() : receiverAddress,
                 amount);
      } else {
        LOG.info("Sending notification for ether transaction from {} to {} with amount {} ether",
                 sender.getId() != null ? sender.getId() : senderAddress,
                 receiver.getId() != null ? receiver.getId() : receiverAddress,
                 amount);
      }
    }

    boolean transactionStatusOk = false;
    boolean transactionSaved = false;
    // Send notification to sender if the address is recognized
    if (sender != null && sender.getId() != null) {
      if (sendNotification) {
        TransactionStatus transactionStatus =
                                            isContractTransaction ? TransactionStatus.CONTRACT_SENDER : TransactionStatus.SENDER;
        if (isContractTransaction || checkTransactionStatus(transaction) != null) {
          transactionStatusOk = true;
          sendNotification(transaction.getHash(), sender, receiver, contractDetails, transactionStatus, amount);
        }
      }

      // Add user transaction
      transactionSaved = true;
      ethereumWalletService.saveAccountTransaction(settings.getDefaultNetworkId(),
                                                   sender.getAddress(),
                                                   transaction.getHash(),
                                                   true);
    }

    // Send notification to receiver if the address is recognized
    if (receiver != null && receiver.getId() != null
        && (sender == null || sender.getId() == null || !sender.getId().equals(receiver.getId()))) {
      if (sendNotification) {
        TransactionStatus transactionStatus = isContractTransaction ? TransactionStatus.CONTRACT_RECEIVER
                                                                    : TransactionStatus.RECEIVER;
        if (isContractTransaction || transactionStatusOk || checkTransactionStatus(transaction) != null) {
          sendNotification(transaction.getHash(), sender, receiver, contractDetails, transactionStatus, amount);
        }
      }

      // Add user transaction
      transactionSaved = true;
      ethereumWalletService.saveAccountTransaction(settings.getDefaultNetworkId(),
                                                   receiver.getAddress(),
                                                   transaction.getHash(),
                                                   false);
    }

    if (transactionSaved) {
      ethereumWalletService.removeTransactionMessageFromCache(transaction.getHash());
    }
  }

  private void sendNotification(String transactionHash,
                                AccountDetail senderAccountDetails,
                                AccountDetail receiverAccountDetails,
                                ContractDetail contractDetails,
                                TransactionStatus transactionStatus,
                                double amount) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderAccountDetails);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverAccountDetails);

    TransactionMessage transactionMessage = ethereumWalletService.getTransactionMessage(transactionHash);
    ctx.append(MESSAGE_PARAMETER, transactionMessage == null ? null : transactionMessage.getMessage());

    if (contractDetails != null) {
      ctx.append(CONTRACT_DETAILS_PARAMETER, contractDetails);
    }
    ctx.append(AMOUNT_PARAMETER, amount);

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private ContractTransactionDetail getReceiverAddressFromContractData(GlobalSettings settings,
                                                                       Transaction transaction) throws Exception {
    if (transaction == null || transaction.getTo() == null) {
      // Contract Transaction type is not considered for notifications
      return null;
    }

    String contractAddress = transaction.getTo();
    List<String> defaultContracts = settings.getDefaultContractsToDisplay();
    if (defaultContracts != null && defaultContracts.contains(contractAddress)) {
      // Default contract transaction, need to check receiver address if
      // he's recognized
      if (transaction.getInput() != null && !transaction.getInput().equals("0x")) {
        TransactionReceipt transactionReceipt = checkTransactionStatus(transaction);

        if (transactionReceipt == null || transactionReceipt.getLogs() == null || transactionReceipt.getLogs().size() == 0) {
          // Transaction may have failed
          return null;
        }

        if (transactionReceipt.getLogs().size() > 1) {
          LOG.info("Transaction logs count is more than expected: {}", transactionReceipt.getLogs().size());
        }

        try {
          EventValues eventValues = ContractUtils.staticExtractEventParameters(CONTRACT_TRANSFER_EVENT,
                                                                               transactionReceipt.getLogs().get(0));

          if (eventValues != null && eventValues.getIndexedValues() != null && eventValues.getNonIndexedValues() != null
              && eventValues.getIndexedValues().size() == 2 && eventValues.getNonIndexedValues().size() == 1) {
            String senderAddress = eventValues.getIndexedValues().get(0).getValue().toString();
            String receiverAddress = eventValues.getIndexedValues().get(1).getValue().toString();
            String amountBigInteger = eventValues.getNonIndexedValues().get(0).getValue().toString();

            return new ContractTransactionDetail(contractAddress,
                                                 senderAddress,
                                                 receiverAddress,
                                                 Double.parseDouble(amountBigInteger));
          }
        } catch (Throwable e) {
          LOG.warn("Error occurred while parsing transaction", e);
        }
      }
    }
    // Contract address is not recognized
    return null;
  }

  private TransactionReceipt checkTransactionStatus(Transaction transaction) throws Exception {
    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transaction.getHash());
    if (transactionReceipt == null || "0x0".equals(transactionReceipt.getStatus())) {
      // Transaction may have failed
      return null;
    }
    return transactionReceipt;
  }

  private AccountDetail getAccountDetail(String address) {
    AccountDetail accountDetails = ethereumWalletService.getAccountDetailsByAddress(address);
    if (accountDetails == null) {
      accountDetails = new AccountDetail(null, null, null, address, address, false, LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
    }
    return accountDetails;
  }

}
