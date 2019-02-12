package org.exoplatform.addon.ethereum.wallet.storage;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class CachedTransactionStorage extends TransactionStorage {

  private FutureExoCache<String, TransactionDetail, Object> transactionFutureCache = null;

  public CachedTransactionStorage(CacheService cacheService, WalletTransactionDAO walletTransactionDAO) {
    super(walletTransactionDAO);

    ExoCache<String, TransactionDetail> transactionCache = cacheService.getCacheInstance("wallet.transaction");

    // Future cache is used for clustered environment improvements (usage of
    // putLocal VS put)
    this.transactionFutureCache = new FutureExoCache<>(new Loader<String, TransactionDetail, Object>() {
      @Override
      public TransactionDetail retrieve(Object context, String hash) throws Exception {
        return CachedTransactionStorage.super.getTransactionByHash(hash);
      }
    }, transactionCache);
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash) {
    return this.transactionFutureCache.get(null, hash.toLowerCase());
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    super.saveTransactionDetail(transactionDetail);
    if (transactionDetail.isPending()) {
      this.transactionFutureCache.remove(transactionDetail.getHash().toLowerCase());
    }
  }
}
