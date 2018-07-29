<template>
  <v-app>
    <main>
      <v-layout row>
        <v-flex xs12 sm6 offset-sm3>
          <v-card>
            <v-toolbar color="blue-grey lighten-1" dark>
              <v-toolbar-title>
                My Wallet
              </v-toolbar-title>
              <v-spacer></v-spacer>

              <send-tokens-modal v-show="!error" :contract="contract" @close="dialog = false"></send-tokens-modal>
            </v-toolbar>

            <v-alert :value="error" type="error">
              {{ error }}
            </v-alert>

            <wallet-card v-show="account" :contract="contract" :account="account"></wallet-card>

            <v-container>
              <v-tabs slot="extension" v-model="tab" color="light-blue darken-2" slider-color="yellow accent-2" grow dark>
                <v-tab key="generalTransactions" color="white">All Transactions</v-tab>
                <v-tab key="tokensTransactions" color="white">Token transactions</v-tab>
              </v-tabs>
              <v-tabs-items v-model="tab">
                <v-tab-item key="generalTransactions">
                  <general-transactions :account="account"></general-transactions>
                </v-tab-item>
                <v-tab-item key="tokensTransactions">
                  <token-transactions :account="account" :contract="contract"></token-transactions>
                </v-tab-item>
              </v-tabs-items>
            </v-container>
          </v-card>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import WalletCard from './WalletCard.vue';
import GeneralTransactions from './GeneralTransactions.vue';
import TokenTransactions from './TokenTransactions.vue';
import SendTokensModal from './SendTokensModal.vue';

import TruffleContract from 'truffle-contract';
import LocalWeb3 from 'web3';
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';

export default {
  components: {
    WalletCard,
    GeneralTransactions,
    TokenTransactions,
    SendTokensModal
  },
  data() {
    return {
      web3Provider: null,
      account: null,
      contract: null,
      errorMessage: null,
      tab: 'generalTransactions'
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
      if (value && value.length) {
        this.initContract();
      }
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
                return window.localWeb3.eth.defaultAccount = this.account = account;
              })
              .catch(e => {
                this.errorMessage = e;
              });
          }
        } else {
          this.account = null;
          this.errorMessage = 'Please make sure that you are connected using Metamask with a valid account';
        }
      });
    },
    initContract() {
      const EAW_CONTRACT = TruffleContract({
        abi: ERC20_COMPLIANT_CONTRACT_ABI
      });
      EAW_CONTRACT.setProvider(this.web3Provider);
      EAW_CONTRACT.defaults({
        from: this.account,
        gas: 300000
      });
      this.contract = EAW_CONTRACT.at("0x345ca3e014aaf5dca488057592ee47305d9b3e10");
    }
  }
};
</script>