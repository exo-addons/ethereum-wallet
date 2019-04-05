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

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import org.exoplatform.addon.ethereum.wallet.contract.ERTTokenV2;
import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletTokenOperationService;
import org.exoplatform.commons.utils.CommonsUtils;

public class TokenUtils {

  private TokenUtils() {
  }

  public static final CompletableFuture<TransactionReceipt> approveAccount(String address) throws Exception {
    return getTokenOperationService().executeWriteOperation(getContractAddress(),
                                                            ERTTokenV2.FUNC_APPROVEACCOUNT,
                                                            address);
  }

  public static final CompletableFuture<TransactionReceipt> disapproveAccount(String address) throws Exception {
    return getTokenOperationService().executeWriteOperation(getContractAddress(),
                                                            ERTTokenV2.FUNC_DISAPPROVEACCOUNT,
                                                            address);
  }

  public static final CompletableFuture<TransactionReceipt> transfer(String address, BigInteger amount) throws Exception {
    return getTokenOperationService().executeWriteOperation(getContractAddress(),
                                                            ERTTokenV2.FUNC_TRANSFER,
                                                            address,
                                                            amount);
  }

  private static EthereumWalletService getWalletService() {
    return CommonsUtils.getService(EthereumWalletService.class);
  }

  private static EthereumWalletTokenOperationService getTokenOperationService() {
    return CommonsUtils.getService(EthereumWalletTokenOperationService.class);
  }

  private static String getContractAddress() {
    GlobalSettings settings = getWalletService().getSettings();
    if (settings == null || StringUtils.isBlank(settings.getDefaultPrincipalAccount())) {
      throw new IllegalStateException("No principal contract address is configured");
    }
    return settings.getDefaultPrincipalAccount();
  }

}
