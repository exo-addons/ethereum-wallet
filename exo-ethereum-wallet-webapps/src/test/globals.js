import $ from 'jquery';
import Vue from 'vue';
import Vuetify from '../main/webapp/js/lib/vuetify.min.js';
import LocalWeb3 from 'web3';
import fs from 'fs';

import {toFixed} from '../main/webapp/vue-app/WalletUtils.js';

import {getDefaultSettings, setWalletDetails, getWalletDetailsBTypeId, getParameter} from './TestUtils.js';

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

global.testWeb3 = new LocalWeb3(new LocalWeb3.providers.HttpProvider('http://localhost:8545'));

$.urlParam = function(url, name) {
  const results = new RegExp(`[?&]${name}=([^]*)`).exec(url);
  if (results == null) {
    return null;
  } else {
    return results[1] || 0;
  }
};

global.eXo = {
  env: {
    portal: {
      context: 'portal',
      rest: 'rest',
      language: 'en',
      userName: 'testuser',
      spaceGroup: 'testspace',
    },
  },
};

global.walletAddresses = global.WALLET_ACCOUNTS.map((account) => account.address);
global.defaultWalletAddress = global.walletAddress = global.walletAddresses[1];
global.testNetworkId = 4452364;
global.addressAssociations = {};
global.userAddresses = {};

global.tokenName = 'Curries';
global.tokenSymbol = 'C';
global.tokenOwner = global.walletAddresses[0];
global.tokenDecimals = 5;
global.tokenSupply = 100000 * Math.pow(10, global.tokenDecimals);
global.tokenSellPrice = '0.002';

global.defaultWalletSettings = {
  isWalletEnabled: true,
  isAdmin: false,
  enableDelegation: false,
  isSpaceAdministrator: false,
  defaultPrincipalAccount: 'ether',
  defaultOverviewAccounts: ['ether'],
  defaultContractsToDisplay: ['ether'],
  userPreferences: {
    defaultGas: 0,
  },
};

setWalletDetails('user', global.eXo.env.portal.userName, global.defaultWalletAddress);
setWalletDetails('space', global.eXo.env.portal.spaceGroup, global.walletAddresses[2]);

global.fetch.mockImplementation((url, options) => {
  let resultJson = null;
  let resultText = null;
  if (url.indexOf('/portal/rest/wallet/api/global-settings') === 0) {
    resultJson = getDefaultSettings();
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
    if (!resultJson) {
      console.warn(`Can't find ${type} with id ${id} in `, global.userAddresses);
    }
  } else if (url.indexOf('/portal/rest/wallet/api/contract/bin/') === 0) {
    const contractBinName = url.replace('/portal/rest/wallet/api/contract/bin/', '');
    resultText = fs.readFileSync(`target/contracts/org/exoplatform/addon/ethereum/wallet/contract/${contractBinName}.bin`, 'utf8');
  } else if (url.indexOf('/portal/rest/wallet/api/contract/abi/') === 0) {
    const contractABIName = url.replace('/portal/rest/wallet/api/contract/abi/', '');
    resultText = fs.readFileSync(`target/contracts/org/exoplatform/addon/ethereum/wallet/contract/${contractABIName}.json`, 'utf8');
  } else if (url === '/portal/rest/wallet/api/account/saveAddress') {
    if (!options || !options.body) {
      throw new Error(`URL ${url} has empty parameters`, options);
    }
    const parameters = JSON.parse(options.body);
    setWalletDetails(parameters.type, parameters.id, parameters.address);
  } else if (url.indexOf('/portal/rest/wallet/api/contract/save') === 0) {
    if (!options || !options.body) {
      throw new Error(`URL ${url} has empty parameters`, options);
    }
    const contractDetails = options.body;
    const parameters = JSON.parse(contractDetails);
    fs.writeFileSync(`target/${parameters.address.toLowerCase()}`, contractDetails);
  } else if (url.indexOf(`/portal/rest/wallet/api/contract/getContract`) === 0) {
    const contractAddress = getParameter(url, 'address').toLowerCase();
    if (fs.existsSync(`target/${contractAddress.toLowerCase()}`)) {
      const contractDetails = fs.readFileSync(`target/${contractAddress.toLowerCase()}`, 'utf8');
      resultJson = JSON.parse(contractDetails);
    }
  } else {
    console.warn(new Error(`URL ${url} isn't mocked`));
  }
  return Promise.resolve(resultText ? new Response(resultText) : new Response(JSON.stringify(resultJson)));
});
