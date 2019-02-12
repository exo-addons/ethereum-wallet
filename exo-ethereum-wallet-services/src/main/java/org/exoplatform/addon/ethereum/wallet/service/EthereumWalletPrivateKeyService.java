package org.exoplatform.addon.ethereum.wallet.service;

import org.exoplatform.addon.ethereum.wallet.storage.PrivateKeyStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EthereumWalletPrivateKeyService {

  private static final Log  LOG = ExoLogger.getLogger(EthereumWalletPrivateKeyService.class);

  private PrivateKeyStorage privateKeyStorage;

  public EthereumWalletPrivateKeyService(PrivateKeyStorage privateKeyStorage) {
    this.privateKeyStorage = privateKeyStorage;
  }

  private void getWalletPrivateKey(long walletId) {
  }

}
