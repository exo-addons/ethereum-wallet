<template>
  <v-app>
    <main>
      <v-layout row>
        <v-flex v-if="!selectedAccount" xs12 sm6 offset-sm3>
          <v-card>
            <v-toolbar color="pink" dark>
              <v-spacer></v-spacer>
              <v-toolbar-title>Accounts ({{ networkName }})</v-toolbar-title>
              <v-spacer></v-spacer>
              <add-contract-modal :net-id="networkId" :account="account"></add-contract-modal>
            </v-toolbar>
            <v-alert :value="error" type="error">
              {{ error }}
            </v-alert>
            <create-account v-if="!account" @added="init"></create-account>
            <v-list two-line>
              <template v-for="(item, index) in accountsDetails">
                <v-list-tile :key="index" avatar ripple @click="openAccountDetail(item)">
                  <v-list-tile-avatar>
                    <v-icon dark class="purple">{{ item.icon }}</v-icon>
                  </v-list-tile-avatar>
                  <v-list-tile-content>
                    <v-list-tile-title v-html="item.title"></v-list-tile-title>
                    <v-list-tile-sub-title>{{ item.balance }} {{ item.symbol }}</v-list-tile-sub-title>
                  </v-list-tile-content>
                </v-list-tile>
                <v-divider v-if="index + 1 < accountsDetails.length" :key="`divider-${index}`"></v-divider>
              </template>
            </v-list>
          </v-card>
        </v-flex>
        <account-detail v-else :account="account" :contract-detail="selectedAccount" @back="selectedAccount = null"></account-detail>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import CreateAccount from './CreateAccount.vue';
import AccountDetail from './AccountDetail.vue';

import TruffleContract from 'truffle-contract';
import LocalWeb3 from 'web3';
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsDetails} from '../WalletToken.js';

export default {
  components: {
    CreateAccount,
    AccountDetail,
    AddContractModal
  },
  data() {
    return {
      web3Provider: null,
      networkId: null,
      networkName: null,
      account: null,
      lastCheckedAccount: null,
      selectedAccount: null,
      contracts: [],
      accountsDetails: {},
      errorMessage: null
    };
  },
  computed: {
    metamaskEnabled () {
      return web3 && web3.currentProvider && web3.currentProvider.isMetaMask;
    },
    metamaskConnected () {
      return this.metamaskEnabled && web3.currentProvider.isConnected();
    },
    error() {
      if (!this.metamaskEnabled) {
        return 'Please install or Enable Metamask';
      } else if (!this.metamaskConnected) {
        return 'Please connect Metamask to the network';
      } else if (!this.account) {
        return 'Please select a valid account using Metamask';
      }
      return this.errorMessage;
    }
  },
  watch: {
    account(value) {
      if (!value) {
        return;
      }
      this.computeBalance();
    }
  },
  created(){
    if (this.metamaskEnabled && this.metamaskConnected) {
      this.init();
    } else {
      window.addEventListener('load', function() {
        this.init();
      });
    }
  },
  methods: {
    init() {
      this.initWeb3();
      this.initAccount();

      const thiss = this;
      // In case account switched in Metamars
      // See https://github.com/MetaMask/faq/blob/master/DEVELOPERS.md
      setInterval(function() {
        thiss.initAccount();
      }, 1000);
    },
    initWeb3() {
      this.web3Provider = web3.currentProvider;
      window.localWeb3 = new LocalWeb3(this.web3Provider);
    },
    initAccount() {
      window.localWeb3.eth.getCoinbase().then(account => {
        if (account && account.length) {
          if (account !== this.account && this.lastCheckedAccount !== account) {
            account = account.toLowerCase();
            this.lastCheckedAccount = account;
            this.errorMessage = '';
            this.account = null;
            this.accountsDetails = {};
            this.computeNetwork()
              .then(() => {
                const accountDetails = this.accountsDetails[account] = {};
                accountDetails.title = 'Account in ETH';
                accountDetails.icon = 'fab fa-ethereum';
                accountDetails.balance = 0;
                accountDetails.symbol = 'ETH';
                accountDetails.isContract = false;

                window.localWeb3.eth.defaultAccount = this.account = account.toLowerCase();
              })
              .then(() => getContractsDetails(account, this.networkId))
              .then(contractsDetails => {
                if (contractsDetails && contractsDetails.length) {
                  contractsDetails.forEach(contractDetails => {
                    this.accountsDetails[contractDetails.address] = contractDetails;
                  });
                  this.$forceUpdate();
                }
              })
              .catch(err => {
                this.errorMessage = err;
                console.log("not unlocked", err);
              });
          }
        } else {
          this.account = null;
          this.errorMessage = 'Please make sure that you are connected using Metamask with a valid account';
        }
      });
    },
    computeNetwork() {
      return window.localWeb3.eth.net.getId().then(netId => {
        this.networkId = netId;
        return window.localWeb3.eth.net.getNetworkType();
      })
      .then(netType => this.networkName = `${netType.toUpperCase()} Network`);
    },
    computeBalance() {
      window.localWeb3.eth.getCoinbase()
        .then(window.localWeb3.eth.getBalance)
        .then(balance => this.accountsDetails[this.account].balance = window.localWeb3.utils.fromWei(balance, "ether"))
        .then(() => this.$forceUpdate());
    },
    openAccountDetail(accountDetails) {
      this.selectedAccount = accountDetails;
    }
  }
};
</script>