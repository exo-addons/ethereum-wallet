import * as constants from './WalletConstants';
import {searchUserOrSpaceObject} from './WalletAddressRegistry';

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
      return computeGasPrice();
    });
}

export function computeGasPrice() {
  if (!window.walletSettings) {
    window.walletSettings = {};
  }
  if (window.localWeb3 && window.localWeb3.currentProvider) {
    return window.localWeb3.eth.getGasPrice()
      .then(gasPrice => {
        window.walletSettings.gasPrice = gasPrice;
        window.walletSettings.gasPriceInEther = gasPrice ? window.localWeb3.utils.fromWei(gasPrice, 'ether'): 0;
      })
      .catch(error => {
        console.debug("computeGasPrice method error: ", error);
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

  if (window.walletSettings.userPreferences.useMetamask
      && window.web3 && window.web3.eth.defaultAccount
      && window.web3.isConnected && window.web3.isConnected()) {

    window.localWeb3 = new LocalWeb3(window.web3.currentProvider);
    window.localWeb3.eth.defaultAccount = window.web3.eth.defaultAccount ? window.web3.eth.defaultAccount.toLowerCase() : null;

    return window.localWeb3.eth.getCoinbase()
      .then(address => {
        if (address) {
          window.walletSettings.metamaskConnected = true;
        } else {
          window.walletSettings.metamaskConnected = false;
          createLocalWeb3Instance(isSpace, true);
        }
        return checkNetworkStatus();
      })
      .catch(e => {
        window.walletSettings.metamaskConnected = false;
        createLocalWeb3Instance(isSpace, true);
        return checkNetworkStatus();
      });
  } else {
    createLocalWeb3Instance(isSpace, window.walletSettings.userPreferences.useMetamask);
    return checkNetworkStatus();
  }
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
          window.walletSettings.userPreferences.useMetamask = localStorage.getItem(`exo-wallet-${username}-metamask`);
          const userP = localStorage.getItem(`exo-wallet-${window.walletSettings.userPreferences.walletAddress}-userp`);
          if (userP) {
            window.walletSettings.userP = userP;
            window.walletSettings.userPreferences.browerWalletStored = true;
          } else {
            window.walletSettings.userPreferences.browerWalletStored = false;
          }
          window.walletSettings.userPreferences.autoGenerated = localStorage.getItem(`exo-wallet-${window.walletSettings.userPreferences.walletAddress}-userp-autoGenerated`);
        }
        if (isSpace && eXo.env.portal.spaceGroup) {
          return initSpaceAccount(eXo.env.portal.spaceGroup)
            .then(retrieveFiatExchangeRate);
        } else {
          return retrieveFiatExchangeRate();
        }
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

export function disableMetamask() {
  const username = eXo.env.portal.userName;
  localStorage.removeItem(`exo-wallet-${username}-metamask`);
  window.walletSettings.userPreferences.useMetamask = false;
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
  return window.localWeb3.eth.net.getId()
    .then((networkId, error) => {
      if (error) {
        throw error;
      }
      if (networkId) {
        window.walletSettings.currentNetworkId = networkId;
        return window.localWeb3.eth.net.getNetworkType();
      } else {
        throw new Error("Network is disconnected");
      }
    })
    .then((netType, error) => {
      if (error) {
        throw error;
      }
      if (netType) {
        window.walletSettings.currentNetworkType = netType;
      }
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

export function saveWallet(password, phrase, address, autoGenerated) {
  password = hashCode(password);
  if (!phrase) {
    phrase = window.walletSettings.userPreferences.phrase;
  }

  if (!address) {
    address = window.walletSettings.userPreferences.walletAddress;
  }

  // Create wallet with user password phrase and personal eXo Phrase generated
  // To avoid having the complete passphrase that allows to decrypt wallet in a single location
  const saved = window.localWeb3.eth.accounts.wallet.save(password + phrase, address);
  if (!saved) {
    throw new Error("An unknown error occrred while saving wallet");
  }

  if (!unlockWallet(password)) {
    throw new Error("An unknown error occrred while unlocking newly saved wallet");
  }

  if (autoGenerated) {
    // Save user passphrase in localStorage to avoid asking him for it eah time he refreshes the page
    localStorage.setItem(`exo-wallet-${address}-userp`, password);
    localStorage.setItem(`exo-wallet-${address}-userp-autoGenerated`, "true");
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp-autoGenerated`);
  }
  window.walletSettings.userP = password;
}


export function getEtherscanlink(networkId)  {
  if (networkId) {
    switch (networkId) {
    case 1:
      return "https://etherscan.io/tx/";
    case 3:
      return "https://ropsten.etherscan.io/tx/";
    }
  }
  return null;
}

export function unlockWallet(password, hash, save) {
  if (hash) {
    password = hashCode(password);
  }

  if (!password || !window.walletSettings.userPreferences.phrase) {
    return false;
  }
  try {
    window.localWeb3.eth.accounts.wallet.load(password + window.walletSettings.userPreferences.phrase,
      window.walletSettings.userPreferences.walletAddress);
  } catch (e) {
    console.debug("error while unlocking wallet", e);
  }
  const unlocked = window.localWeb3.eth.accounts.wallet.length > 0 && window.localWeb3.eth.accounts.wallet[window.walletSettings.userPreferences.walletAddress];
  if (unlocked) {
    if (save) {
      localStorage.setItem(`exo-wallet-${window.walletSettings.userPreferences.walletAddress}-userp`, password);
      localStorage.removeItem(`exo-wallet-${window.walletSettings.userPreferences.walletAddress}-userp-autoGenerated`);
    } else {
      window.walletSettings.userP = password;
    }
  }
  return unlocked;
}

export function removeWallet() {
  return window.localWeb3.eth.accounts.wallet.remove(window.walletSettings.userPreferences.walletAddress);
}

function createLocalWeb3Instance(isSpace, useMetamask) {
  if (window.walletSettings.userPreferences.walletAddress) {
    window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider(window.walletSettings.providerURL));
    window.localWeb3.eth.defaultAccount = window.walletSettings.userPreferences.walletAddress.toLowerCase();

    const accountId = isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName;
    const accountType = isSpace ? 'space' : 'user';

    if (useMetamask
        || !window.walletSettings.userPreferences.walletAddress
        || (isSpace && !window.walletSettings.isSpaceAdministrator)) {
      window.walletSettings.isReadOnly = true;
    } else {
      window.walletSettings.browserWallet = unlockWallet(window.walletSettings.userP);

      let encryptedWalletObject = localStorage.getItem(window.walletSettings.userPreferences.walletAddress);
      if (encryptedWalletObject) {
        encryptedWalletObject = JSON.parse(encryptedWalletObject);
      }

      window.walletSettings.browserWalletExists = encryptedWalletObject !== null && encryptedWalletObject.length > 0 && encryptedWalletObject[0] != null
        && encryptedWalletObject[0].address
        && (encryptedWalletObject[0].address.toLowerCase() === window.walletSettings.userPreferences.walletAddress.toLowerCase()
            || `0x${encryptedWalletObject[0].address.toLowerCase()}` === window.walletSettings.userPreferences.walletAddress.toLowerCase());

      window.walletSettings.isReadOnly = !window.walletSettings.browserWallet;
    }
  } else {
    // Wallet not configured
    throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
  }
}

function isBrowserWallet(id, type, address) {
  address = address.toLowerCase();
  return localStorage.getItem(`exo-wallet-${type}-${id}`) === address;
}

function checkNetworkStatus(waitTime) {
  if (!waitTime) {
    waitTime = 100;
  }
  // Test if network is connected: isListening operation can hang up forever
  let isListening = false;
  window.localWeb3.eth.net.isListening()
    .then(listening => isListening = listening);
  return new Promise((resolve) => setTimeout(resolve, waitTime))
    .then(() => {
      if (!isListening) {
        throw new Error(constants.ERROR_WALLET_DISCONNECTED);
      }
    })
    .then(() => computeNetwork())
    .then(() => computeGasPrice())
    .then(() => constants.OK)
    .catch(error => {
      if (waitTime >= 5000) {
        throw error;
      }
      return checkNetworkStatus(waitTime * 2);
    });
}

function initSpaceAccount(spaceGroup) {
  return searchUserOrSpaceObject(spaceGroup, 'space')
    .then((spaceObject, error) => {
      if (error) {
        throw error;
      }

      if(spaceObject && spaceObject.managers && spaceObject.managers.length
          && spaceObject.managers.indexOf(eXo.env.portal.userName) > -1) {
        return window.walletSettings.isSpaceAdministrator = true;
      } else {
        window.walletSettings.isReadOnly = true;
        return window.walletSettings.isSpaceAdministrator = false;
      }
      return false;
    });
}

function hashCode(s) {
  var h = 0, l = s.length, i = 0;
  if ( l > 0 )
    while (i < l)
      h = (h << 5) - h + s.charCodeAt(i++) | 0;
  return h;
};