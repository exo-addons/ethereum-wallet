/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.addon.ethereum.wallet.service;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;

@Managed
@NameTemplate({ @Property(key = "service", value = "ethereum"), @Property(key = "view", value = "wallet") })
@ManagedDescription("Ethereum blockchain wallet service")
public class EthereumWalletServiceManaged {

  private EthereumClientConnector          clientConnector;

  private EthereumWalletAccountService     accountService;

  private EthereumWalletTransactionService transactionService;

  public EthereumWalletServiceManaged(EthereumWalletService ethereumWalletService) {
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain known treated transactions")
  public long getKnownTreatedTransactionsCount() {
    return getTransactionService().getWatchedTreatedTransactionsCount();
  }

  @Managed
  @ManagedDescription("Get ethereum wallets count")
  public long getWalletsCount() {
    return getAccountService().getWalletsCount();
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain connection interruption count")
  public int getConnectionInterruptionCount() {
    return getClientConnector().getConnectionInterruptionCount();
  }

  public EthereumWalletTransactionService getTransactionService() {
    if (transactionService == null) {
      transactionService = CommonsUtils.getService(EthereumWalletTransactionService.class);
    }
    return transactionService;
  }

  public EthereumWalletAccountService getAccountService() {
    if (accountService == null) {
      accountService = CommonsUtils.getService(EthereumWalletAccountService.class);
    }
    return accountService;
  }

  public EthereumClientConnector getClientConnector() {
    if (clientConnector == null) {
      clientConnector = CommonsUtils.getService(EthereumClientConnector.class);
    }
    return clientConnector;
  }
}
