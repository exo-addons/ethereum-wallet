import {FIAT_CURRENCIES} from './WalletConstants.js';

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
  const currency = window.walletSettings && window.walletSettings.currency ? window.walletSettings.currency : 'usd';
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
        window.walletSettings.fiatSymbol = window.walletSettings.currency && FIAT_CURRENCIES[window.walletSettings.currency] ? FIAT_CURRENCIES[window.walletSettings.currency].symbol : '$';
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

export function initWeb3(isSpace) {
  if (isSpace) {
    if (!window.walletSettings || !window.walletSettings.providerURL) {
      new Error("Please configure a default space provider URL for Web3");
    }
    if (window.walletSettings.providerURL.indexOf("ws") === 0) {
      window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.WebsocketProvider(window.walletSettings.providerURL));
    } else {
      window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider(window.walletSettings.providerURL));
    }
  } else if (window.web3) {
    if (!window.web3.isConnected || !window.web3.isConnected()) {
      throw new Error("Please connect to Metamask");
    } else {
      window.localWeb3 = new LocalWeb3(window.web3.currentProvider);
      window.localWeb3.eth.defaultAccount = window.web3.eth.defaultAccount;
    }
  } else {
    throw new Error("Please install/enable Metamask to create/access your wallet");
  }

  // Test if network is connected
  let isListening = false;
  window.localWeb3.eth.net.isListening()
    .then(listening => isListening = listening);
  return new Promise((resolve) => setTimeout(resolve, 1000))
    .then(() => {
      if (!isListening) {
        throw new Error("Metamask is disconnected from network");
      }
    })
    .then(() => retrieveGasPrice());
}

export function initSettings(isSpace) {
  return getMetamaskCurrentNetworkId(isSpace)
    .then(networkId => {
      if (!networkId) {
        networkId = 0;
      }
      return fetch(`/portal/rest/wallet/api/global-settings?networkId=${networkId}`, {credentials: 'include'});
    })
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
        if (!window.walletSettings.userDefaultGas) {
          window.walletSettings.userDefaultGas = window.walletSettings.defaultGas;
        }
        return retrieveFiatExchangeRate();
      }
    })
    .catch(e => {
      console.debug("initSettings method - error", e);
      throw e;
    });
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

function getMetamaskCurrentNetworkId(isSpace) {
  let currentNetworkId = 0;
  if (!isSpace && typeof window.web3 !== 'undefined') {
    window.web3.version.getNetwork((e, netId) => currentNetworkId = netId);
    return new Promise((resolve) => setTimeout(resolve, 500))
      .then(() => {
        return currentNetworkId;
      });
  }
  return Promise.resolve(currentNetworkId);
}
