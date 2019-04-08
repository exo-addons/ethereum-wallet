package org.exoplatform.addon.ethereum.wallet.service;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import org.exoplatform.addon.ethereum.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumWalletTokenOperationService {

  private static final Log        LOG = ExoLogger.getLogger(EthereumWalletTokenOperationService.class);

  private WalletService           walletService;

  private EthereumClientConnector clientConnector;

  private WalletAccountService    accountService;

  public EthereumWalletTokenOperationService(EthereumClientConnector ethereumClientConnector,
                                             WalletService ethereumWalletService,
                                             WalletAccountService walletAccountService) {
    this.walletService = ethereumWalletService;
    this.clientConnector = ethereumClientConnector;
    this.accountService = walletAccountService;
  }

  public CompletableFuture<TransactionReceipt> executeTokenTransaction(final String contractAddress,
                                                                       final String methodName,
                                                                       final Object... arguments) throws Exception { // NOSONAR
    ECKeyPair adminWalletKeys = (ECKeyPair) accountService.getAdminWalletKeys();
    if (adminWalletKeys == null) {
      return null;
    }
    Credentials credentials = Credentials.create(adminWalletKeys);
    GlobalSettings settings = walletService.getSettings();
    ContractGasProvider gasProvider = new StaticGasProvider(new BigInteger(String.valueOf(settings.getMinGasPrice())),
                                                            new BigInteger(String.valueOf(settings.getDefaultGas())));
    ERTTokenV2 ertInstance = ERTTokenV2.load(contractAddress, clientConnector.getWeb3j(), credentials, gasProvider);
    Method methodToInvoke = getMethod(methodName);
    if (methodToInvoke == null) {
      throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
    }
    @SuppressWarnings("unchecked")
    RemoteCall<TransactionReceipt> response = (RemoteCall<TransactionReceipt>) methodToInvoke.invoke(ertInstance, arguments);
    response.observable()
            .doOnError(error -> LOG.error("Error while sending transaction on contract with address {}, operation: {}, arguments: {}",
                                          contractAddress,
                                          methodName,
                                          arguments,
                                          error));
    return response.sendAsync();
  }

  public Object executeReadOperation(final String contractAddress,
                                     final String methodName,
                                     final Object... arguments) throws Exception {
    ECKeyPair adminWalletKeys = (ECKeyPair) accountService.getAdminWalletKeys();
    if (adminWalletKeys == null) {
      return null;
    }
    Credentials credentials = Credentials.create(adminWalletKeys);
    GlobalSettings settings = walletService.getSettings();
    ContractGasProvider gasProvider = new StaticGasProvider(new BigInteger(String.valueOf(settings.getMinGasPrice())),
                                                            new BigInteger(String.valueOf(settings.getDefaultGas())));
    ERTTokenV2 ertInstance = ERTTokenV2.load(contractAddress, clientConnector.getWeb3j(), credentials, gasProvider);
    Method methodToInvoke = getMethod(methodName);
    if (methodToInvoke == null) {
      throw new IllegalStateException("Can't find method " + methodName + " in Token instance");
    }
    RemoteCall<?> response = (RemoteCall<?>) methodToInvoke.invoke(ertInstance, arguments);
    response.observable()
            .doOnError(error -> LOG.error("Error while calling method {} on contract with address {}, arguments: {}",
                                          methodName,
                                          contractAddress,
                                          arguments,
                                          error));
    return response.send();
  }

  private Method getMethod(String methodName) {
    Method methodToInvoke = null;
    Method[] methods = ERTTokenV2.class.getDeclaredMethods();
    for (Method method : methods) {
      if (StringUtils.equals(methodName, method.getName())) {
        methodToInvoke = method;
      }
    }
    return methodToInvoke;
  }

}
