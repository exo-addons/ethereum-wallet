package org.exoplatform.addon.ethereum.wallet.storage;

import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.*;

public class CachedTransactionStorage extends TransactionStorage implements Startable {

  private ExoCache<String, TransactionDetail>               transactionCache       = null;

  private FutureExoCache<String, TransactionDetail, Object> transactionFutureCache = null;

  public CachedTransactionStorage(CacheService cacheService, WalletTransactionDAO walletTransactionDAO) {
    super(walletTransactionDAO);

    transactionCache = cacheService.getCacheInstance("wallet.transaction");
    Loader<String, TransactionDetail, Object> transactionLoader = new Loader<String, TransactionDetail, Object>() {
      @Override
      public TransactionDetail retrieve(Object context, String hash) throws Exception {
        return CachedTransactionStorage.super.getTransactionByHash(hash);
      }
    };

    transactionCache.addCacheListener(new CacheListener<String, TransactionDetail>() {
      @Override
      public void onExpire(CacheListenerContext context, String key, TransactionDetail obj) throws Exception {
        // The cache doesn't expire
      }

      @Override
      public void onRemove(CacheListenerContext context, String key, TransactionDetail obj) throws Exception {
        // The cache entries aren't removed
      }

      @Override
      public void onPut(CacheListenerContext context, String key, TransactionDetail obj) throws Exception {
        // Nothing to trigger
      }

      @Override
      public void onGet(CacheListenerContext context, String key, TransactionDetail obj) throws Exception {
        // Nothing to trigger
      }

      @Override
      public void onClearCache(CacheListenerContext context) throws Exception {
        // Reload pending transactions on manual cache clear.
        // We have to keep all the time the list of pending transactions in
        // cache.
        CachedTransactionStorage.this.start();
      }
    });

    // Future cache is used for clustered environment improvements (usage of
    // putLocal VS put)
    this.transactionFutureCache = new FutureExoCache<>(transactionLoader, transactionCache);
  }

  @Override
  @ExoTransactional
  public void start() {
    // Load pending transactions in cache (Must be made on each cluster node)
    List<TransactionDetail> pendingTransactions = getPendingTransactions();
    pendingTransactions.forEach(pendingTransaction -> this.transactionFutureCache.get(null,
                                                                                      pendingTransaction.getHash()
                                                                                                        .toLowerCase()));
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash) {
    return this.transactionFutureCache.get(null, hash.toLowerCase());
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    super.saveTransactionDetail(transactionDetail);
    if (transactionDetail.isPending()) {
      this.transactionCache.put(transactionDetail.getHash().toLowerCase(), transactionDetail);
    } else {
      this.transactionFutureCache.remove(transactionDetail.getHash().toLowerCase());
    }
  }
}
