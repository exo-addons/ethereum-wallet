package org.exoplatform.addon.ethereum.wallet.service;

import java.io.IOException;
import java.math.BigInteger;

import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;

public interface WalletTokenTransactionService {

  BigInteger getEtherBalanceOf(String address) throws IOException;// NOSONAR

  void reward(String receiver,
              double tokenAmount,
              double rewardAmount,
              String label,
              String message,
              String username) throws Exception;// NOSONAR

  void reward(TransactionDetail transactionDetail) throws Exception;// NOSONAR

  void transfer(String receiver,
                double tokenAmount,
                String label,
                String message,
                String username,
                boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  void transfer(TransactionDetail transactionDetail, boolean enableChecksBeforeSending) throws Exception;// NOSONAR

  void initialize(String receiver, String username) throws Exception;// NOSONAR

  void initialize(String receiver,
                  double tokenAmount,
                  double etherAmount,
                  String label,
                  String message,
                  String username) throws Exception;// NOSONAR

  void initialize(TransactionDetail transactionDetail) throws Exception;// NOSONAR

  void disapproveAccount(String receiver, String label, String message, String username) throws Exception;// NOSONAR

  void disapproveAccount(TransactionDetail transactionDetail) throws Exception;// NOSONAR

  void approveAccount(String receiver, String label, String message, String username) throws Exception;// NOSONAR

  void approveAccount(TransactionDetail transactionDetail) throws Exception;// NOSONAR

  BigInteger balanceOf(String address) throws Exception;// NOSONAR

  boolean isInitializedAccount(String address) throws Exception;// NOSONAR

  boolean isAdminAccount(String address) throws Exception;// NOSONAR

  int getAdminLevel(String address) throws Exception;// NOSONAR

  boolean isApprovedAccount(String address) throws Exception;// NOSONAR

}
