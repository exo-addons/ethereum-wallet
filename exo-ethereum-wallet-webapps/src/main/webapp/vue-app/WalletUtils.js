import * as constants from './WalletConstants';
import {isBrowserWallet} from './WalletAddressRegistry';

export function etherToFiat(amount) {
  if (window.walletSettings.fiatPrice && amount)  {
    return (window.walletSettings.fiatPrice * amount).toFixed(2);
  }
  return 0;
}

export function gasToEther(amount, gasPriceInEther) {
  if (!gasPriceInEther) {
    gasPriceInEther = window.walletSettings.gasPriceInEther;
  }
  if (gasPriceInEther && amount)  {
    return (gasPriceInEther * amount);
  }
  return 0;
}

export function gasToFiat(amount, gasPriceInEther) {
  if (!gasPriceInEther) {
    gasPriceInEther = window.walletSettings.gasPriceInEther;
  }
  if (window.walletSettings.fiatPrice && gasPriceInEther && amount)  {
    return (gasPriceInEther * window.walletSettings.fiatPrice * amount).toFixed(2);
  }
  return 0;
}

export function retrieveFiatExchangeRate() {
  const currency = window.walletSettings && window.walletSettings.userPreferences.currency ? window.walletSettings.userPreferences.currency : 'usd';
  // Retrieve Fiat <=> Ether exchange rate
  return fetch(`https://api.coinmarketcap.com/v1/ticker/ethereum/?convert=${currency}`, {
      referrerPolicy: "no-referrer",
      headers: {
        'Origin': ''
      }
    })
    .then(resp => {
      if (resp && resp.ok) {
        return resp.json();
      } else if(typeof resp === 'string' && resp.length) {
        return JSON.parse(resp);
      } else {
        return null;
      }
    })
    .then(content => {
      if (content && content.length && content[0][`price_${currency}`]) {
        window.walletSettings.usdPrice = parseFloat(content[0].price_usd);
        window.walletSettings.fiatPrice = parseFloat(content[0][`price_${currency}`]);
        window.walletSettings.priceLastUpdated = new Date(parseInt(content[0].last_updated) * 1000);
        window.walletSettings.fiatSymbol = window.walletSettings.userPreferences.currency && constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency] ? constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency].symbol : '$';
      }
    })
    .then(() => {
      if (window.retrieveFiatExchangeRateInterval) {
        clearInterval(window.retrieveFiatExchangeRateInterval);
      }
      window.retrieveFiatExchangeRateInterval = setTimeout(() => {
        retrieveFiatExchangeRate();
      }, 300000);
      return retrieveGasPrice();
    });
}

export function retrieveGasPrice() {
  if (!window.walletSettings) {
    window.walletSettings = {};
  }
  if (window.localWeb3) {
    return window.localWeb3.eth.getGasPrice()
      .then(gasPrice => {
        window.walletSettings.gasPrice = gasPrice;
        window.walletSettings.gasPriceInEther = gasPrice ? window.localWeb3.utils.fromWei(gasPrice, 'ether'): 0;
      });
  }
}

export function initEmptyWeb3Instance() {
  window.localWeb3 = new LocalWeb3();
}

export function initWeb3(isSpace) {
  if (!window.walletSettings || !window.walletSettings.userPreferences) {
    // User settings aren't loaded
    throw new Error(constants.ERROR_WALLET_SETTINGS_NOT_LOADED);
  }

  if (window.walletSettings.userPreferences.useMetamask) {
    if (!window.web3 || !window.web3.isConnected || !window.web3.isConnected()) {
      // Please connect to Metamask
      throw new Error(constants.ERROR_METAMASK_NOT_CONNECTED);
    }
    window.localWeb3 = new LocalWeb3(window.web3.currentProvider);
    window.localWeb3.eth.defaultAccount = window.web3.eth.defaultAccount;
  } else if (window.walletSettings.userPreferences.walletAddress) {
    if (!window.walletSettings || !window.walletSettings.providerURL) {
      // Wrong Wallet settings
      throw new Error(constants.ERROR_WRONG_WALLET_SETTINGS);
    }
    window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider(window.walletSettings.providerURL));
    window.localWeb3.eth.defaultAccount = window.walletSettings.userPreferences.walletAddress;

    if (!isSpace) {
      const accountId = isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName;
      const accountType = isSpace ? 'space' : 'user';
      if(!isBrowserWallet(accountId, accountType, window.walletSettings.userPreferences.walletAddress)) {
        throw new Error(constants.ERROR_WALLET_CONFIGURED_ADDRESS_NOT_FOUND);
      }
    }
  } else {
    // Wallet not configured
    throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
  }

  // Test if network is connected
  let isListening = false;
  window.localWeb3.eth.net.isListening()
    .then(listening => isListening = listening);
  return new Promise((resolve) => setTimeout(resolve, 1000))
    .then(() => {
      if (!isListening) {
        throw new Error(constants.ERROR_WALLET_DISCONNECTED);
      }
    })
    .then(() => retrieveGasPrice())
    .then(() => constants.OK);
}

export function initSettings(isSpace) {
  const spaceId = isSpace ? eXo.env.portal.spaceGroup : '';
  return fetch(`/portal/rest/wallet/api/global-settings?networkId=0&spaceId=${spaceId}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if (settings && settings.isWalletEnabled) {
        if (!window.walletSettings) {
          window.walletSettings = {};
        }
        Object.keys(settings).forEach(key => {
          window.walletSettings[key] = settings[key];
        });
        if (!window.walletSettings.defaultGas) {
          window.walletSettings.defaultGas = 21000;
        }
        if (!window.walletSettings.userPreferences) {
          window.walletSettings.userPreferences = {};
        }
        if (!window.walletSettings.userPreferences.userDefaultGas) {
          window.walletSettings.userPreferences.userDefaultGas = window.walletSettings.defaultGas;
        }
        if (window.walletSettings.userPreferences.walletAddress) {
          const username = eXo.env.portal.userName;
          window.walletSettings.userPreferences.useMetamask = localStorage.getItem(`exo-wallet-${username}-metamask`) === 'true';
          console.log("window.walletSettings.userPreferences.useMetamask", window.walletSettings.userPreferences.useMetamask);
        }
        return retrieveFiatExchangeRate();
      }
    })
    .catch(e => {
      console.debug("initSettings method - error", e);
      throw e;
    });
}

export function enableMetamask() {
  const username = eXo.env.portal.userName;
  localStorage.setItem(`exo-wallet-${username}-metamask`, 'true');
  window.walletSettings.userPreferences.useMetamask = true;
}

export function watchTransactionStatus(hash, transactionFinishedcallback) {
  // Because no websocket connection is allowed using metamask,
  // we will watch the transaction status periodically
  getTransactionReceipt(hash)
    .then(receipt => {
      if (receipt) {
        window.localWeb3.eth.getBlock(receipt.blockNumber)
          .then(block => {
            if (block) {
              transactionFinishedcallback(receipt, block);
            } else {
              setTimeout(() => {
                watchTransactionStatus(hash, transactionFinishedcallback);
              }, 2000);
            }
          });
      } else {
        setTimeout(() => {
          watchTransactionStatus(hash, transactionFinishedcallback);
        }, 2000);
      }
    });
}

export function getTransactionReceipt(hash) {
  return window.localWeb3.eth.getTransactionReceipt(hash);
}

export function computeNetwork() {
  let netId = null;
  return window.localWeb3.eth.net.getId()
    .then((networkId, error) => {
      if (error) {
        throw error;
      }
      if (networkId) {
        if (window.walletSettings) {
          window.walletSettings.currentNetworkId = netId = networkId;
        } else {
          window.currentMetamaskNetworkId = netId = networkId;
        }
        return window.localWeb3.eth.net.getNetworkType();
      } else {
        throw new Error("Network is disconnected");
      }
    })
    .then((netType, error) => {
      if (error) {
        throw error;
      }
      return {
        netId: netId,
        netType: netType
      };
    });
}

export function computeBalance(account) {
  return window.localWeb3.eth.getBalance(account)
    .then((retrievedBalance, error) => {
      if (error) {
        throw error;
      }
      return retrievedBalance = window.localWeb3.utils.fromWei(retrievedBalance, "ether");
    })
    .then((retrievedBalance, error) => {
      if (error) {
        throw error;
      }
      return {
        balance: retrievedBalance,
        balanceFiat: etherToFiat(retrievedBalance)
      };
    });
}
