package org.exoplatform.addon.ethereum.wallet.storage;

import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.dao.WalletTransactionDAO;
import org.exoplatform.addon.ethereum.wallet.model.TransactionDetail;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class CachedTransactionStorage extends TransactionStorage implements Startable {

  private CacheService                                      cacheService;

  private ExoCache<String, TransactionDetail>               transactionCache       = null;

  private FutureExoCache<String, TransactionDetail, Object> transactionFutureCache = null;

  public CachedTransactionStorage(WalletTransactionDAO walletTransactionDAO) {
    super(walletTransactionDAO);
  }

  @Override
  @ExoTransactional
  public void start() {
    transactionCache = getCacheService().getCacheInstance("wallet.transaction");
    Loader<String, TransactionDetail, Object> transactionLoader = new Loader<String, TransactionDetail, Object>() {
      @Override
      public TransactionDetail retrieve(Object context, String hash) throws Exception {
        return CachedTransactionStorage.super.getTransactionByHash(hash, false);
      }
    };
    this.transactionFutureCache = new FutureExoCache<>(transactionLoader, transactionCache);
    List<TransactionDetail> pendingTransactions = getPendingTransactions();
    pendingTransactions.forEach(pendingTransaction -> transactionCache.put(pendingTransaction.getHash(), pendingTransaction));
  }

  @Override
  public TransactionDetail getTransactionByHash(String hash, boolean onlyPending) {
    if (onlyPending) {
      return transactionCache.get(hash);
    } else {
      return this.transactionFutureCache.get(null, hash);
    }
  }

  @Override
  public void saveTransactionDetail(TransactionDetail transactionDetail) {
    super.saveTransactionDetail(transactionDetail);
    if (transactionDetail.isPending()) {
      this.transactionCache.put(transactionDetail.getHash(), transactionDetail);
    } else {
      this.transactionFutureCache.remove(transactionDetail.getHash());
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  private CacheService getCacheService() {
    if (cacheService == null) {
      cacheService = CommonsUtils.getService(CacheService.class);
    }
    return cacheService;
  }
}
