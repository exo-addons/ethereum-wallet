import fs from 'fs';

import WalletApp from '../main/webapp/vue-app/components/WalletApp';
import WalletAppMenu from '../main/webapp/vue-app/components/WalletAppMenu';
import WalletSetup from '../main/webapp/vue-app/components/WalletSetup';
import WalletSummary from '../main/webapp/vue-app/components/WalletSummary';
import WalletAccountsList from '../main/webapp/vue-app/components/WalletAccountsList';
import AccountDetail from '../main/webapp/vue-app/components/AccountDetail';
import WalletSettingsModal from '../main/webapp/vue-app/components/WalletSettingsModal';
import WalletBrowserSetup from '../main/webapp/vue-app/components/WalletBrowserSetup';
import WalletMetamaskSetup from '../main/webapp/vue-app/components/WalletMetamaskSetup';
import WalletBackupModal from '../main/webapp/vue-app/components/WalletBackupModal';
import WalletResetModal from '../main/webapp/vue-app/components/WalletResetModal';

import {deployContract, createNewContractInstanceByName, createNewContractInstanceByNameAndAddress, saveContractAddressAsDefault} from '../main/webapp/vue-app/WalletToken.js';
import {saveBrowerWalletInstance} from '../main/webapp/vue-app/WalletUtils.js';

const {mount} = require('@vue/test-utils');

export function initApp(app) {
  return app.vm.init().then(() => deployTokenContract(global.walletAddresses[0]));
}

export function getWalletApp(isSpace) {
  return mount(WalletApp, {
    stubs: {
      'wallet-setup': WalletSetup,
      'wallet-app-menu': WalletAppMenu,
      'wallet-accounts-list': WalletAccountsList,
      'wallet-settings-modal': WalletSettingsModal,
      'wallet-summary': WalletSummary,
      'account-detail': AccountDetail,
      'wallet-browser-setup': WalletBrowserSetup,
      'wallet-metamask-setup': WalletMetamaskSetup,
      'wallet-backup-modal': WalletBackupModal,
      'wallet-reset-modal': WalletResetModal,
    },
    propsData: {
      isSpace: isSpace,
    },
    attachToDocument: true,
  });
}

export function expectCountElement(app, id, count) {
  try {
    expect(app.findAll(`#${id}`)).toHaveLength(count);
  } catch (e) {
    console.error(`expectCountElement error - app doesn't have element with id ${id} count = ${count}`);
    throw e;
  }
}

export function setWalletDetails(type, id, walletAddress, name) {
  const details = {
    avatar: `/rest/v1/social/users/${id}/avatar`,
    technicalId: '2',
    spaceAdministrator: false,
    enabled: true,
    address: walletAddress,
    name: name ? name : 'NAME',
    type: type,
    id: id,
  };

  global.addressAssociations[walletAddress.toLowerCase()] = details;
  global.userAddresses[`${type}_${id}`.toLowerCase()] = details;
}

export function getWalletDetailsBTypeId(type, id) {
  const walletDetails = global.userAddresses[`${type}_${id}`.toLowerCase()];
  if (global.defaultWalletSettings.isSpaceAdministrator) {
    walletDetails.spaceAdministrator = true;
  }
  return walletDetails;
}

export function getWalletDetailsBTypeAddress(walletAddress) {
  return global.addressAssociations[walletAddress.toLowerCase()];
}

export function getEtherAccountDetails(walletAddress, balance, balanceFiat) {
  return {
    title: 'ether',
    icon: 'fab fa-ethereum',
    symbol: 'ether',
    isContract: false,
    address: walletAddress,
    balance: balance,
    balanceFiat: balanceFiat,
  };
}

export function getTokenAccountDetails(address, balance) {
  return {
    isDefault: true,
    isContract: true,
    contractType: 1,
    contractTypeLabel: 'ERT Token',
    networkId: global.testNetworkId,
    address: global.tokenAddress,
    name: global.tokenName,
    symbol: global.tokenSymbol,
    decimals: global.tokenDecimals,
    owner: global.tokenOwner,
    sellPrice: Number(global.tokenSellPrice),
    title: global.tokenName,
    balance: balance ? Number(balance) : global.tokenSupply / Math.pow(10, global.tokenDecimals),
  };
}

export function expectHasClass(app, id, className) {
  try {
    expect(app.find(`#${id}`).classes()).toContain(className);
  } catch (e) {
    console.error(`expectHasClass error - app doesn't have element with id ${id} having a class ${className}`);
    throw e;
  }
}

export function expectObjectValueEqual(value, expected, ignoredKeys, notStrictObjectComparaison) {
  expect(expected).not.toBeNull();
  expect(value).not.toBeNull();

  expect(typeof expected).toBe('object');
  expect(typeof value).toBe('object');

  const errors = [];

  try {
    Object.keys(expected).forEach((key) => {
      if (ignoredKeys && ignoredKeys.indexOf(key) >= 0) {
        return;
      }
      try {
        expect(value.hasOwnProperty(key) || (value.hasOwnProperty('_computedWatchers') && value['_computedWatchers'] && Object.keys(value['_computedWatchers']).indexOf(key) >= 0) || (value.hasOwnProperty('_props') && value['_props'] && Object.keys(value['_props']).indexOf(key) >= 0)).toBeTruthy();
      } catch (e) {
        console.error(`Can't find element with key ${key} in result`);
        errors.push(e);
        return;
      }
      compareValues(key, expected[key], value[key], ignoredKeys, notStrictObjectComparaison, errors);
    });
  } catch (e) {
    errors.push(e);
  }

  if (errors.length) {
    throw new Error('there is some errors in test, see log below', ...errors);
  }
}

function compareValues(key, expectedValue, receivedValue, ignoredKeys, notStrictObjectComparaison, errors) {
  try {
    if (typeof expectedValue === 'boolean') {
      // Boolean
      if (expectedValue) {
        expect(receivedValue).toBeTruthy();
      } else {
        expect(receivedValue).toBeFalsy();
      }
    } else if (typeof expectedValue === 'number') {
      // Number
      if (notStrictObjectComparaison) {
        expect(Number(receivedValue)).toBe(Number(expectedValue));
      } else {
        expect(receivedValue).toBe(expectedValue);
      }
    } else if (typeof expectedValue === 'string') {
      // String
      if (notStrictObjectComparaison) {
        expect(String(receivedValue)).toBe(String(expectedValue));
      } else {
        expect(receivedValue).toBe(expectedValue);
      }
    } else if (typeof expectedValue === 'object' && expectedValue.reduceRight) {
      // Array
      expect(receivedValue).toEqual(expectedValue);
    } else if (typeof expectedValue === 'object') {
      // Object
      if (notStrictObjectComparaison) {
        Object.keys(expectedValue).forEach((subKey) => {
          if (ignoredKeys && ignoredKeys.indexOf(subKey) >= 0) {
            return;
          }
          if (!receivedValue || !receivedValue.hasOwnProperty(subKey)) {
            console.error(`receivedValue doesn't have sub-key ${subKey}`);
            throw new Error(`Wrong value for sub-key = ${subKey} \r\n -- expectedValue -- \r\n ${expectedValue} \r\n -- found -- \r\n ${JSON.parse(stringify(receivedValue))}`);
          }
          const error = compareValues(subKey, expectedValue[subKey], receivedValue[subKey], ignoredKeys, notStrictObjectComparaison, errors);
          if (error) {
            errors.push(error);
          }
        });
      } else {
        // Pure object
        expect(receivedValue).toEqual(expectedValue);
      }
    } else {
      errors.push(new Error(`cannot find type of key: ${key}`));
      return;
    }
  } catch (e) {
    console.warn('Wrong value for key = ', key, ' in result, \r\n -- expectedValue -- \r\n ', expectedValue, '\r\n -- found -- \r\n ', typeof receivedValue === 'object' ? JSON.parse(stringify(receivedValue)) : receivedValue, e);
    errors.push(e);
  }
}

export function sendEther(from, to, amount) {
  amount = window.localWeb3.utils.toWei(String(amount), 'ether').toString();
  return window.localWeb3.eth.sendTransaction({
    from: from,
    to: to,
    value: amount,
    gas: window.walletSettings.userPreferences.defaultGas,
    gasPrice: window.walletSettings.maxGasPrice,
  });
}

export function sendTokens(contract, from, to, amount) {
  return contract.methods.transfer(to, amount * Math.pow(10, global.tokenDecimals)).send({
    from: from,
    gas: window.walletSettings.userPreferences.defaultGas,
    gasPrice: window.walletSettings.maxGasPrice,
  });
}

export function initiateBrowserWallet(address, password, isSpace, generated, backedUp) {
  const walletDetails = global.WALLET_ACCOUNTS.find((wallet) => wallet.address === address);
  if (!walletDetails) {
    throw new Error("Can't find wallet private key for address", address);
  }
  window.localWeb3.eth.accounts.wallet.clear();
  const wallet = window.localWeb3.eth.accounts.wallet.add(walletDetails.secretKey);
  return saveBrowerWalletInstance(wallet, password, isSpace, generated, backedUp);
}

export function getParameter(url, param) {
  let urlPart;
  // eslint-disable-next-line no-useless-escape
  if (!param || !(url = url && url.trim()) || url.indexOf('?') < 0 || !(urlPart = url.match(new RegExp(`[\?&]{1}${param}=[^&#]*`)))) {
    return null;
  }
  return urlPart.length ? urlPart[0].split('=')[1] : null;
}

export function deployTokenContract(adminAddress) {
  try {
    global.tokenAddress = fs.readFileSync('target/contractAddress.txt', 'utf-8');
    if (global.tokenAddress) {
      global.contractAbi = JSON.parse(fs.readFileSync('target/contractAbi', 'utf-8'));
      global.contractBin = fs.readFileSync('target/contractBin', 'utf-8');
      return;
    }
  } catch (e) {
    console.debug('Token address not found from cache');
  }

  const gasLimit = 10000000;
  const gasPrice = 4000000000;

  let ertDataContractInstance, ertERTTokenImplInstance, ertTokenContractInstance;
  let ertDataContractAddress, ertERTTokenImplAddress, ertTokenContractAddress;

  // Deploy Data contract
  return createNewContractInstanceByName('ERTTokenDataV1')
    .then((newContractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!newContractInstance) {
        throw new Error('Cannot instantiate contract ERTTokenDataV1');
      }
      ertDataContractInstance = newContractInstance;
      return deployContract(ertDataContractInstance, adminAddress, gasLimit, gasPrice);
    })
    .then((newContractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!newContractInstance || !newContractInstance.options || !(ertDataContractAddress = newContractInstance.options.address)) {
        throw new Error('Cannot find address of newly deployed address');
      }
      ertDataContractInstance = newContractInstance;
    })
    // Verify attributes
    .then(() => ertDataContractInstance.methods.implementation().call())
    .then((owner) => {
      expect(owner).not.toBeNull();
      expect(owner.toLowerCase()).toBe(adminAddress.toLowerCase());
    })
    // Deploy implementation contract
    .then(() => createNewContractInstanceByName('ERTTokenV1'))
    .then((newContractInstance, error) => {
      if (!newContractInstance) {
        throw new Error('Cannot instantiate contract ERTTokenV1');
      }
      ertERTTokenImplInstance = newContractInstance;
      global.contractAbi = ertERTTokenImplInstance.abi;
      global.contractBin = ertERTTokenImplInstance.bin;
      fs.writeFileSync('target/contractAbi', JSON.stringify(global.contractAbi));
      fs.writeFileSync('target/contractBin', global.contractBin);
      return deployContract(ertERTTokenImplInstance, adminAddress, gasLimit, gasPrice);
    })
    .then((newContractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!newContractInstance || !newContractInstance.options || !(ertERTTokenImplAddress = newContractInstance.options.address)) {
        throw new Error('Cannot find address of newly deployed address');
      }
      ertERTTokenImplInstance = newContractInstance;
    })
    // Verify attributes
    .then(() => ertERTTokenImplInstance.methods.owner().call())
    .then((owner) => {
      expect(owner).not.toBeNull();
      expect(owner.toLowerCase()).toBe(adminAddress.toLowerCase());
    })
    // Deploy proxy contract
    .then(() => createNewContractInstanceByName('ERTToken', ertERTTokenImplAddress, ertDataContractAddress))
    .then((newContractInstance, error) => {
      ertERTTokenImplInstance = newContractInstance;
      if (!newContractInstance) {
        throw new Error('Cannot instantiate contract ERTToken');
      }
      ertTokenContractInstance = newContractInstance;
      return deployContract(ertTokenContractInstance, adminAddress, gasLimit, gasPrice);
    })
    .then((newContractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!newContractInstance || !newContractInstance.options || !(ertTokenContractAddress = newContractInstance.options.address)) {
        throw new Error('Cannot find address of newly deployed address');
      }
      ertTokenContractInstance = newContractInstance;
    })
    // Verify attributes
    .then(() => ertTokenContractInstance.methods.implementationAddress().call())
    .then((implementationAddress) => {
      expect(implementationAddress).not.toBeNull();
      expect(implementationAddress.toLowerCase()).toBe(ertERTTokenImplAddress.toLowerCase());
    })
    .then(() => ertTokenContractInstance.methods.getDataAddress(1).call())
    .then((dataAddress) => {
      expect(dataAddress).not.toBeNull();
      expect(dataAddress.toLowerCase()).toBe(ertDataContractAddress.toLowerCase());
    })
    .then(() => ertTokenContractInstance.methods.owner().call())
    .then((owner) => {
      expect(owner).not.toBeNull();
      expect(owner.toLowerCase()).toBe(adminAddress.toLowerCase());
    })
    // Transfer ownership to proxy and real implementation
    .then(() =>
      ertDataContractInstance.methods.transferDataOwnership(ertTokenContractAddress, ertERTTokenImplAddress).send({
        from: adminAddress,
        gasPrice: gasPrice,
        gas: gasLimit,
      })
    )
    // Refresh Proxy Token contract instance with Impl ABI
    .then(() => createNewContractInstanceByNameAndAddress('ERTTokenV1', ertTokenContractAddress))
    .then((contractInstance) => (ertTokenContractInstance = contractInstance))
    // Initialize Token attributes
    .then(() =>
      ertTokenContractInstance.methods.initialize(global.tokenSupply, global.tokenName, global.tokenDecimals, global.tokenSymbol).send({
        from: adminAddress,
        gasPrice: gasPrice,
        gas: gasLimit,
      })
    )
    // Verify attributes
    .then(() => ertTokenContractInstance.methods.totalSupply().call())
    .then((totalSupply) => {
      expect(totalSupply).not.toBeNull();
      expect(Number(totalSupply)).toBe(Number(global.tokenSupply));
    })
    .then(() => ertTokenContractInstance.methods.name().call())
    .then((name) => {
      expect(name).not.toBeNull();
      expect(name).toBe(global.tokenName);
    })
    .then(() => ertTokenContractInstance.methods.decimals().call())
    .then((decimals) => {
      expect(decimals).not.toBeNull();
      expect(Number(decimals)).toBe(Number(global.tokenDecimals));
    })
    .then(() => ertTokenContractInstance.methods.symbol().call())
    .then((symbol) => {
      expect(symbol).not.toBeNull();
      expect(symbol).toBe(global.tokenSymbol);
    })
    .then(() => ertTokenContractInstance.methods.balanceOf(adminAddress).call())
    .then((adminBalance) => {
      expect(adminBalance).not.toBeNull();
      expect(Number(adminBalance)).toBe(Number(global.tokenSupply));
    })

    // Save contract details
    .then(() =>
      saveContractAddressAsDefault({
        networkId: global.testNetworkId,
        address: ertTokenContractAddress,
        isContract: true,
        name: global.tokenName,
        symbol: global.tokenSymbol,
        decimals: global.tokenDecimals,
      })
    )
    // Return Token address
    .then(() => {
      global.tokenAddress = ertTokenContractAddress.toLowerCase();
      fs.writeFileSync('target/contractAddress.txt', global.tokenAddress);
      return ertTokenContractAddress;
    })
    .catch((e) => {
      console.error('Error deploying contracts', e);
      throw e;
    });
}

export function getDefaultSettings() {
  return {
    defaultNetworkId: global.testNetworkId, // Configured netword in global settings
    isWalletEnabled: global.defaultWalletSettings.isWalletEnabled, // true if the wallet application is enabled for current user
    minGasPrice: 4000000000, // Cheap gas price choice amount to use when sending a transaction
    normalGasPrice: 8000000000, // Normal gas price choice amount to use when sending a transaction
    maxGasPrice: 15000000000, // Max gas price choice amount to use when sending a transaction
    dataVersion: 2, // Global Settings data version
    websocketProviderURL: 'http://localhost:8545', // Not used in UI, only server side to listen to blockchain events
    defaultGas: 1500000, // Default gas limit to use for transactions to send
    isAdmin: false, // Whether the current user is in /platform/administrators group or not
    defaultPrincipalAccount: global.defaultWalletSettings.defaultPrincipalAccount, // Default contract/ether account to display for user in Wallet Application UI
    // Contracts List to display in UI in Wallet Application (may use 'ether' and 'fiat' to display ether account details)
    defaultContractsToDisplay: global.defaultWalletSettings.defaultContractsToDisplay,
    // List of accounts configured in administration that the user can display in his wallet
    defaultOverviewAccounts: global.defaultWalletSettings.defaultOverviewAccounts,
    providerURL: 'http://localhost:8545', // The blockchain URL to use
    enableDelegation: false, // Whether the end delegate tokens is enabled or not for current user
    fundsHolderType: 'user', // Funds holder type: 'space' or 'user'
    fundsHolder: 'root', // Funds holder username/spacePrettyName
    principalContractAdminName: 'Admin', // The name to use in UI to replace principal token/contract owner address
    principalContractAdminAddress: global.walletAddresses[0], // Principal contract administrator
    initialFundsRequestMessage: 'Here a few bucks to get started. Enjoy your Wallet!', // initial funds message, used by administrator only
    initialFunds: [
      // Initial funds configured by administrator that will be used to initialize the community users wallets
      {
        amount: 0.002,
        address: 'ether',
      },
    ],
    userPreferences: {
      // Specific user preferences
      phrase: 'JBkPvc838ZhHBAIKGeKc', // Password generated on Server side, to be combined by user password to be able to unlock wallet
      dataVersion: 0, // User preferences data version
      currency: 'usd', // User currency used to display fiat amounts
      defaultGas: global.defaultWalletSettings.userPreferences.defaultGas, // User gas limit preference
      walletAddress: global.walletAddress, // associated user address
      overviewAccountsToDisplay: global.defaultWalletSettings.defaultOverviewAccounts, // user contracts to display
    },
    contractBin: global.contractBin, // Principal ERT Token contract BIN, used to estimate gas for tokens transfer
    contractAbi: global.contractAbi, // Principal ERT Token contract ABI
  };
}

function stringify(obj) {
  const getCircularReplacer = () => {
    const seen = new WeakSet();
    return (key, value) => {
      if (typeof value === 'object' && value !== null) {
        if (seen.has(value)) {
          return;
        }
        seen.add(value);
      }
      return value;
    };
  };
  return JSON.stringify(obj, getCircularReplacer());
}
