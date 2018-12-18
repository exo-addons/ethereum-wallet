import $ from 'jquery';
import Vue from 'vue';
import Vuetify from "../main/webapp/js/lib/vuetify.min.js";

import LocalWeb3 from "../main/webapp/js/lib/web3.min.js";

global.Vuetify = Vuetify;
global.$ = $;
global.Vue = Vue;
global.LocalWeb3 = LocalWeb3;


Vue.use(Vuetify);

window.testWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider("http://localhost:8545"))

global.eXo = {
  env: {
    portal: {
      context: 'portal',
      rest: 'rest',
      language: 'en'
    }
  }
};

global.fetch = require('jest-fetch-mock');
fetch.mockImplementation(url => {
  let resultJson = null;
  let resultText = '';
  if(url.indexOf('/portal/rest/wallet/api/global-settings') === 0) {
    resultJson = {
        "defaultNetworkId":3, // Configured netword in global settings
        "isWalletEnabled":true, // true if the wallet application is enabled for current user
        "minGasPrice":4000000000, // Cheap gas price choice amount to use when sending a transaction
        "normalGasPrice":8000000000, // Normal gas price choice amount to use when sending a transaction
        "maxGasPrice":15000000000, // Max gas price choice amount to use when sending a transaction
        "dataVersion":2, // Global Settings data version
        "websocketProviderURL":"http://localhost:8545", // Not used in UI, only server side to listen to blockchain events
        "defaultGas":150000, // Default gas limit to use for transactions to send
        "isAdmin":false, // Whether the current user is in /platform/administrators group or not
        "defaultPrincipalAccount":"ether", // Default contract/ether account to display for user in Wallet Application UI
        "defaultContractsToDisplay":[ // Contracts List to display in UI in Wallet Application (may use 'ether' and 'fiat' to display ether account details)
           "ether"
        ],
        "defaultOverviewAccounts":[ // List of accounts configured in administration that the user can display in his wallet
          "ether"
        ],
        "providerURL":"http://localhost:8545", // The blockchain URL to use
        "enableDelegation":false, // Whether the end delegate tokens is enabled or not for current user
        "fundsHolderType":"user", // Funds holder type: 'space' or 'user'
        "fundsHolder":"root", // Funds holder username/spacePrettyName
        "principalContractAdminName":"Admin", // The name to use in UI to replace principal token/contract owner address
        "principalContractAdminAddress":"0x104e1124a0a64dc13b02effdd675b860c66f72d5", // Principal contract administrator
        "initialFundsRequestMessage":"Here a few bucks to get started. Enjoy your Wallet!", // initial funds message, used by administrator only
        "initialFunds":[ // Initial funds configured by administrator that will be used to initialize the community users wallets
          {
             "amount":0.002,
             "address":"ether"
          }
       ],
        "userPreferences":{ // Specific user preferences
           "phrase":"JBkPvc838ZhHBAIKGeKc", // Password generated on Server side, to be combined by user password to be able to unlock wallet
           "dataVersion":0, // User preferences data version
           "currency":"usd", // User currency used to display fiat amounts
           "defaultGas":0, // User gas limit preference
           "walletAddress":"0x104e1124a0a64dc13b02effdd675b860c66f72d5" // associated user address
        },
        "contractBin":"", // Principal ERT Token contract BIN
     };
  }
  return Promise.resolve(resultJson ? new Response(JSON.stringify(resultJson)) : resultText);
});