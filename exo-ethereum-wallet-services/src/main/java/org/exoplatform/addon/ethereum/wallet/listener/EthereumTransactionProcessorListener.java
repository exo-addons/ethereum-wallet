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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
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

    TransactionReceipt transactionReceipt = null;
    if (ethereumWalletService.getTransactionDetailFromCache(transaction.getHash()) != null) {
      transactionReceipt = getTransactionStatus(transaction);
      try {
        ethereumWalletService.broadcastNewTransactionEvent(transaction.getHash(),
                                                           null,
                                                           transactionReceipt != null && transactionReceipt.isStatusOK());
      } catch (Exception e) {
        LOG.error("Error broadcasting finished transaction with hash " + transaction.getHash(), e);
      }
    }

    String senderAddress = transaction.getFrom();
    String receiverAddress = null;

    AccountDetail sender = null;
    AccountDetail receiver = null;

    GlobalSettings settings = ethereumWalletService.getSettings();

    ContractDetail contractDetails =
                                   StringUtils.isBlank(transaction.getTo()) ? null
                                                                            : ethereumWalletService.getContractDetail(transaction.getTo(),
                                                                                                                      settings.getDefaultNetworkId());
    // Save transaction in contract transactions list
    if (contractDetails != null && StringUtils.isNotBlank(contractDetails.getName())) {
      ethereumWalletService.saveAccountTransaction(settings.getDefaultNetworkId(),
                                                   transaction.getTo(),
                                                   transaction.getHash(),
                                                   false,
                                                   null);
    }

    BigInteger amountBigInteger = transaction.getValue();
    double amount = Convert.fromWei(amountBigInteger.toString(), Convert.Unit.ETHER).doubleValue();
    boolean isContractTransaction = amount == 0;
    boolean sendNotification = true;

    String contractAddress = null;
    // Compute receiver address switch transaction type
    if (isContractTransaction) {
      contractAddress = transaction.getTo();
      if (contractAddress == null) {
        // Contract address not found
        sendNotification = false;
      } else {
        try {
          TransactionDetail contractTransactionDetail =
                                                      getReceiverAddressFromContractData(settings,
                                                                                         contractDetails,
                                                                                         transaction,
                                                                                         transactionReceipt);
          if (contractTransactionDetail == null) {
            // The contract information couldn't be parsed
            sendNotification = false;
          } else {
            amount = contractTransactionDetail.getContractAmount();
            receiverAddress = contractTransactionDetail.getTo();
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
    sender = getAccountDetail(senderAddress);
    receiver = getAccountDetail(receiverAddress);

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

    // Send notification to sender only when not already sent
    TransactionDetail transactionDetailFromCache = ethereumWalletService.getTransactionDetailFromCache(transaction.getHash());
    boolean isTransactionPendingInCache = transactionDetailFromCache != null && transactionDetailFromCache.isPending();
    if (transactionDetailFromCache != null) {
      // Mark transaction as not pending in cache
      transactionDetailFromCache.setPending(false);
      ethereumWalletService.setTransactionDetailInCache(transactionDetailFromCache);
    }

    // Send notification to sender if the address is recognized
    if (sender != null && sender.getId() != null) {
      if (sendNotification && isContractTransaction || getTransactionStatus(transaction) != null) {
        transactionStatusOk = true;
        // Chack if ether was sent to contract
        if (!isContractTransaction && StringUtils.isBlank(receiver.getId()) && contractDetails != null) {
          receiver.setName("Contract: " + contractDetails.getName());
        }

        if (isTransactionPendingInCache) {
          sendNotification(transaction.getHash(), sender, receiver, contractDetails, TransactionStatus.SENDER, amount);
        }
      }

      if (transactionReceipt == null) {
        transactionReceipt = getTransactionStatus(transaction);
      }

      // Add user transaction
      ethereumWalletService.saveAccountTransaction(settings.getDefaultNetworkId(),
                                                   sender.getAddress(),
                                                   transaction.getHash(),
                                                   true,
                                                   transactionReceipt != null && transactionReceipt.isStatusOK());
    }

    // Send notification to receiver if the address is recognized
    if (receiver != null && receiver.getId() != null
        && (sender == null || sender.getId() == null || !sender.getId().equals(receiver.getId())))

    {
      if (sendNotification && isContractTransaction || transactionStatusOk || getTransactionStatus(transaction) != null) {
        // Change address to 'Admin' if the sender address equals to the
        // principal contract owner
        if (sender != null && sender.getId() == null && settings != null && settings.getPrincipalContractAdminAddress() != null
            && StringUtils.equalsIgnoreCase(settings.getPrincipalContractAdminAddress(), sender.getAddress())) {
          sender.setName(settings.getPrincipalContractAdminName());
        }

        if (isTransactionPendingInCache) {
          // Send notification to receiver
          sendNotification(transaction.getHash(), sender, receiver, contractDetails, TransactionStatus.RECEIVER, amount);
        }
      }

      // Add user transaction
      boolean saveLabel = sender == null && SPACE_ACCOUNT_TYPE.equals(receiver.getType());
      if (transactionReceipt == null) {
        transactionReceipt = getTransactionStatus(transaction);
      }
      ethereumWalletService.saveAccountTransaction(settings.getDefaultNetworkId(),
                                                   receiver.getAddress(),
                                                   transaction.getHash(),
                                                   saveLabel,
                                                   transactionReceipt != null && transactionReceipt.isStatusOK());
    }
  }

  private void sendNotification(String transactionHash,
                                AccountDetail senderAccountDetails,
                                AccountDetail receiverAccountDetails,
                                ContractDetail contractDetails,
                                TransactionStatus transactionStatus,
                                double amount) {
    if (receiverAccountDetails == null || senderAccountDetails == null || StringUtils.isBlank(receiverAccountDetails.getAddress())
        || StringUtils.isBlank(senderAccountDetails.getAddress())) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can't send notification for transaction {} because sender address is '{}' and receiver address is '{}'",
                  senderAccountDetails == null || senderAccountDetails.getAddress() == null ? "null"
                                                                                            : senderAccountDetails.getAddress(),
                  receiverAccountDetails == null
                      || receiverAccountDetails.getAddress() == null ? "null" : receiverAccountDetails.getAddress());
      }
      return;
    }
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(HASH_PARAMETER, transactionHash);
    ctx.append(SENDER_ACCOUNT_DETAIL_PARAMETER, senderAccountDetails);
    ctx.append(RECEIVER_ACCOUNT_DETAIL_PARAMETER, receiverAccountDetails);

    TransactionDetail transactionMessage = ethereumWalletService.getTransactionDetailFromCache(transactionHash);
    ctx.append(MESSAGE_PARAMETER, transactionMessage == null ? "" : transactionMessage.getMessage());

    if (contractDetails == null) {
      ctx.append(SYMBOL_PARAMETER, "ether");
      ctx.append(CONTRACT_ADDRESS_PARAMETER, "");
    } else {
      ctx.append(SYMBOL_PARAMETER, contractDetails.getSymbol());
      ctx.append(CONTRACT_ADDRESS_PARAMETER, contractDetails.getAddress());
    }
    ctx.append(AMOUNT_PARAMETER, amount);

    // Notification type is determined automatically by
    // transactionStatus.getNotificationId()
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(transactionStatus.getNotificationId()))).execute(ctx);
  }

  private TransactionDetail getReceiverAddressFromContractData(GlobalSettings settings,
                                                               ContractDetail contractDetail,
                                                               Transaction transaction,
                                                               TransactionReceipt transactionReceipt) throws InterruptedException,
                                                                                                      ExecutionException {
    if (transaction == null || transaction.getTo() == null || contractDetail == null) {
      // Contract Transaction type is not considered for notifications
      return null;
    }

    String contractAddress = transaction.getTo();
    Set<String> defaultContracts = settings.getDefaultContractsToDisplay();
    // Default contract transaction, need to check receiver address if
    // he's recognized
    if (defaultContracts != null && defaultContracts.contains(contractAddress) && transaction.getInput() != null
        && !transaction.getInput().equals("0x")) {
      if (transactionReceipt == null) {
        transactionReceipt = getTransactionStatus(transaction);
      }
      if (transactionReceipt == null || !transactionReceipt.isStatusOK() || transactionReceipt.getLogs() == null
          || transactionReceipt.getLogs().isEmpty()) {
        // Transaction may have failed
        return null;
      }

      try {
        for (int i = 0; i < transactionReceipt.getLogs().size(); i++) {
          TransactionDetail contractTransactionDetail = getContractTransactiondetail(contractDetail,
                                                                                     contractAddress,
                                                                                     transactionReceipt,
                                                                                     i);
          if (contractTransactionDetail != null) {
            return contractTransactionDetail;
          }
        }
      } catch (Throwable e) {
        LOG.warn("Error occurred while parsing transaction", e);
      }
    }
    // Contract address is not recognized
    return null;
  }

  private TransactionDetail getContractTransactiondetail(ContractDetail contractDetail,
                                                         String contractAddress,
                                                         TransactionReceipt transactionReceipt,
                                                         int i) {
    EventValues eventValues = ContractUtils.staticExtractEventParameters(CONTRACT_TRANSFER_EVENT,
                                                                         transactionReceipt.getLogs().get(i));

    if (eventValues != null && eventValues.getIndexedValues() != null && eventValues.getNonIndexedValues() != null
        && eventValues.getIndexedValues().size() == 2 && eventValues.getNonIndexedValues().size() == 1) {
      String senderAddress = eventValues.getIndexedValues().get(0).getValue().toString();
      String receiverAddress = eventValues.getIndexedValues().get(1).getValue().toString();
      BigInteger amountBigInteger = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
      String amountString = BigDecimal.valueOf(Double.parseDouble(amountBigInteger.toString()))
                                      .divide(BigDecimal.valueOf(10).pow(contractDetail.getDecimals()))
                                      .toString();

      TransactionDetail transactionDetail = new TransactionDetail();
      transactionDetail.setContractAddress(contractAddress);
      transactionDetail.setFrom(senderAddress);
      transactionDetail.setTo(receiverAddress);
      transactionDetail.setContractAmount(Double.parseDouble(amountString));
      transactionDetail.setHash(transactionReceipt.getTransactionHash());
      return transactionDetail;
    }
    return null;
  }

  private TransactionReceipt getTransactionStatus(Transaction transaction) throws InterruptedException, ExecutionException {
    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(transaction.getHash());
    if (transactionReceipt == null || "0x0".equals(transactionReceipt.getStatus())) {
      // Transaction may have failed
      return null;
    }
    return transactionReceipt;
  }

  private AccountDetail getAccountDetail(String address) {
    if (StringUtils.isBlank(address)) {
      return new AccountDetail(null, null, null, address, address, false, true, LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
    }

    AccountDetail accountDetails = ethereumWalletService.getAccountDetailsByAddress(address);
    if (accountDetails == null) {
      accountDetails =
                     new AccountDetail(null, null, null, address, address, false, true, LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
    }
    return accountDetails;
  }

}
