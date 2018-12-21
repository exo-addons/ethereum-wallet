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
import {saveBrowerWalletInstance} from '../main/webapp/vue-app/WalletUtils.js';

const {mount} = require('@vue/test-utils');

export function initApp(app) {
  return app.vm.init();
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
  } catch(e) {
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
  if(global.defaultWalletSettings.isSpaceAdministrator) {
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

export function expectHasClass(app, id, className) {
  try {
    expect(app.find(`#${id}`).classes()).toContain(className);
  } catch(e) {
    console.error(`expectHasClass error - app doesn't have element with id ${id} having a class ${className}`);
    throw e;
  }
}

export function expectObjectValueEqual(value, expected, ignoredKeys) {
  expect(expected).not.toBeNull();
  expect(value).not.toBeNull();

  expect(typeof expected).toBe('object');
  expect(typeof value).toBe('object');

  let error = null;

  Object.keys(expected).forEach((key) => {
    if (ignoredKeys && ignoredKeys.indexOf(key) >= 0) {
      return;
    }
    try {
      expect(value.hasOwnProperty(key) || (value.hasOwnProperty('_computedWatchers') && value['_computedWatchers'] && Object.keys(value['_computedWatchers']).indexOf(key) >= 0) || (value.hasOwnProperty('_props') && value['_props'] && Object.keys(value['_props']).indexOf(key) >= 0)).toBeTruthy();
    } catch (e) {
      console.error("can't find element with key in result", key);
      error = e;
      return;
    }
    const expectedValue = expected[key];
    try {
      if (typeof expectedValue === 'boolean') {
        if (expectedValue) {
          expect(value[key]).toBeTruthy();
        } else {
          expect(value[key]).toBeFalsy();
        }
      } else if (typeof expectedValue === 'number') {
        expect(value[key]).toBe(expectedValue);
      } else if (typeof expectedValue === 'string') {
        expect(value[key]).toBe(expectedValue);
      } else if (typeof expectedValue === 'object' && expectedValue.reduceRight) {
        // Array
        expect(value[key]).toEqual(expectedValue);
      } else if (typeof expectedValue === 'object') {
        // Pure object
        expect(value[key]).toEqual(expectedValue);
      } else {
        error = new Error(`cannot find type of key: ${key}`);
        return;
      }
    } catch (e) {
      console.error('Wrong value for key in result', key, ', \r\n -- expectedValue -- \r\n ', expectedValue, '\r\n -- found -- \r\n ', value && value[key] && JSON.parse(JSON.stringify(value[key])));
      error = e;
      return;
    }
  });

  if (error) {
    throw new Error('there is some errors in test, see log below');
  }
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

export function getDefaultSettings() {
  return {
    defaultNetworkId: global.testNetworkId, // Configured netword in global settings
    isWalletEnabled: global.defaultWalletSettings.isWalletEnabled, // true if the wallet application is enabled for current user
    minGasPrice: 4000000000, // Cheap gas price choice amount to use when sending a transaction
    normalGasPrice: 8000000000, // Normal gas price choice amount to use when sending a transaction
    maxGasPrice: 15000000000, // Max gas price choice amount to use when sending a transaction
    dataVersion: 2, // Global Settings data version
    websocketProviderURL: 'http://localhost:8545', // Not used in UI, only server side to listen to blockchain events
    defaultGas: 150000, // Default gas limit to use for transactions to send
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
    },
    contractBin: '', // Principal ERT Token contract BIN, used to estimate gas for tokens transfer
  };
}
