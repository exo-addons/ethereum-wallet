import * as constants from './WalletConstants';
import {searchUserOrSpaceObject, saveNewAddress} from './WalletAddressRegistry';

export function etherToFiat(amount) {
  if (window.walletSettings.fiatPrice && amount)  {
    return (window.walletSettings.fiatPrice * amount).toFixed(4);
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
    return (gasPriceInEther * window.walletSettings.fiatPrice * amount).toFixed(4);
  }
  return 0;
}

export function retrieveFiatExchangeRate() {
  window.walletSettings.fiatSymbol = window.walletSettings.userPreferences.currency && constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency] ? constants.FIAT_CURRENCIES[window.walletSettings.userPreferences.currency].symbol : '$';

  const currency = window.walletSettings && window.walletSettings.userPreferences.currency ? window.walletSettings.userPreferences.currency : 'usd';
  // Retrieve Fiat <=> Ether exchange rate
  return retrieveFiatExchangeRateOnline(currency)
    .then(content => {
      if (content && content.length && content[0][`price_${currency}`]) {
        localStorage.setItem(`exo-wallet-exchange-${currency}`, JSON.stringify(content));
      } else {
        // Try to get old information from local storage
        content = localStorage.getItem(`exo-wallet-exchange-${currency}`);
        if (content) {
          content = JSON.parse(content);
        }
      }

      window.walletSettings.usdPrice = content ? parseFloat(content[0].price_usd) : 0;
      window.walletSettings.fiatPrice = content ? parseFloat(content[0][`price_${currency}`]) : 0;
      window.walletSettings.priceLastUpdated = content ? new Date(parseInt(content[0].last_updated) * 1000) : null;
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

function retrieveFiatExchangeRateOnline(currency) {
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
      }
    })
    .catch (error => {
      console.debug("error retrieving currency exchange, trying to get exchange from local store", error);
    });
}

export function computeGasPrice() {
  if (!window.walletSettings) {
    window.walletSettings = {};
  }
  if (window.localWeb3 && window.localWeb3.currentProvider) {
    console.debug("Retrieving gas price");
    return window.localWeb3.eth.getGasPrice()
      .then(gasPrice => {
        console.debug("detected Gas price:", gasPrice);

        // Ad 10% of gas price to avoid "transaction underpriced" errors
        window.walletSettings.gasPrice = Math.floor(gasPrice * 1.1);
        // Avoid adding excessive gas price
        if (window.walletSettings.gasPrice > 200000000000) {
          window.walletSettings.gasPrice = 200000000000;
        }
        window.walletSettings.gasPriceInEther = gasPrice ? window.localWeb3.utils.fromWei(gasPrice, 'ether'): 0;
      })
      .catch(error => {
        console.debug("computeGasPrice method error: ", error);
      });
  } else {
    console.debug("Cannot retrieve gas price because no preconfigured provider");
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
      && window.ethereum
      && window.web3
      && window.web3.isConnected && window.web3.isConnected()) {

    const tempWeb3 = new LocalWeb3(window.web3.currentProvider);

    return window.ethereum.enable()
      .then(accounts => {
        window.walletSettings.detectedMetamaskAccount = tempWeb3.eth.defaultAccount = accounts && accounts.length ? accounts[0].toLowerCase() : null;
      })
      .then(() => tempWeb3.eth.getCoinbase())
      .then(address => {
        if (address) {
          window.walletSettings.metamaskConnected = true;
        } else {
          window.walletSettings.metamaskConnected = false;
        }

        // Display wallet in read only mode when selected Metamask account is
        // not the associated one
        if ((isSpace && !window.walletSettings.isSpaceAdministrator) || !window.walletSettings.metamaskConnected ||  !tempWeb3.eth.defaultAccount || (window.walletSettings.userPreferences.walletAddress && tempWeb3.eth.defaultAccount.toLowerCase() !== window.walletSettings.userPreferences.walletAddress)) {
          createLocalWeb3Instance(isSpace, true);
        } else {
          window.localWeb3 = tempWeb3;
          window.walletSettings.isReadOnly = false;
        }
        return checkNetworkStatus();
      })
      .catch(e => {
        console.debug("error retrieving metamask connection status. Consider Metamask as disconnected", e);

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

  clearCache();

  return fetch(`/portal/rest/wallet/api/global-settings?networkId=0&spaceId=${spaceId}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if (settings && (settings.isWalletEnabled ||  settings.isAdmin)) {
        window.walletSettings = window.walletSettings || {};
        window.walletSettings.userPreferences = {};
        window.walletSettings = $.extend(window.walletSettings, settings);
        window.walletSettings.enableDelegation = window.walletSettings.hasOwnProperty('enableDelegation') ? window.walletSettings.enableDelegation : true;
        window.walletSettings.defaultGas = window.walletSettings.defaultGas || 35000;
        window.walletSettings.userPreferences.defaultGas = 
          window.walletSettings.userPreferences.defaultGas || window.walletSettings.defaultGas;
        window.walletSettings.userPreferences.enableDelegation =
          window.walletSettings.userPreferences.hasOwnProperty('enableDelegation') ?
            window.walletSettings.userPreferences.enableDelegation
          : window.walletSettings.enableDelegation;

        if (!window.walletSettings.defaultOverviewAccounts || !window.walletSettings.defaultOverviewAccounts.length) {
          if (window.walletSettings.defaultContractsToDisplay) {
            window.walletSettings.defaultOverviewAccounts = window.walletSettings.defaultContractsToDisplay.slice();
            window.walletSettings.defaultOverviewAccounts.unshift('fiat', 'ether');
          } else {
            window.walletSettings.defaultOverviewAccounts = ['fiat', 'ether'];
          }
        }
        window.walletSettings.defaultPrincipalAccount = window.walletSettings.defaultPrincipalAccount || window.walletSettings.defaultOverviewAccounts[0];
        window.walletSettings.userPreferences.principalAccount = window.walletSettings.userPreferences.principalAccount
          || window.walletSettings.defaultPrincipalAccount;
        window.walletSettings.userPreferences.overviewAccounts = window.walletSettings.userPreferences.overviewAccounts
          || window.walletSettings.defaultOverviewAccounts;

        const username = eXo.env.portal.userName;
        const spaceGroup = eXo.env.portal.spaceGroup;

        const accountId = getRemoteId(isSpace);
        window.walletSettings.userPreferences.useMetamask = localStorage.getItem(`exo-wallet-${accountId}-metamask`);

        const address = window.walletSettings.userPreferences.walletAddress;
        if (address) {
          window.walletSettings.userP = localStorage.getItem(`exo-wallet-${address}-userp`);
          window.walletSettings.storedPassword = window.walletSettings.userP && window.walletSettings.userP.length > 0;
          window.walletSettings.userPreferences.autoGenerated = localStorage.getItem(`exo-wallet-${address}-userp-autoGenerated`);
          window.walletSettings.userPreferences.backedUp = localStorage.getItem(`exo-wallet-${address}-userp-backedup`);
          window.walletSettings.userPreferences.skipBackup = localStorage.getItem(`exo-wallet-${address}-userp-skipBackup`);
          window.walletSettings.userPreferences.skipWalletPasswordSet = localStorage.getItem(`exo-wallet-${address}-skipWalletPasswordSet`);
        }

        if (isSpace && spaceGroup) {
          return initSpaceAccount(spaceGroup)
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

export function enableMetamask(isSpace) {
  const accountId = getRemoteId(isSpace);

  localStorage.setItem(`exo-wallet-${accountId}-metamask`, 'true');
  window.walletSettings.userPreferences.useMetamask = true;
}

export function disableMetamask(isSpace) {
  const accountId = getRemoteId(isSpace);

  localStorage.removeItem(`exo-wallet-${accountId}-metamask`);
  window.walletSettings.userPreferences.useMetamask = false;
}

export function watchTransactionStatus(hash, transactionFinishedcallback) {
  hash = hash.toLowerCase();

  if (!window.watchingTransactions) {
      window.watchingTransactions = {};
  }
  let initWatching = false;
  if (!window.watchingTransactions[hash]) {
      window.watchingTransactions[hash] = [];
      initWatching = true;
  }
  window.watchingTransactions[hash].push(transactionFinishedcallback);
  if(initWatching) {
    waitAsyncForTransactionStatus(hash);
  }
}

export function getTransactionReceipt(hash) {
  return window.localWeb3.eth.getTransactionReceipt(hash);
}

export function computeNetwork() {
  return window.localWeb3.eth.net.getId()
    .then((networkId, error) => {
      if (error) {
        console.debug("Error computing network id", error);
        throw error;
      }
      if (networkId) {
        console.debug("Detected network id:", networkId);
        window.walletSettings.currentNetworkId = networkId;
        return window.localWeb3.eth.net.getNetworkType();
      } else {
        console.debug("Network is disconnected");
        throw new Error("Network is disconnected");
      }
    })
    .then((netType, error) => {
      if (error) {
        console.debug("Error computing network type", error);
        throw error;
      }
      if (netType) {
        window.walletSettings.currentNetworkType = netType;
        console.debug("Detected network type:", netType);
      }
    });
}

export function computeBalance(account) {
  if (!window.localWeb3) {
    return Promise.reject(new Error("You don't have a wallet yet"));
  }
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

export function saveBrowerWalletInstance(wallet, password, isSpace, autoGenerateWallet, backedUp) {
  const account = window.localWeb3.eth.accounts.wallet.add(wallet);
  const address = account['address'].toLowerCase();

  return saveNewAddress(
    isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName,
    isSpace ? 'space' : 'user',
    address,
    true)
    .then((resp, error) => {
      if (error) {
        throw error;
      }
      if (resp && resp.ok) {
        return resp.text();
      } else {
        throw new Error('Error saving new Wallet address');
      }
    })
    .then((phrase, error) => {
      saveBrowerWallet(password, phrase, address, autoGenerateWallet, autoGenerateWallet);
      setWalletBackedUp(address, backedUp);
      disableMetamask();
    });
}

export function saveBrowerWallet(password, phrase, address, autoGenerated, save) {
  if (!phrase) {
    phrase = window.walletSettings.userPreferences.phrase;
  }

  if (!address) {
    address = window.walletSettings.userPreferences.walletAddress;
  }

  if (!password || !password.length) {
    throw new Error("Password is mandatory");
  }
  if (!phrase || !phrase.length) {
    throw new Error("Empty user settings");
  }
  if (!address || !address.length) {
    throw new Error("Address is empty");
  }

  password = hashCode(password);

  // Create wallet with user password phrase and personal eXo Phrase generated
  // To avoid having the complete passphrase that allows to decrypt wallet in a
  // single location
  const saved = window.localWeb3.eth.accounts.wallet.save(password + phrase, address);
  if (!saved || !browserWalletExists(address)) {
    throw new Error("An unknown error occrred while saving new wallet");
  }

  rememberPassword(save || autoGenerated, password, address);

  if (autoGenerated) {
    localStorage.setItem(`exo-wallet-${address}-userp-autoGenerated`, "true");
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp-autoGenerated`);
  }

  if (!unlockBrowerWallet(password, phrase, address)) {
    throw new Error("An unknown error occrred while unlocking newly saved wallet");
  } else {
    lockBrowerWallet(address);
  }
}

export function rememberPassword(remember, password, address) {
  address = address || window.walletSettings.userPreferences.walletAddress;
  if (!address) {
    throw new Error("Can't find address of user");
  }
  if (remember) {
    localStorage.setItem(`exo-wallet-${address}-userp`, password);
    window.walletSettings.userP = password;
    window.walletSettings.storedPassword = true;
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp`);
    window.walletSettings.userP = null;
    window.walletSettings.storedPassword = false;
  }
}

export function getAddressEtherscanlink(networkId)  {
  if (networkId) {
    switch (networkId) {
    case 1:
      return "https://etherscan.io/address/";
    case 3:
      return "https://ropsten.etherscan.io/address/";
    }
  }
  return null;
}

export function getTokenEtherscanlink(networkId)  {
  if (networkId) {
    switch (networkId) {
    case 1:
      return "https://etherscan.io/token/";
    case 3:
      return "https://ropsten.etherscan.io/token/";
    }
  }
  return null;
}

export function getTransactionEtherscanlink(networkId)  {
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

export function getCurrentBrowerWallet() {
  return window && window.localWeb3 && window.localWeb3.eth.accounts.wallet
    && window.walletSettings.userPreferences.walletAddress && window.localWeb3.eth.accounts.wallet[window.walletSettings.userPreferences.walletAddress];
}

export function lockBrowerWallet(address) {
  address = address || window.walletSettings.userPreferences.walletAddress;
  if (address) {
    window.localWeb3.eth.accounts.wallet.remove(address);
  }
}

export function unlockBrowerWallet(password, phrase, address) {
  if (!phrase || !phrase.length) {
    phrase = window.walletSettings.userPreferences.phrase;
  }

  if (!address || !address.length) {
    address = window.walletSettings.userPreferences.walletAddress;
  }
  
  if (!password || !password.length) {
    password = window.walletSettings.userP;
  }

  if (!password || !password.length || !phrase || !phrase.length || !address || !address.length) {
    return false;
  }

  try {
    // lock previously unlocked wallet first
    if (isWalletUnlocked(address)) {
      lockBrowerWallet(address);
    }

    window.localWeb3.eth.accounts.wallet.load(password + phrase, address);
  } catch (e) {
    console.debug("error while unlocking wallet", e);
    return false;
  }

  return isWalletUnlocked(address);
}

function isWalletUnlocked(address) {
  return window.localWeb3.eth.accounts.wallet.length > 0 && window.localWeb3.eth.accounts.wallet[address] && window.localWeb3.eth.accounts.wallet[address].privateKey;
}

export function setWalletBackedUp(address, backedUp) {
  if (!address || !address.length) {
    address = window.walletSettings.userPreferences.walletAddress;
  }
  if (backedUp) {
    localStorage.setItem(`exo-wallet-${address}-userp-backedup`, "true");
  } else {
    localStorage.removeItem(`exo-wallet-${address}-userp-backedup`);
  }
  window.walletSettings.userPreferences.backedUp = backedUp;
}

export function skipWalletBackedUp() {
  const address = window.walletSettings.userPreferences.walletAddress;
  localStorage.setItem(`exo-wallet-${address}-userp-skipBackup`, "true");
  window.walletSettings.userPreferences.skipBackup = true;
}

export function skipWalletPasswordSet() {
  const address = window.walletSettings.userPreferences.walletAddress;
  localStorage.setItem(`exo-wallet-${address}-skipWalletPasswordSet`, "true");
  window.walletSettings.userPreferences.skipWalletPasswordSet = true;
}

export function hashCode(s) {
  var h = 0, l = s.length, i = 0;
  if ( l > 0 )
    while (i < l)
      h = (h << 5) - h + s.charCodeAt(i++) | 0;
  return String(h);
}

export function truncateError(error) {
  if (!error) {
    return '';
  }
  error = String(error);
  if(error.indexOf(' at ') > 0) {
    error = error.substring(0, error.indexOf(' at '));
  }

  if (error.indexOf('replacement transaction underpriced') > 0 || error.indexOf('known transaction') > 0) {
    error = 'Another transaction is in progress please wait until the first transaction is finished';
  }
  return error;
}

export function generatePassword() {
  return Math.random().toString(36).slice(2);
}

export function markFundRequestAsSent(notificationId) {
  return fetch(`/portal/rest/wallet/api/account/markFundRequestAsSent?notificationId=${notificationId}`, {credentials: 'include'})
    .then(resp =>  {
      return resp && resp.ok;
    });
}

export function checkFundRequestStatus(notificationId) {
  return fetch(`/portal/rest/wallet/api/account/fundRequestSent?notificationId=${notificationId}`, {credentials: 'include'})
    .then(resp =>  {
      return resp && resp.ok && resp.text();
    })
    .then(content =>  {
      return "true" === content;
    });
}

export function getWallets() {
  return fetch(`/portal/rest/wallet/api/account/list`, {credentials: 'include'})
    .then(resp =>  {
      return resp && resp.ok && resp.json();
    });
}

export function setDraggable() {
  $("#WalletApp .v-dialog:not(.not-draggable)").draggable();
  $("#WalletAdminApp .v-dialog:not(.not-draggable)").draggable();
}

/*
 * return amount * 10 ^ decimals
 */
export function convertTokenAmountToSend(amount, decimals) {
  const toBN = window.localWeb3.utils.toBN
  return toBN(amount).imul(toBN(10).pow(toBN(decimals))).toString();
}

/*
 * return amount * 10 ^ decimals
 */
export function convertTokenAmountReceived(amount, decimals) {
  const toBN = window.localWeb3.utils.toBN
  return toBN(amount).div(toBN(10).pow(toBN(decimals))).toString();
}

function createLocalWeb3Instance(isSpace, useMetamask) {
  if (window.walletSettings.userPreferences.walletAddress) {
    window.localWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider(window.walletSettings.providerURL));
    window.localWeb3.eth.defaultAccount = window.walletSettings.userPreferences.walletAddress.toLowerCase();

    const accountId = getRemoteId(isSpace);
    const accountType = isSpace ? 'space' : 'user';

    if (useMetamask || (isSpace && !window.walletSettings.isSpaceAdministrator)) {
      window.walletSettings.isReadOnly = true;
    } else {
      window.walletSettings.browserWalletExists = browserWalletExists(window.walletSettings.userPreferences.walletAddress);
      window.walletSettings.isReadOnly = !window.walletSettings.browserWalletExists;
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
        console.debug("The network seems to be disconnected");
        throw new Error(constants.ERROR_WALLET_DISCONNECTED);
      }
    })
    .then(() => computeNetwork())
    .then(() => computeGasPrice())
    .then(() => console.debug("Network status: OK"))
    .then(() => constants.OK)
    .catch(error => {
      if (waitTime >= 5000) {
        throw error;
      }
      waitTime = waitTime * 2;
      console.debug("Reattempt to connect with wait time:", waitTime);
      return checkNetworkStatus(waitTime);
    });
}

function initSpaceAccount(spaceGroup) {
  return searchUserOrSpaceObject(spaceGroup, 'space')
    .then((spaceObject, error) => {
      if (error) {
        throw error;
      }
      if(spaceObject && spaceObject.spaceAdministrator) {
        return window.walletSettings.isSpaceAdministrator = true;
      } else {
        window.walletSettings.isReadOnly = true;
        return window.walletSettings.isSpaceAdministrator = false;
      }
      return false;
    });
}

function waitAsyncForTransactionStatus(hash) {
  getTransactionReceipt(hash)
    .then(receipt => {
      if (receipt) {
        window.localWeb3.eth.getBlock(receipt.blockNumber)
          .then(block => {
            if (block) {
              if (window.watchingTransactions[hash] && window.watchingTransactions[hash].length) {
                window.watchingTransactions[hash].forEach(callback => {
                  callback(receipt, block);
                });
              }
            } else {
              setTimeout(() => {
                waitAsyncForTransactionStatus(hash);
              }, 2000);
            }
          })
          .catch(() => {
            setTimeout(() => {
              waitAsyncForTransactionStatus(hash);
            }, 2000);
          });
      } else {
        setTimeout(() => {
          waitAsyncForTransactionStatus(hash);
        }, 2000);
      }
    })
    .catch(error => {
      if (window.watchingTransactions[hash] && window.watchingTransactions[hash].length) {
        window.watchingTransactions[hash].forEach(callback => {
          callback(null, null);
        });
      }
    });
}

function getRemoteId(isSpace) {
  return isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName;
}

function browserWalletExists(address) {
  let encryptedWalletObject = localStorage.getItem(address);
  if (encryptedWalletObject) {
    encryptedWalletObject = JSON.parse(encryptedWalletObject);
  }

  return encryptedWalletObject !== null && encryptedWalletObject.length > 0 && encryptedWalletObject[0] != null
    && encryptedWalletObject[0].address
    && (encryptedWalletObject[0].address.toLowerCase() === address.toLowerCase()
        || `0x${encryptedWalletObject[0].address.toLowerCase()}` === address.toLowerCase());
}

function clearCache() {
  Object.keys(sessionStorage).forEach(key => {
    // Remove association of (address <=> user/space)
    if (key.indexOf('exo-wallet-address-') === 0) {
      sessionStorage.removeItem(key);
    }
  });
}