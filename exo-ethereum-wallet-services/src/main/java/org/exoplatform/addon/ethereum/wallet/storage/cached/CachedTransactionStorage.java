package org.exoplatform.addon.ethereum.wallet.storage.cached;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.addon.ethereum.wallet.storage.TransactionStorage;
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
    TransactionDetail transactionDetail = this.transactionFutureCache.get(null, hash.toLowerCase());
    return transactionDetail == null ? null : transactionDetail.clone();
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    super.saveTransactionDetail(transactionDetail);
    this.transactionFutureCache.remove(transactionDetail.getHash().toLowerCase());
  }
}
