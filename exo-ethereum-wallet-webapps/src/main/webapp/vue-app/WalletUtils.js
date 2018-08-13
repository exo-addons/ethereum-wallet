export function etherToUSD(amount) {
  if (window.walletSettings.usdPrice && amount)  {
    return (window.walletSettings.usdPrice * amount).toFixed(2);
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

export function gasToUSD(amount, gasPriceInEther) {
  if (!gasPriceInEther) {
    gasPriceInEther = window.walletSettings.gasPriceInEther;
  }
  if (window.walletSettings.usdPrice && gasPriceInEther && amount)  {
    return (gasPriceInEther * window.walletSettings.usdPrice * amount).toFixed(2);
  }
  return 0;
}

export function retrieveUSDExchangeRate() {
  // Retrieve USD <=> Ether exchange rate
  return window.localWeb3.eth.getGasPrice()
    // gas price returned 1 in space context (strange behavior)
    .then(gasPrice => window.walletSettings.gasPrice = gasPrice)
    .then(() => sessionStorage.getItem('exo-wallet-exchange-rate'))
    .then(exchangeRate => {
      return exchangeRate ? exchangeRate : fetch('https://api.coinmarketcap.com/v1/ticker/ethereum/', {
        referrerPolicy: "no-referrer",
        headers: {
          'Origin': ''
        }
      });
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
      if (content && content.length && content[0].price_usd) {
        sessionStorage.setItem('exo-wallet-exchange-rate', JSON.stringify(content));

        window.walletSettings.usdPrice = parseFloat(content[0].price_usd);
        window.walletSettings.usdPriceLastUpdated = new Date(parseInt(content[0].last_updated) * 1000);
        return true;
      }
    })
    .then(usdPriceRetrieved => {return usdPriceRetrieved ? window.walletSettings.gasPrice: null;})
    .then(gasPrice => {window.walletSettings.gasPriceInEther = gasPrice ? window.localWeb3.utils.fromWei(gasPrice, 'ether'): 0;});
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
    });
}

export function initSettings(isSpace) {
  return getMetamaskCurrentNetworkId(isSpace)
    .then(networkId => fetch(`/portal/rest/wallet/api/global-settings?networkId=${networkId}`, {credentials: 'include'}))
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if (settings && settings.isWalletEnabled) {
        window.walletSettings = settings;
        if (!window.walletSettings.defaultGas) {
          window.walletSettings.defaultGas = 21000;
        }
        if (!window.walletSettings.userDefaultGas) {
          window.walletSettings.userDefaultGas = window.walletSettings.defaultGas;
        }
      }
    })
    .catch(e => {
      console.debug("initSettings method - error", e);
      throw e;
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

export function computeBalance() {
  return window.localWeb3.eth.getBalance(window.localWeb3.eth.defaultAccount)
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
        balanceUSD: etherToUSD(retrievedBalance)
      };
    });
}
