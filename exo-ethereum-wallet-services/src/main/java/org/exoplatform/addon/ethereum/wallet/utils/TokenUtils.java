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
package org.exoplatform.addon.ethereum.wallet.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.*;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class TokenUtils {

  private static final String RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY = "receiver address parameter is mandatory";

  private static final Log    LOG                                     = ExoLogger.getLogger(TokenUtils.class);

  private TokenUtils() {
  }

  public static final boolean isApprovedAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    return (Boolean) getTokenOperationService().executeReadOperation(getContractAddress(),
                                                                     ERTTokenV2.FUNC_ISAPPROVEDACCOUNT,
                                                                     address);
  }

  public static final int getAdminLevel(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    BigInteger adminLevel = (BigInteger) getTokenOperationService().executeReadOperation(getContractAddress(),
                                                                                         ERTTokenV2.FUNC_GETADMINLEVEL,
                                                                                         address);
    return adminLevel == null ? 0 : adminLevel.intValue();
  }

  public static final boolean isAdminAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    return (Boolean) getTokenOperationService().executeReadOperation(getContractAddress(),
                                                                     ERTTokenV2.FUNC_ISADMIN,
                                                                     address);
  }

  public static final boolean isInitializedAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    return (Boolean) getTokenOperationService().executeReadOperation(getContractAddress(),
                                                                     ERTTokenV2.FUNC_ISINITIALIZEDACCOUNT,
                                                                     address);
  }

  public static final BigInteger balanceOf(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    return (BigInteger) getTokenOperationService().executeReadOperation(getContractAddress(),
                                                                        ERTTokenV2.FUNC_BALANCEOF,
                                                                        address);
  }

  public static final CompletableFuture<TransactionReceipt> approveAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    checkAdminWalletIsValid();
    if (!isApprovedAccount(address)) {
      return getTokenOperationService().executeTokenTransaction(getContractAddress(),
                                                                ERTTokenV2.FUNC_APPROVEACCOUNT,
                                                                address);
    } else {
      LOG.debug("Wallet account {} is already approved", address);
      return CompletableFuture.completedFuture(null);
    }
  }

  public static final CompletableFuture<TransactionReceipt> disapproveAccount(String address) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    checkAdminWalletIsValid();
    boolean adminAccount = isAdminAccount(address);
    boolean approvedAccount = isApprovedAccount(address);
    if (approvedAccount && !adminAccount) {
      return getTokenOperationService().executeTokenTransaction(getContractAddress(),
                                                                ERTTokenV2.FUNC_DISAPPROVEACCOUNT,
                                                                address);
    } else {
      if (adminAccount) {
        LOG.debug("Wallet account {} is an admin, thus can't disapprove account", address);
      } else if (!approvedAccount) { // NOSONAR (Test added on purpose to make
                                     // code more clear)
        LOG.debug("Wallet account {} is already disapproved", address);
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  public static final CompletableFuture<TransactionReceipt> initialize(String address,
                                                                       BigInteger tokenAmount,
                                                                       BigInteger etherAmount) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (tokenAmount == null || tokenAmount.longValue() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();

    if (isInitializedAccount(address)) {
      throw new IllegalStateException("Wallet {} is already initialized");
    }
    String adminWalletAddress = getAdminAddress();
    if (balanceOf(adminWalletAddress).longValue() < tokenAmount.longValue()) {
      throw new IllegalStateException("Wallet admin hasn't enough tokens to initialize " + tokenAmount.longValue() + " tokens to "
          + address);
    }

    if (getEtherBalanceOf(adminWalletAddress).longValue() < etherAmount.longValue()) {
      throw new IllegalStateException("Wallet admin hasn't enough ether to initialize " + etherAmount.longValue() + " WEI to "
          + address);
    }

    return getTokenOperationService().executeTokenTransaction(getContractAddress(),
                                                              ERTTokenV2.FUNC_TRANSFER,
                                                              address,
                                                              tokenAmount,
                                                              etherAmount);
  }

  public static final CompletableFuture<TransactionReceipt> transfer(String address,
                                                                     BigInteger amount,
                                                                     boolean enableChecksBeforeSending) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (amount == null || amount.longValue() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive amount");
    }

    checkAdminWalletIsValid();
    if (enableChecksBeforeSending) {
      boolean approvedReceiver = isApprovedAccount(address);
      if (!approvedReceiver) {
        throw new IllegalStateException("Wallet receiver {} is not approved yet, thus no transfer is allowed");
      }

      String adminWalletAddress = getAdminAddress();
      if (balanceOf(adminWalletAddress).longValue() < amount.longValue()) {
        throw new IllegalStateException("Wallet admin hasn't enough funds to send " + amount.longValue() + " to " + address);
      }
    }

    return getTokenOperationService().executeTokenTransaction(getContractAddress(),
                                                              ERTTokenV2.FUNC_TRANSFER,
                                                              address,
                                                              amount);
  }

  public static final CompletableFuture<TransactionReceipt> reward(String address,
                                                                   BigInteger tokenAmount,
                                                                   BigInteger rewardAmount) throws Exception {
    if (StringUtils.isBlank(address)) {
      throw new IllegalArgumentException(RECEIVER_ADDRESS_PARAMETER_IS_MANDATORY);
    }
    if (tokenAmount == null || tokenAmount.longValue() <= 0) {
      throw new IllegalArgumentException("token amount parameter has to be a positive");
    }
    if (rewardAmount == null || rewardAmount.longValue() <= 0) {
      throw new IllegalArgumentException("reward amount parameter has to be a positive");
    }

    checkAdminWalletIsValid();
    boolean approvedReceiver = isApprovedAccount(address);
    if (!approvedReceiver) {
      throw new IllegalStateException("Wallet receiver {} is not approved yet, thus no transfer is allowed");
    }

    String adminWalletAddress = getAdminAddress();
    if (balanceOf(adminWalletAddress).longValue() < tokenAmount.longValue()) {
      throw new IllegalStateException("Wallet admin hasn't enough funds to send " + tokenAmount.longValue() + " to "
          + address);
    }

    return getTokenOperationService().executeTokenTransaction(getContractAddress(),
                                                              ERTTokenV2.FUNC_TRANSFER,
                                                              address,
                                                              tokenAmount,
                                                              rewardAmount);
  }

  public static final BigInteger getEtherBalanceOf(String address) throws IOException {
    Web3j web3j = getClientConnector().getWeb3j();
    if (web3j == null) {
      throw new IllegalStateException("Can't get ether balance of " + address + " . Connection is not established.");
    }
    return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance();
  }

  private static String getContractAddress() {
    GlobalSettings settings = getWalletService().getSettings();
    if (settings == null || StringUtils.isBlank(settings.getDefaultPrincipalAccount())) {
      throw new IllegalStateException("No principal contract address is configured");
    }
    return settings.getDefaultPrincipalAccount();
  }

  private static final void checkAdminWalletIsValid() throws Exception {
    String adminAddress = getAdminAddress();
    if (adminAddress == null) {
      throw new IllegalStateException("No admin wallet is set");
    }
    int adminLevel = getAdminLevel(adminAddress);
    if (adminLevel < 4) {
      throw new IllegalStateException("Admin wallet haven't enough privileges to manage wallets");
    }
  }

  private static String getAdminAddress() {
    return getWalletAccountService().getAdminWalletAddress();
  }

  private static EthereumWalletService getWalletService() {
    return CommonsUtils.getService(EthereumWalletService.class);
  }

  private static WalletAccountService getWalletAccountService() {
    return CommonsUtils.getService(WalletAccountService.class);
  }

  private static EthereumClientConnector getClientConnector() {
    return CommonsUtils.getService(EthereumClientConnector.class);
  }

  private static EthereumWalletTokenOperationService getTokenOperationService() {
    return CommonsUtils.getService(EthereumWalletTokenOperationService.class);
  }
}
