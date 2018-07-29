<template>
  <v-app>
    <main>
      <v-layout row>
        <v-flex v-if="!selectedAccount" xs12 sm6 offset-sm3>
          <v-card>
            <v-toolbar color="pink" dark>
              <v-spacer></v-spacer>
              <v-toolbar-title>Accounts</v-toolbar-title>
              <v-spacer></v-spacer>
              <add-contract-modal :account="account"></add-contract-modal>
            </v-toolbar>
            <v-alert :value="error" type="error">
              {{ error }}
            </v-alert>
            <v-list two-line>
              <template v-for="(item, index) in accountsDetails">
                <v-list-tile :key="index" avatar ripple @click="openAccountDetail(item)">
                  <v-list-tile-avatar>
                    <v-icon>fa-ethereum</v-icon>
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
import AccountDetail from './AccountDetail.vue';

import TruffleContract from 'truffle-contract';
import LocalWeb3 from 'web3';
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsDetails} from '../WalletToken.js';

export default {
  components: {
    AccountDetail,
    AddContractModal
  },
  data() {
    return {
      web3Provider: null,
      account: null,
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
        return 'Please select an account using Metamask';
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
      this.computeNetwork();
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
          if (account !== this.account) {
            window.localWeb3.eth.personal.unlockAccount(account)
              .then(() => {
                const accountDetails = this.accountsDetails[account] = {};

                accountDetails.title = 'Account in ETH';
                accountDetails.icon = 'fa-ethereum';
                accountDetails.balance = 0;
                accountDetails.symbol = 'ETH';
                accountDetails.isContract = false;

                getContractsDetails(account)
                  .then(contractsDetails => {
                    if (contractsDetails && contractsDetails.length) {
                      contractsDetails.forEach(contractDetails => {
                        this.accountsDetails[contractDetails.address] = contractDetails;
                      });
                      this.$forceUpdate();
                    }
                  })
                  .catch((e) => this.errorMessage = `Error retrieving contracts ${e}`);

                return window.localWeb3.eth.defaultAccount = this.account = account;
              })
              .catch(e => {
                this.errorMessage = `${e}`;
              });
          }
        } else {
          this.account = null;
          this.errorMessage = 'Please make sure that you are connected using Metamask with a valid account';
        }
      });
    },
    computeNetwork() {
      const thiss = this;
      window.localWeb3.eth.net.getNetworkType((err, netType) => {
        if(err) {
          thiss.errorMessage = err;
          return;
        }
        thiss.networkName = `${netType.toUpperCase()} Network`;
      });
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