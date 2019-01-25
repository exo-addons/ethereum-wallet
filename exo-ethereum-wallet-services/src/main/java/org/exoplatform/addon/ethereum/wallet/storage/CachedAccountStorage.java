package org.exoplatform.addon.ethereum.wallet.storage;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.addon.ethereum.wallet.dao.WalletAccountDAO;
import org.exoplatform.addon.ethereum.wallet.model.Wallet;
import org.exoplatform.addon.ethereum.wallet.model.WalletCacheKey;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class CachedAccountStorage extends AccountStorage implements Startable {

  private CacheService                                   cacheService;

  private FutureExoCache<WalletCacheKey, Wallet, Object> walletFutureCache = null;

  public CachedAccountStorage(WalletAccountDAO walletAccountDAO) {
    super(walletAccountDAO);
  }

  @Override
  public void start() {
    ExoCache<WalletCacheKey, Wallet> walletCache = getCacheService().getCacheInstance("wallet.account");
    Loader<WalletCacheKey, Wallet, Object> walletLoader = new Loader<WalletCacheKey, Wallet, Object>() {
      @Override
      public Wallet retrieve(Object context, WalletCacheKey cacheKey) throws Exception {
        if (StringUtils.isNotBlank(cacheKey.getAddress())) {
          return CachedAccountStorage.super.getWalletByAddress(cacheKey.getAddress());
        } else {
          return CachedAccountStorage.super.getWalletByIdentityId(cacheKey.getIdentityId());
        }
      }
    };
    this.walletFutureCache = new FutureExoCache<>(walletLoader, walletCache);
  }

  @Override
  public Wallet getWalletByAddress(String address) {
    Wallet wallet = this.walletFutureCache.get(null, new WalletCacheKey(address));
    return wallet == null ? null : wallet.clone();
  }

  @Override
  public Wallet getWalletByIdentityId(long identityId) {
    Wallet wallet = this.walletFutureCache.get(null, new WalletCacheKey(identityId));
    return wallet == null ? null : wallet.clone();
  }

  @Override
  public void saveWallet(Wallet wallet, boolean isNew) {
    super.saveWallet(wallet, isNew);
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
  }

  @Override
  public Wallet removeWallet(long identityId) {
    Wallet wallet = super.removeWallet(identityId);
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getAddress()));
    this.walletFutureCache.remove(new WalletCacheKey(wallet.getTechnicalId()));
    return wallet;
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
