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
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.utils.Convert;

import org.exoplatform.addon.ethereum.wallet.model.*;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
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
  private static final Log      LOG = ExoLogger.getLogger(EthereumTransactionProcessorListener.class);

  private EthereumWalletStorage ethereumWalletStorage;

  public EthereumTransactionProcessorListener(EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
  }

  @Override
  public void onEvent(Event<Transaction, TransactionReceipt> event) throws Exception {
    Transaction transaction = event.getSource();
    TransactionReceipt transactionReceipt = event.getData();
    if (transaction == null) {
      return;
    }

    String senderAddress = transaction.getFrom();
    String receiverAddress = null;

    String contractAddress = null;

    AccountDetail senderAccountDetails = null;
    AccountDetail receiverAccountDetails = null;

    BigInteger amountBigInteger = transaction.getValue();
    double amount = Convert.fromWei(amountBigInteger.toString(), Convert.Unit.ETHER).doubleValue();
    boolean isContractTransaction = amount == 0;

    // Compute receiver address switch transaction type
    if (isContractTransaction) {
      contractAddress = transaction.getTo();
      if (contractAddress == null) {
        // Contract address not found
        return;
      }
      if (transactionReceipt == null) {
        // Transaction may have failed
        return;
      }

      ContractTransactionDetail contractTransactionDetail = getReceiverAddressFromContractData(transaction, transactionReceipt);
      amount = contractTransactionDetail.getAmount();
      receiverAddress = contractTransactionDetail.getReceiver();
    } else {
      receiverAddress = transaction.getTo();
    }

    // Retrieve associated user/space names using addresses
    if (senderAddress != null) {
      senderAccountDetails = getAccountDetail(senderAddress);
    }
    if (receiverAddress != null) {
      receiverAccountDetails = getAccountDetail(receiverAddress);
    }

    if (senderAddress != null && receiverAddress != null
        && (senderAccountDetails.getId() != null || receiverAccountDetails.getId() != null)) {
      if (isContractTransaction) {
        LOG.info("Sending notification for contract {} transaction from {} to {} with amount {}",
                 contractAddress,
                 senderAddress,
                 receiverAddress,
                 amount);
      } else {
        LOG.info("Sending notification for ether transaction from {} to {} with amount {} ether",
                 senderAddress,
                 receiverAddress,
                 amount);
      }
    }

    // Send notification to sender if the address is recognized
    if (senderAccountDetails != null && senderAccountDetails.getId() != null) {
      TransactionStatus transactionStatus = isContractTransaction ? TransactionStatus.CONTRACT_SENDER : TransactionStatus.SENDER;
      sendNotification(senderAccountDetails, receiverAccountDetails, contractAddress, transactionStatus, amount);
    }

    // Send notification to receiver if the address is recognized
    if (receiverAccountDetails != null && receiverAccountDetails.getId() != null) {
      TransactionStatus transactionStatus = isContractTransaction ? TransactionStatus.CONTRACT_RECEIVER
                                                                  : TransactionStatus.RECEIVER;
      sendNotification(senderAccountDetails, receiverAccountDetails, contractAddress, transactionStatus, amount);
    }
  }

  private void sendNotification(AccountDetail senderAccountDetails,
                                AccountDetail receiverAccountDetails,
                                String contractAddress,
                                TransactionStatus transactionStatus,
                                double amount) {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderAccountDetails);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverAccountDetails);
    if (contractAddress != null) {
      ctx.append(CONTRACT_PARAMETER, contractAddress);
    }
    ctx.append(AMOUNT_PARAMETER, amount);

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private ContractTransactionDetail getReceiverAddressFromContractData(Transaction transaction,
                                                                       TransactionReceipt transactionReceipt) {
    if (transaction == null || transaction.getTo() == null || transactionReceipt == null || transactionReceipt.getLogs() == null
        || transactionReceipt.getLogs().size() == 0) {
      // Contract Transaction type is not considered for notifications
      return null;
    }

    String contractAddress = transaction.getTo();
    GlobalSettings settings = ethereumWalletStorage.getSettings(null, null);
    List<String> defaultContracts = settings.getDefaultContractsToDisplay();
    if (defaultContracts != null && defaultContracts.contains(contractAddress)) {
      // Default contract transaction, need to check receiver address if
      // he's recognized
      if (transaction.getInput() != null && !transaction.getInput().equals("0x")) {
        // TODO **** compute receiverAddress, receiverAccountDetails and
        // amount from contract data
        // receiverAddress =
        // getContractReceiverAddress(transaction.getInput())
        // amount = getContractTokenAmount(transaction.getInput())

        if (transactionReceipt.getLogs().size() > 1) {
          LOG.info("Transaction logs count is more than expected: {}", transactionReceipt.getLogs().size());
        }

        try {
          EventValues eventValues = Contract.staticExtractEventParameters(CONTRACT_TRANSFER_EVENT,
                                                                          transactionReceipt.getLogs().get(0));

          if (eventValues.getIndexedValues() != null && eventValues.getIndexedValues().size() == 3) {
            Type amountType = eventValues.getIndexedValues().get(0);
            Type senderType = eventValues.getIndexedValues().get(1);
            Type receiverType = eventValues.getIndexedValues().get(2);

            // return new ContractTransactionDetail(contractAddress, sender, receiver, amount);
          }
        } catch (Throwable e) {
          LOG.warn("Error occurred while parsing ");
        }
      }
    }
    // Contract address is not recognized
    return null;
  }

  private AccountDetail getAccountDetail(String address) {
    AccountDetail accountDetails = ethereumWalletStorage.getAccountDetailsByAddress(address);
    if (accountDetails == null) {
      accountDetails = new AccountDetail(null, null, address, address, null, LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
    }
    return accountDetails;
  }

}
