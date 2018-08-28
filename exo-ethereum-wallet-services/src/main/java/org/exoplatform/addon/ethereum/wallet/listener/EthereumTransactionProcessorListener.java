package org.exoplatform.addon.ethereum.wallet.listener;

import java.net.IDN;

import org.web3j.protocol.core.methods.response.Transaction;

import org.exoplatform.addon.ethereum.wallet.model.AccountDetail;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletStorage;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * A listener to process newly detected transactions coming from configured network
 */
public class EthereumTransactionProcessorListener extends Listener<Object, Transaction> {
  private EthereumWalletStorage ethereumWalletStorage;

  public EthereumTransactionProcessorListener(EthereumWalletStorage ethereumWalletStorage) {
    this.ethereumWalletStorage = ethereumWalletStorage;
  }

  @Override
  public void onEvent(Event<Object, Transaction> event) throws Exception {
    Transaction transaction = event.getData();
    if (transaction == null) {
      return;
    }

    AccountDetail toUserDetails = ethereumWalletStorage.getUserDetailsByAddress(transaction.getTo());
    AccountDetail fromUserDetails = ethereumWalletStorage.getUserDetailsByAddress(transaction.getFrom());

    if (toUserDetails != null) {
      System.out.println("toUserDetails: " + toUserDetails);
    }

    if (fromUserDetails != null) {
      System.out.println("fromUserDetails: " + fromUserDetails);
    }

    if (transaction.getInput() != null) {
      String input = IDN.toASCII(transaction.getInput());
      System.out.println("input: " + input);
    }
  }
}