<template>
  <v-flex v-if="contractDetails && contractDetails.title" id="accountDetail" class="text-xs-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout column>
        <v-flex id="accountDetailTitle">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">{{ contractDetails.icon }}</v-icon>
            {{ contractDetails.title }}
          </div>
          <h3 v-if="!contractDetails.isContract" class="font-weight-light">{{ fiatBalance }}</h3>
          <h4 v-if="!contractDetails.isContract" class="grey--text font-weight-light">{{ balance }}</h4>
          <h3 v-else class="font-weight-light">{{ balance }}</h3>
        </v-flex>

        <v-flex v-if="!isDisplayOnly" id="accountDetailActions">
          <!-- Ether action -->
          <send-ether-modal
            v-if="!contractDetails.isContract"
            :is-readonly="isReadOnly"
            :account="walletAddress"
            :balance="contractDetails.balance"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />

          <!-- Contract actions -->
          <send-tokens-modal
            v-if="contractDetails.isContract"
            :is-readonly="isReadOnly"
            :account="walletAddress"
            :contract-details="contractDetails"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />
          <send-delegated-tokens-modal
            v-if="contractDetails.isContract && enableDelegation"
            :wallet-address="walletAddress"
            :is-readonly="isReadOnly"
            :contract-details="contractDetails"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />
          <delegate-tokens-modal
            v-if="contractDetails.isContract && enableDelegation"
            :is-readonly="isReadOnly"
            :contract-details="contractDetails"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />
        </v-flex>
        <v-btn icon class="rightIcon" @click="$emit('back')">
          <v-icon>close</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <transactions-list
      id="transactionsList"
      ref="transactionsList"
      :network-id="networkId"
      :account="walletAddress"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :selected-transaction-hash="selectedTransactionHash"
      :error="error"
      @error="error = $event"
      @refresh-balance="refreshBalance" />

  </v-flex>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import TransactionsList from './TransactionsList.vue';
import SendTokensModal from './SendTokensModal.vue';
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import {retrieveContractDetails} from '../WalletToken.js';
import {etherToFiat} from '../WalletUtils.js';

export default {
  components: {
    AddContractModal,
    SendEtherModal,
    SendTokensModal,
    DelegateTokensModal,
    SendDelegatedTokensModal,
    TransactionsList
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    isDisplayOnly: {
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
    selectedTransactionHash: {
      type: String,
      default: function() {
        return null;
      }
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      // Avoid refreshing list and balance twice
      refreshing: false,
      enableDelegation: true,
      error: null
    };
  },
  computed: {
    fiatBalance() {
      return this.contractDetails && this.contractDetails.balanceFiat ? `${this.contractDetails.balanceFiat} ${this.fiatSymbol}` : `0 ${this.fiatSymbol}`;
    },
    balance() {
      return this.contractDetails && this.contractDetails.balance ? `${this.contractDetails.balance} ${this.contractDetails && this.contractDetails.symbol}` : '';
    }
  },
  watch: {
    contractDetails() {
      this.error = null;
      this.enableDelegation = window.walletSettings.userPreferences.enableDelegation;
    }
  },
  methods: {
    refreshBalance() {
      if (!this.contractDetails) {
        return Promise.resolve(null);
      }
      return window.localWeb3.eth.getBalance(this.walletAddress)
        .then(balance => {
          balance = window.localWeb3.utils.fromWei(String(balance), "ether");
          if (this.contractDetails.isContract) {
            this.contractDetails.etherBalance = balance;
            return retrieveContractDetails(this.walletAddress, this.contractDetails)
              .then(() => this.$forceUpdate());
          } else {
            this.$set(this.contractDetails, "balance", balance);
            this.$set(this.contractDetails, "balanceFiat", etherToFiat(balance));
            if (this.contractDetails.details) {
              this.$set(this.contractDetails.details, "balance", this.contractDetails.balance);
              this.$set(this.contractDetails.details, "balanceFiat", this.contractDetails.balanceFiat);
            }
            this.$forceUpdate();
          }
        });
    },
    isEtherBalanceSame(balance) {
      return this.contractDetails.isContract ?
        this.contractDetails.etherBalance === balance :
        this.contractDetails.balance === balance;
    },
    newTransactionPending(transaction, contractDetails) {
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.addTransaction(transaction, contractDetails);
      }
      this.$emit("transaction-sent");
    }
  }
};
</script>