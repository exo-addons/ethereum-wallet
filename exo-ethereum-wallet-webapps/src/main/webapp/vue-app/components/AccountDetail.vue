<template>
  <v-flex v-if="contractDetail && contractDetail.title" id="accountDetail" class="text-xs-center white">
    <v-card-media min-height="80px">
      <v-layout column fill-height>
        <v-card-title class="pb-0">
          <v-spacer />
          <div class="title">
            <h3>
              <v-icon class="primary--text">{{ contractDetail.icon }}</v-icon>
              {{ contractDetail.title }}
            </h3>
          </div>
          <v-spacer />
          <v-btn icon class="rightIcon" @click="stopLoading(); $emit('back')">
            <v-icon>close</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-title class="pt-0">
          <v-spacer />
          <v-list class="transparent">
            <v-list-tile>
              <v-list-tile-content v-if="contractDetail.balanceFiat" class="text-xs-center">
                <v-list-tile-title class="text-xs-center">{{ contractDetail.balanceFiat }} {{ fiatSymbol }}</v-list-tile-title>
                <v-list-tile-sub-title>{{ contractDetail.balance }} {{ contractDetail.symbol }}</v-list-tile-sub-title>
              </v-list-tile-content>
              <v-list-tile-content v-else class="text-xs-center">
                <v-list-tile-title>{{ contractDetail.balance }} {{ contractDetail.symbol }}</v-list-tile-title>
              </v-list-tile-content>
            </v-list-tile>
          </v-list>
          <v-spacer />
        </v-card-title>
      </v-layout>
    </v-card-media>

    <v-divider v-if="isReadOnly" />
    <div v-else class="text-xs-center">
      <!-- Contract actions -->
      <send-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :account="walletAddress"
        :contract="contractDetail.contract"
        @sent="newTransactionPending($event)"
        @error="loading = false; error = $event" />
      <delegate-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :balance="contractDetail.balance"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        @sent="newTransactionPending($event)"
        @error="loading = false; error = $event" />
      <send-delegated-tokens-modal
        v-if="contractDetail.isContract"
        :disabled="!hasDelegatedTokens || contractDetail.balance === 0 || contractDetail.etherBalance === 0"
        :ether-balance="contractDetail.etherBalance"
        :contract="contractDetail.contract"
        :has-delegated-tokens="hasDelegatedTokens"
        @sent="newTransactionPending($event)"
        @error="loading = false; error = $event" />

      <!-- Ether account actions -->
      <send-ether-modal
        v-if="!contractDetail.isContract"
        :account="walletAddress"
        :balance="contractDetail.balance"
        @sent="newTransactionPending($event)"
        @error="loading = false; error = $event" />
    </div>

    <div v-if="error && !loading" class="alert alert-error">
      <i class="uiIconError"></i>{{ error }}
    </div>

    <v-progress-circular v-show="loading" indeterminate color="primary" />

    <token-transactions
      v-if="contractDetail.isContract"
      id="contractTransactionsContent"
      ref="contractTransactions"
      :network-id="networkId"
      :account="walletAddress"
      :contract="contractDetail.contract"
      @has-delegated-tokens="hasDelegatedTokens = true"
      @loading="loading = true"
      @end-loading="loading = false"
      @error="loading = false; error = $event"
      @refresh-balance="refreshBalance" />
    <general-transactions
      v-else id="generalTransactionsContent"
      ref="generalTransactions"
      :network-id="networkId"
      :account="walletAddress"
      @loading="loading = true"
      @end-loading="loading = false"
      @error="loading = false; error = $event"
      @refresh-balance="refreshBalance" />
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
import {etherToFiat} from '../WalletUtils.js';

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
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
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
    contractDetail() {
      this.error = null;
    },
    loading() {
      if(this.loading) {
        this.error = null;
      }
    }
  },
  methods: {
    refreshBalance() {
      return window.localWeb3.eth.getBalance(this.walletAddress)
        .then(balance => {
          balance = window.localWeb3.utils.fromWei(balance, "ether");
          if (this.contractDetail.isContract) {
            this.contractDetail.etherBalance = balance;
            return retrieveContractDetails(this.walletAddress, this.contractDetail)
              .then(() => {
                this.$forceUpdate();
              });
          } else {
            this.contractDetail.balance = balance;
            this.contractDetail.balanceFiat = etherToFiat(balance);
            this.$forceUpdate();
          }
        });
    },
    isEtherBalanceSame(balance) {
      return this.contractDetail.isContract ?
        this.contractDetail.etherBalance === balance :
        this.contractDetail.balance === balance;
    },
    newTransactionPending(transaction) {
      if (this.contractDetail.isContract) {
        if (transaction.type === 'sendToken') {
          this.$refs.contractTransactions.addSendTransaction(transaction);
        } else if (transaction.type === 'delegateToken') {
          this.$refs.contractTransactions.addDelegateTransaction(transaction);
        } else {
          throw new Error("Transaction type is not recognized", transaction);
        }
      } else {
        this.$refs.generalTransactions.addTransaction(transaction);
      }
    },
    stopLoading() {
      if (this.$refs.generalTransactions) {
        this.$refs.generalTransactions.stopLoadingTransactions();
      }
    }
  }
};
</script>