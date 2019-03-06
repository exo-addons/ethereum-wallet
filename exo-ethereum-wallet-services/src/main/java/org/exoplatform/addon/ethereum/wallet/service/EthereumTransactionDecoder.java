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
package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.contract.ERTTokenV1.*;

import java.math.BigInteger;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthBlock.Block;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.contract.ERTTokenV1;
import org.exoplatform.addon.ethereum.wallet.model.ContractDetail;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumTransactionDecoder {

  private static final Log                 LOG                          = ExoLogger.getLogger(EthereumTransactionDecoder.class);

  private static final String              FUNC_DEPOSIT_FUNDS           = "depositFunds";

  private static final String              TRANSFER_SIGNATURE           =
                                                              org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.TRANSFER_EVENT);

  private static final String              APPROVAL_SIGNATURE           =
                                                              org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.APPROVAL_EVENT);

  private static final String              ADDEDADMIN_METHOD_SIGNATURE  =
                                                                       org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.ADDEDADMIN_EVENT);

  private static final String              REMOVEDADMIN_SIGNATURE       =
                                                                  org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.REMOVEDADMIN_EVENT);

  private static final String              APPROVEDACCOUNT_SIGNATURE    =
                                                                     org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.APPROVEDACCOUNT_EVENT);

  private static final String              DISAPPROVEDACCOUNT_SIGNATURE =
                                                                        org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.DISAPPROVEDACCOUNT_EVENT);

  private static final String              CONTRACTPAUSED_SIGNATURE     =
                                                                    org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.CONTRACTPAUSED_EVENT);

  private static final String              CONTRACTUNPAUSED_SIGNATURE   =
                                                                      org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.CONTRACTUNPAUSED_EVENT);

  private static final String              DEPOSITRECEIVED_SIGNATURE    =
                                                                     org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.DEPOSITRECEIVED_EVENT);

  private static final String              TOKENPRICECHANGED_SIGNATURE  =
                                                                       org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.TOKENPRICECHANGED_EVENT);

  private static final String              TRANSFEROWNERSHIP_SIGNATURE  =
                                                                       org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(ERTTokenV1.TRANSFEROWNERSHIP_EVENT);

  private static final Map<String, String> CONTRACT_METHODS_BY_SIG      = new HashMap<>();

  static {
    CONTRACT_METHODS_BY_SIG.put(TRANSFER_SIGNATURE, FUNC_TRANSFER);
    CONTRACT_METHODS_BY_SIG.put(APPROVAL_SIGNATURE, FUNC_APPROVE);
    CONTRACT_METHODS_BY_SIG.put(ADDEDADMIN_METHOD_SIGNATURE, FUNC_ADDADMIN);
    CONTRACT_METHODS_BY_SIG.put(REMOVEDADMIN_SIGNATURE, FUNC_REMOVEADMIN);
    CONTRACT_METHODS_BY_SIG.put(APPROVEDACCOUNT_SIGNATURE, FUNC_APPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(DISAPPROVEDACCOUNT_SIGNATURE, FUNC_DISAPPROVEACCOUNT);
    CONTRACT_METHODS_BY_SIG.put(CONTRACTPAUSED_SIGNATURE, FUNC_PAUSE);
    CONTRACT_METHODS_BY_SIG.put(CONTRACTUNPAUSED_SIGNATURE, FUNC_UNPAUSE);
    CONTRACT_METHODS_BY_SIG.put(DEPOSITRECEIVED_SIGNATURE, FUNC_DEPOSIT_FUNDS);
    CONTRACT_METHODS_BY_SIG.put(TOKENPRICECHANGED_SIGNATURE, FUNC_SETSELLPRICE);
    CONTRACT_METHODS_BY_SIG.put(TRANSFEROWNERSHIP_SIGNATURE, FUNC_TRANSFEROWNERSHIP);
  }

  private EthereumWalletContractService contractService;

  private EthereumClientConnector       ethereumClientConnector;

  public EthereumTransactionDecoder(EthereumClientConnector ethereumClientConnector,
                                    EthereumWalletContractService contractService) {
    this.ethereumClientConnector = ethereumClientConnector;
    this.contractService = contractService;
  }

  public TransactionDetail computeTransactionDetail(long networkId,
                                                    String hash,
                                                    ContractDetail contractDetail) throws InterruptedException {
    if (StringUtils.isBlank(hash)) {
      throw new IllegalArgumentException("Transaction hash is empty");
    }
    if (networkId <= 0) {
      throw new IllegalArgumentException("Unknown network id: " + networkId);
    }

    TransactionDetail transactionDetail = new TransactionDetail();
    transactionDetail.setNetworkId(networkId);
    transactionDetail.setHash(hash);
    return computeTransactionDetail(transactionDetail, contractDetail);
  }

  public TransactionDetail computeTransactionDetail(TransactionDetail transactionDetail,
                                                    ContractDetail contractDetail) throws InterruptedException {
    if (transactionDetail == null) {
      throw new IllegalArgumentException("Empty transaction detail parameter");
    }
    String hash = transactionDetail.getHash();
    if (StringUtils.isBlank(hash)) {
      throw new IllegalStateException("Transaction hash is empty");
    }
    if (transactionDetail.getNetworkId() <= 0) {
      throw new IllegalStateException("Unknown network id: " + transactionDetail.getNetworkId());
    }

    Transaction transaction = ethereumClientConnector.getTransaction(hash);
    if (transaction == null) {
      LOG.info("Can't find transaction with hash {}, it may be pending", hash);
      return transactionDetail;
    }

    Block block = ethereumClientConnector.getBlock(transaction.getBlockHash());
    transactionDetail.setTimestamp(block.getTimestamp().longValue());

    String senderAddress = transaction.getFrom();
    transactionDetail.setFrom(senderAddress);
    transactionDetail.setValue(transaction.getValue() == null ? 0 : transaction.getValue().longValue());

    TransactionReceipt transactionReceipt = ethereumClientConnector.getTransactionReceipt(hash);
    transactionDetail.setPending(transactionReceipt == null);
    transactionDetail.setSucceeded(transactionReceipt != null && transactionReceipt.isStatusOK());

    String receiverAddress = transaction.getTo();
    transactionDetail.setTo(receiverAddress);

    if (contractDetail == null && receiverAddress != null) {
      contractDetail = contractService.getContractDetail(receiverAddress, transactionDetail.getNetworkId());
    }

    if (contractDetail != null) {
      transactionDetail.setContractAddress(contractDetail.getAddress());
      computeContractTransactionDetail(transactionDetail, transactionReceipt);
    }

    return transactionDetail;
  }

  private void computeContractTransactionDetail(TransactionDetail transactionDetail,
                                                TransactionReceipt transactionReceipt) {
    List<org.web3j.protocol.core.methods.response.Log> logs = transactionReceipt.getLogs();

    String hash = transactionDetail.getHash();
    if (logs == null || logs.isEmpty()) {
      if (transactionDetail.getValue() == 0) {
        LOG.debug("Retrieving information from blockchain for transaction {} with NO LOGS, set it as failed", hash);
        transactionDetail.setSucceeded(false);
      }
    } else {
      LOG.debug("Retrieving information from blockchain for transaction {} with {} LOGS", hash, logs.size());
      transactionDetail.setSucceeded(true);

      boolean transactionLogTreated = false;
      for (int i = 0; i < logs.size(); i++) {
        org.web3j.protocol.core.methods.response.Log log = logs.get(i);

        List<String> topics = log.getTopics();
        if (topics == null || topics.isEmpty()) {
          LOG.warn("Transaction {} has NO topics", hash);
          transactionDetail.setSucceeded(false);
          continue;
        }
        String topic = topics.get(0);
        LOG.debug("Treating transaction {} with {} topics", hash, topics.size());
        String methodName = CONTRACT_METHODS_BY_SIG.get(topic);
        transactionDetail.setContractMethodName(methodName);
        if (StringUtils.equals(methodName, FUNC_TRANSFER)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(TRANSFER_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
          transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
          BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
          transactionDetail.setContractAmount(amount.doubleValue());
          if (!StringUtils.equals(transactionReceipt.getFrom(), transactionDetail.getFrom())) {
            transactionDetail.setBy(transactionReceipt.getFrom());
            transactionDetail.setContractMethodName(FUNC_TRANSFERFROM);
          }
        } else if (StringUtils.equals(methodName, FUNC_APPROVE)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(APPROVAL_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(parameters.getIndexedValues().get(0).getValue().toString());
          transactionDetail.setTo(parameters.getIndexedValues().get(1).getValue().toString());
          BigInteger amount = (BigInteger) parameters.getNonIndexedValues().get(0).getValue();
          transactionDetail.setContractAmount(amount.doubleValue());
        } else if (StringUtils.equals(methodName, FUNC_APPROVEACCOUNT)) {
          if (logs.size() > 1) {
            // Implicit acccount approval
            continue;
          }
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(APPROVEDACCOUNT_EVENT, log);
          transactionDetail.setFrom(transactionReceipt.getFrom());
          if (parameters == null) {
            continue;
          }
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_DISAPPROVEACCOUNT)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(DISAPPROVEDACCOUNT_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_ADDADMIN)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(ADDEDADMIN_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (StringUtils.equals(methodName, FUNC_REMOVEADMIN)) {
          transactionLogTreated = true;
          EventValues parameters = extractEventParameters(REMOVEDADMIN_EVENT, log);
          if (parameters == null) {
            continue;
          }
          transactionDetail.setFrom(transactionReceipt.getFrom());
          transactionDetail.setTo(parameters.getNonIndexedValues().get(0).getValue().toString());
          transactionDetail.setAdminOperation(true);
        } else if (!transactionLogTreated && (i + 1) == logs.size()) {
          LOG.info("Can't find contract method name of transaction {}", transactionDetail);
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private static EventValues extractEventParameters(Event event, org.web3j.protocol.core.methods.response.Log log) {
    List<String> topics = log.getTopics();
    String encodedEventSignature = org.exoplatform.addon.ethereum.wallet.fork.EventEncoder.encode(event);
    if (!topics.get(0).equals(encodedEventSignature)) {
      return null;
    }

    List<Type> indexedValues = new ArrayList<>();
    List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());

    List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
    for (int i = 0; i < indexedParameters.size(); i++) {
      Type value = FunctionReturnDecoder.decodeIndexedValue(topics.get(i + 1), indexedParameters.get(i));
      indexedValues.add(value);
    }
    return new EventValues(indexedValues, nonIndexedValues);
  }

}
