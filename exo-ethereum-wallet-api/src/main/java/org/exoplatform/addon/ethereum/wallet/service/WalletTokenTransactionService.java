package org.exoplatform.addon.ethereum.wallet.service;

import java.math.BigInteger;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;

public interface WalletTokenTransactionService {

  BigInteger getEtherBalanceOf(String address) throws Exception;// NOSONAR

  TransactionDetail reward(String receiver,
                           double tokenAmount,
                           double rewardAmount,
                           String label,
                           String message,
                           String username) throws Exception;// NOSONAR

  TransactionDetail reward(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail transfer(String receiver,
                             double tokenAmount,
                             String label,
                             String message,
                             String issuerUsername,
                             boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  TransactionDetail transfer(TransactionDetail transactionDetail,
                             String issuerUsername,
                             boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  TransactionDetail initialize(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail initialize(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail disapproveAccount(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail disapproveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail approveAccount(String receiver, String issuerUsername) throws Exception;// NOSONAR

  TransactionDetail approveAccount(TransactionDetail transactionDetail, String issuerUsername) throws Exception;// NOSONAR

  BigInteger balanceOf(String address) throws Exception;// NOSONAR

  boolean isInitializedAccount(String address) throws Exception;// NOSONAR

  boolean isAdminAccount(String address) throws Exception;// NOSONAR

  int getAdminLevel(String address) throws Exception;// NOSONAR

  boolean isApprovedAccount(String address) throws Exception;// NOSONAR

  String getContractAddress();

  void reinit();

}
