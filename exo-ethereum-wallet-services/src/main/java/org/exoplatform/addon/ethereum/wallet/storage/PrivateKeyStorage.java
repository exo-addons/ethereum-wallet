package org.exoplatform.addon.ethereum.wallet.storage;

import org.exoplatform.addon.ethereum.wallet.dao.WalletPrivateKeyDAO;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletPrivateKeyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

public class PrivateKeyStorage {

  private static final Log    LOG = ExoLogger.getLogger(EthereumWalletPrivateKeyService.class);

  private WalletPrivateKeyDAO privateKeyDAO;

  private AbstractCodec       codec;

  public PrivateKeyStorage(WalletPrivateKeyDAO privateKeyDAO, CodecInitializer codecInitializer) {
    this.privateKeyDAO = privateKeyDAO;

    try {
      this.codec = codecInitializer.getCodec();
    } catch (TokenServiceInitializationException e) {
      LOG.error("Error initializing codecs", e);
    }
  }

  private void getWalletPrivateKey(long walletId) {
  }

  protected String decodePassword(String password) {
    return this.codec.decode(password);
  }

  protected String encodePassword(String password) {
    return this.codec.encode(password);
  }

}
