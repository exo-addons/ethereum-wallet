<template>
  <v-card id="accountDetail" class="text-xs-center">
    <v-card-media min-height="80px">
      <v-layout column fill-height>
        <v-card-title class="pb-0">
          <v-btn absolute icon @click="$emit('back')">
            <v-icon>arrow_back</v-icon>
          </v-btn>
          <v-spacer />
          <div class="title">{{ contractDetail.title }}</div>
          <v-spacer />
        </v-card-title>
        <v-card-title class="pt-0">
          <v-spacer />
          <v-list class="transparent">
            <v-list-tile>
              <v-list-tile-content class="text-xs-center">
                <v-list-tile-sub-title>{{ contractDetail.balance }} {{ contractDetail.symbol }}</v-list-tile-sub-title>
                <v-list-tile-sub-title v-if="contractDetail.balanceUSD">{{ contractDetail.balanceUSD }} $</v-list-tile-sub-title>
              </v-list-tile-content>
            </v-list-tile>
          </v-list>
          <v-spacer />
        </v-card-title>
      </v-layout>
    </v-card-media>

    <v-divider v-if="isSpace" />
    <div v-else class="text-xs-center grey lighten-4">
      <!--  -->
      <send-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        @loading="loading = true"
        @end-loading="loading = false" />
      <delegate-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        @loading="loading = true" />
      <send-delegated-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="!hasDelegatedTokens || contractDetail.etherBalance === 0"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        @has-delegated-tokens="hasDelegatedTokens = true" />
      <send-ether-modal
        v-if="!contractDetail.isContract"
        :account="account"
        :balance="contractDetail.balance"
        @loading="loading = true; refreshed = false"
        @end-loading="loading = false"
        @loaded="loaded" />
    </div>

    <v-alert :value="error" type="error">
      {{ error }}
    </v-alert>
    <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>

    <token-transactions v-if="contractDetail.isContract" :account="account" :contract="contractDetail.contract" @has-delegated-tokens="hasDelegatedTokens = true" @loaded="loaded" @error="loading = false;error = e"></token-transactions>
    <general-transactions v-else ref="generalTransactions" :account="account" @loading="loading = true; refreshed = false" @end-loading="loading = false" @error="loading = false;error = e"></general-transactions>
  </v-card>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import GeneralTransactions from './GeneralTransactions.vue';
import TokenTransactions from './TokenTransactions.vue';
import SendTokensModal from './SendTokensModal.vue';
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {loadContractDetails} from '../WalletToken.js';
import {etherToUSD} from '../WalletUtils.js';

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
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
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
        }).watch(() => {
          this.$forceUpdate();
        });
      }
    }
  },
  methods: {
    loaded() {
      if (this.contractDetail.isContract) {
        // Refresh Contract balance and tranactions
        loadContractDetails(this.account, this.contractDetail)
          .then(() => this.loading = false)
          .catch(e => {
            this.loading = false;
            this.error = `${e}`;
          });
      } else if (!this.refreshing) {
        // Refresh Ether balance and tranactions
        // And avoid parallel refresh with refreshing = true
        this.refreshing = true;
        const account = window.localWeb3.eth.defaultAccount;
        window.localWeb3.eth.getBalance(account)
          .then(balance => {
            balance = window.localWeb3.utils.fromWei(balance, "ether");
            this.contractDetail.balanceUSD = etherToUSD(balance);
            if(this.refreshed) {
              return false;
            } else if(this.contractDetail.balance === balance) {
              return true;
            } else {
              this.contractDetail.balance = balance;
              return this.$refs.generalTransactions.init();
            }
          })
          .then(continueLoading => {
            // sometimes the balane gets refreshed, but the event 'confirmed' continues to be triggered,
            // thus, loading is already finished and should stop
            if (!continueLoading) {
              this.refreshed = true;
            }
            return this.loading = continueLoading && !this.refreshed;
          })
          // Permit to refresh again once the above conditions finished
          .then(() => this.refreshing = false)
          .catch(e => {
            this.refreshing = false;
            this.loading = false;
            this.error = `${e}`;
          });
      }
    }
  }
};
</script>