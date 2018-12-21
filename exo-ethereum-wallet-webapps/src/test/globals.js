import $ from 'jquery';
import Vue from 'vue';
import Vuetify from '../main/webapp/js/lib/vuetify.min.js';
import LocalWeb3 from 'web3';

import {toFixed} from '../main/webapp/vue-app/WalletUtils.js';

import {setWalletDetails, getWalletDetailsBTypeId, getParameter, getWalletDetailsBTypeAddress} from './TestUtils.js';

require('./constants.js');

global.fetch = require('jest-fetch-mock');

global.Vuetify = Vuetify;
global.$ = $;
global.Vue = Vue;
global.LocalWeb3 = LocalWeb3;

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);
Vue.prototype.isMaximized = true;
Vue.config.silent = true;

window.testWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider('http://localhost:8545'));

$.urlParam = function(url, name){
  var results = new RegExp('[\?&]' + name + '=([^]*)').exec(url);
  if (results==null){
     return null;
  }
  else{
     return results[1] || 0;
  }
}

global.eXo = {
  env: {
    portal: {
      context: 'portal',
      rest: 'rest',
      language: 'en',
      userName: 'testuser',
    },
  },
};

global.walletAddresses = global.WALLET_ACCOUNTS.map(account => account.address);
global.defaultWalletAddress = global.walletAddress = global.walletAddresses[1];
global.testNetworkId = 4452365;
global.addressAssociations = {};
global.userAddresses = {};
setWalletDetails("user", "testuser", global.defaultWalletAddress);

global.fetch.mockImplementation((url, options) => {
  let resultJson = null;
  if (url.indexOf('/portal/rest/wallet/api/global-settings') === 0) {
    resultJson = {
      defaultNetworkId: global.testNetworkId, // Configured netword in global settings
      isWalletEnabled: true, // true if the wallet application is enabled for current user
      minGasPrice: 4000000000, // Cheap gas price choice amount to use when sending a transaction
      normalGasPrice: 8000000000, // Normal gas price choice amount to use when sending a transaction
      maxGasPrice: 15000000000, // Max gas price choice amount to use when sending a transaction
      dataVersion: 2, // Global Settings data version
      websocketProviderURL: 'http://localhost:8545', // Not used in UI, only server side to listen to blockchain events
      defaultGas: 150000, // Default gas limit to use for transactions to send
      isAdmin: false, // Whether the current user is in /platform/administrators group or not
      defaultPrincipalAccount: 'ether', // Default contract/ether account to display for user in Wallet Application UI
      defaultContractsToDisplay: [
        // Contracts List to display in UI in Wallet Application (may use 'ether' and 'fiat' to display ether account details)
        'ether',
      ],
      defaultOverviewAccounts: [
        // List of accounts configured in administration that the user can display in his wallet
        'ether',
      ],
      providerURL: 'http://localhost:8545', // The blockchain URL to use
      enableDelegation: false, // Whether the end delegate tokens is enabled or not for current user
      fundsHolderType: 'user', // Funds holder type: 'space' or 'user'
      fundsHolder: 'root', // Funds holder username/spacePrettyName
      principalContractAdminName: 'Admin', // The name to use in UI to replace principal token/contract owner address
      principalContractAdminAddress: '0x2d232d448FB0B5b370D3abAD2681399e2002aE2A', // Principal contract administrator
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
        defaultGas: 0, // User gas limit preference
        walletAddress: global.walletAddress, // associated user address
      },
      contractBin: '', // Principal ERT Token contract BIN, used to estimate gas for tokens transfer
    };
  } else if (url === 'https://api.coinmarketcap.com/v1/ticker/ethereum/?convert=usd') {
    resultJson = [
      {
        id: 'ethereum',
        name: 'Ethereum',
        symbol: 'ETH',
        rank: '3',
        price_usd: '93.6172896711',
        price_btc: '0.02663421',
        '24h_volume_usd': '2256800339.33',
        market_cap_usd: '9723872573.0',
        available_supply: '103868341.0',
        total_supply: '103868341.0',
        max_supply: null,
        percent_change_1h: '-0.09',
        percent_change_24h: '5.02',
        percent_change_7d: '4.77',
        last_updated: '1545137119',
      },
    ];
  } else if (url.toLowerCase() === `/portal/rest/wallet/api/account/getTransactions?networkId=${global.testNetworkId}&address=${global.walletAddress}`.toLowerCase()) {
    resultJson = [];
  } else if (url.indexOf('/portal/rest/wallet/api/account/detailsById') === 0) {
    const type = getParameter(url, 'type');
    const id = getParameter(url, 'id');
    resultJson = getWalletDetailsBTypeId(type, id);
    if(!resultJson) {
      console.warn(`Can't find ${type} with id ${id} in `, global.userAddresses);
    }
  } else if (url === '/portal/rest/wallet/api/account/saveAddress') {
    if(!options || !options.body) {
      console.warn(new Error(`URL ${url} has empty parameters`, options));
      return;
    }
    const parameters = JSON.parse(options.body);
    console.log("parameters", parameters);
    setWalletDetails(parameters.type, parameters.id, parameters.address);
  } else {
    console.warn(new Error(`URL ${url} isn't mocked`));
  }
  return Promise.resolve(new Response(JSON.stringify(resultJson)));
});
