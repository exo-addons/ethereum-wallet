<template>
  <v-flex v-if="contractDetail && contractDetail.title" id="accountDetail" class="text-xs-center white">
    <v-card-media min-height="80px">
      <v-layout column fill-height>
        <v-card-title class="pb-0">
          <v-btn absolute icon @click="$emit('back')">
            <v-icon>arrow_back</v-icon>
          </v-btn>
          <v-spacer />
          <div class="title">
            <span>{{ contractDetail.title }}</span>
            <div v-if="contractDetail.isContract" class="title mt-2">
              Contract address: <code>{{ contractDetail.address }}</code>
            </div>
          </div>
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
      <!-- Contract actions -->
      <send-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :account="account"
        :contract="contractDetail.contract"
        @loaded="loaded"
        @loading="loading = true"
        @error="loading = false; error = $event"
        @end-loading="loading = false" />
      <delegate-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        @loaded="loaded"
        @loading="loading = true"
        @error="loading = false; error = $event"
        @end-loading="loading = false" />
      <send-delegated-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="!hasDelegatedTokens || contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        :has-delegated-tokens="hasDelegatedTokens"
        @loaded="loaded"
        @loading="loading = true"
        @error="loading = false; error = $event"
        @end-loading="loading = false" />

      <!-- Ether account actions -->
      <send-ether-modal
        v-if="!contractDetail.isContract"
        :account="account"
        :balance="contractDetail.balance"
        @loading="loading = true"
        @loaded="loaded"
        @error="loading = false; error = $event"
        @end-loading="loading = false" />
    </div>

    <v-alert :value="error" type="error" dismissible>
      {{ error }}
    </v-alert>

    <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>

    <token-transactions v-if="contractDetail.isContract" id="contractTransactionsContent" ref="contractTransactions" :account="account" :contract="contractDetail.contract" @has-delegated-tokens="hasDelegatedTokens = true" @loading="loading = true" @end-loading="loading = false" @error="loading = false; error = $event" />
    <general-transactions v-else id="generalTransactionsContent" ref="generalTransactions" :account="account" @loading="loading = true" @end-loading="loading = false" @error="loading = false; error = $event" />
  </v-flex>
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
import {retrieveContractDetails} from '../WalletToken.js';
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
    }
  },
  methods: {
    loaded() {
      if (this.loading && !this.refreshing) {
        // Refresh Ether balance and tranactions
        // And avoid parallel refresh with refreshing = true
        this.refreshing = true;
        const account = window.localWeb3.eth.defaultAccount;
        return window.localWeb3.eth.getBalance(account)
          .then(balance => {
            balance = window.localWeb3.utils.fromWei(balance, "ether");
            if(!this.loading) {
              return false;
            } else if(this.isEtherBalanceSame(balance)) {
              return true;
            } else {
              return this.refreshList(balance)
                .then(false); // Stop loading
            }
          })
          .then(continueLoading => {
            if (!continueLoading) {
              this.loading = false;
              this.$forceUpdate();
            }
            return this.loading;
          })
          .catch(e => {
            console.debug("Web3.eth.getBalance method - error", e);
            // Stop loading on error
            this.loading = false;
            this.error = `${e}`;
          })
          // Permit to refresh again once the above conditions finished
          .finally(() => this.refreshing = false);
      }
    },
    refreshList(balance) {
      console.debug("Account details - refreshList after balance modified");
      if (this.contractDetail.isContract) {
        this.contractDetail.etherBalance = balance;
        // Refresh Contract balance and tranactions
        return retrieveContractDetails(this.account, this.contractDetail)
          .then(() => this.$refs.contractTransactions.refreshNewwestTransactions());
      } else {
        this.contractDetail.balanceUSD = etherToUSD(balance);
        this.contractDetail.balance = balance;
        return this.$refs.generalTransactions.refreshNewwestTransactions();
      }
    },
    isEtherBalanceSame(balance) {
      return this.contractDetail.isContract ?
        this.contractDetail.etherBalance === balance :
        this.contractDetail.balance === balance;
    }
  }
};
</script>