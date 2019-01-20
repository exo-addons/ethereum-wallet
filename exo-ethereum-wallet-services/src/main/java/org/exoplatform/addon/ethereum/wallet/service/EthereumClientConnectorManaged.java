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

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;

@Managed
@NameTemplate(@Property(key = "connector", value = "ethereum"))
@ManagedDescription("Ethereum blockchain client connector")
public class EthereumClientConnectorManaged {

  private EthereumClientConnector ethereumClientConnector;

  public EthereumClientConnectorManaged(EthereumClientConnector ethereumClientConnector) {
    this.ethereumClientConnector = ethereumClientConnector;
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain connection interruption count")
  public int getConnectionInterruptionCount() {
    return ethereumClientConnector.getConnectionInterruptionCount();
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain transactions queue size")
  public int getTransactionQueueSize() {
    return ethereumClientConnector.getTransactionQueueSize();
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain max transactions queue size since startup")
  public int getTransactionQueueMaxSize() {
    return ethereumClientConnector.getTransactionQueueMaxSize();
  }

  @Managed
  @ManagedDescription("Get last watched block from ethereum blockchain")
  public long getLastWatchedBlockNumber() {
    return ethereumClientConnector.getLastWatchedBlockNumber();
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain watching start time in minutes")
  public String getWatchingBlockchainStartTime() {
    long watchingBlockchainStartTime = ethereumClientConnector.getWatchingBlockchainStartTime();
    return "Since " + ((System.currentTimeMillis() - watchingBlockchainStartTime) / 1000 / 60) + " minutes";
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain watched transactions")
  public int getWatchedTransactionCount() {
    return ethereumClientConnector.getWatchedTransactionCount();
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain watching transactions rate per minute")
  public int getWatchingTransactionRatePerMinute() {
    int watchedTransactionCount = ethereumClientConnector.getWatchedTransactionCount();
    double diffTime = (double) System.currentTimeMillis() - ethereumClientConnector.getWatchingBlockchainStartTime();
    diffTime = diffTime / 1000 / 60;
    return (int) (watchedTransactionCount / diffTime);
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain treating transactions rate per minute")
  public int getTreatingTransactionRatePerMinute() {
    int watchedTransactionCount = ethereumClientConnector.getWatchedTransactionCount();
    int queueSize = ethereumClientConnector.getTransactionQueueSize();
    double diffTime = (double) System.currentTimeMillis() - ethereumClientConnector.getWatchingBlockchainStartTime();
    diffTime = diffTime / 1000 / 60;
    return (int) ((watchedTransactionCount - queueSize) / diffTime);
  }

  @Managed
  @ManagedDescription("Get ethereum blockchain transactions treatment percentage")
  public String getTransactionTreatmentSpeedPercentage() {
    return (getTreatingTransactionRatePerMinute() / getWatchingTransactionRatePerMinute() * 100) + "%";
  }

  @Managed
  @ManagedDescription("Clear all transactions in queue")
  public void clearQueue() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Managed
  @ManagedDescription("Set last watched block on ethereum blockchain")
  public void setLastWatchedBlockNumber() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

}
