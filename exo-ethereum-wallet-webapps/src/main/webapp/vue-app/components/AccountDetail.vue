<template>
  <v-layout row>
    <v-flex xs12 sm6 offset-sm3>
      <v-card class="text-xs-center">
        <v-card-media class="purple white--text" height="200px">
          <v-layout column fill-height>
            <v-card-title>
              <v-btn dark icon @click="$emit('back')">
                <v-icon>arrow_back</v-icon>
              </v-btn>

              <v-spacer></v-spacer>
              <div class="title">{{ contractDetail.title }}</div>
              <v-spacer></v-spacer>
            </v-card-title>

            <v-card-title class="white--text pl-5 pt-5">
              <v-spacer></v-spacer>
              <div class="display-1">{{ contractDetail.balance }} {{ contractDetail.symbol }}</div>
              <v-spacer></v-spacer>
            </v-card-title>
          </v-layout>
        </v-card-media>

        <div v-if="contractDetail.isContract && (contractDetail.balance > 0 || hasDelegatedTokens)" class="text-xs-center">
          <send-tokens-modal v-if="contractDetail.balance > 0" :contract="contractDetail.contract" @loading="loading = true" @end-loading="loading = false"></send-tokens-modal>
          <delegate-tokens-modal v-if="contractDetail.balance > 0" :contract="contractDetail.contract" @loading="loading = true"></delegate-tokens-modal>
          <send-delegated-tokens-modal v-if="hasDelegatedTokens" :contract="contractDetail.contract" @has-delegated-tokens="hasDelegatedTokens = true" @loading="loading = true"></send-delegated-tokens-modal>
        </div>
        <div v-if="!contractDetail.isContract && contractDetail.balance > 0" class="text-xs-center">
          <send-ether-modal v-if="contractDetail.balance > 0" :account="account" @loading="loading = true; refreshed = false" @end-loading="loading = false" @loaded="loaded"></send-ether-modal>
        </div>
        <v-divider></v-divider>

        <v-alert :value="error" type="error">
          {{ error }}
        </v-alert>
        <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>

        <token-transactions v-if="contractDetail.isContract" :account="account" :contract="contractDetail.contract" @has-delegated-tokens="hasDelegatedTokens = true" @loaded="loaded" @error="loading = false;error = e"></token-transactions>
        <general-transactions v-else ref="generalTransactions" :account="account" @error="loading = false;error = e"></general-transactions>
      </v-card>
    </v-flex>
  </v-layout>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import GeneralTransactions from './GeneralTransactions.vue';
import TokenTransactions from './TokenTransactions.vue';
import SendTokensModal from './SendTokensModal.vue';
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import TruffleContract from 'truffle-contract';
import LocalWeb3 from 'web3';
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {loadContractBalance} from '../WalletToken.js';

export default {
  components: {
    AddContractModal,
    DelegateTokensModal,
    SendDelegatedTokensModal,
    SendTokensModal,
    GeneralTransactions,
    SendEtherModal,
    TokenTransactions
  },
  props: {
    account: {
      type: String,
      default: function() {
        return {};
      }
    },
    contractDetail: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      loading: false,
      // Avoid displaying loading ison when the refresh is already made 
      // and the event 'confirmed' continues to be triggered
      refreshed: false,
      // Avoid refreshing list and balance twice
      refreshing: false,
      error: null,
      hasDelegatedTokens: false
    };
  },
  watch: {
    account() {
      this.$emit('back');
    },
    contractDetail() {
      this.error = null;
      if (this.contractDetail && this.contractDetail.isContract && this.contractDetail.contract) {
        this.contractDetail.contract.Transfer({}, {
          fromBlock: 0,
          toBlock: 'latest',
        }).watch(function(error, event) {
          this.$forceUpdate();
        });
      }
    }
  },
  methods: {
    loaded() {
      if (this.contractDetail.isContract) {
        loadContractBalance(this.account, this.contractDetail)
          .then(() => this.loading = false)
          .catch(e => {
            this.loading = false;
            this.error = `Error: ${e}`;
          });
      } else {
        if (!this.refreshing) {
          this.refreshing = true;
          window.localWeb3.eth.getCoinbase()
            .then(window.localWeb3.eth.getBalance)
            .then(balance => {
              balance = window.localWeb3.utils.fromWei(balance, "ether");
              if(this.contractDetail.balance === balance) {
                return false;
              } else {
                this.refreshed = true;
                return this.contractDetail.balance = balance;
              }
            })
            .then(refresh => {
              if (refresh) {
                return this.$refs.generalTransactions.init();
              }
              return true;
            })
            .then(continueLoading => {
              // sometimes the balane gets refreshed,
              // but the event 'confirmed' continues to be triggered,
              // thus, loading is already finished and should stop
              this.loading = continueLoading && !this.refreshed;
            })
            .then(() => this.refreshing = false)
            .catch(e => {
              this.loading = false;
              this.error = `Error: ${e}`;
            });
          }
      }
    }
  }
};
</script>