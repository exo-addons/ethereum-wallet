<template>
  <v-flex v-if="contractDetails && contractDetails.title" id="accountDetail" class="text-xs-center white">
    <v-card-title class="align-start">
      <v-layout column>
        <v-flex id="accountDetailTitle">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">{{ contractDetails.icon }}</v-icon>
            {{ contractDetails.title }}
          </div>
          <h3 v-if="contractDetails.balanceFiat" class="font-weight-light">{{ contractDetails.balanceFiat }} {{ fiatSymbol }}</h3>
          <h4 v-if="contractDetails.balanceFiat" class="grey--text font-weight-light">{{ contractDetails.balance }} {{ contractDetails.symbol }}</h4>
          <h3 v-else class="font-weight-light">{{ contractDetails.balance }} {{ contractDetails.symbol }}</h3>
        </v-flex>

        <v-flex id="accountDetailActions">
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
          <v-divider v-if="contractDetails.isContract && enableDelegation" vertical />
          <send-delegated-tokens-modal
            v-if="contractDetails.isContract && enableDelegation"
            :is-readonly="isReadOnly"
            :contract-details="contractDetails"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />
          <v-divider v-if="contractDetails.isContract && enableDelegation" vertical />
          <delegate-tokens-modal
            v-if="contractDetails.isContract && enableDelegation"
            :is-readonly="isReadOnly"
            :contract-details="contractDetails"
            use-navigation
            @sent="newTransactionPending"
            @error="error = $event" />
        </v-flex>
        <v-btn icon class="rightIcon" @click="stopLoading(); $emit('back')">
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
  watch: {
    contractDetails() {
      this.error = null;
      this.enableDelegation = window.walletSettings.userPreferences.enableDelegation;
    }
  },
  methods: {
    refreshBalance() {
      return window.localWeb3.eth.getBalance(this.walletAddress)
        .then(balance => {
          balance = window.localWeb3.utils.fromWei(balance, "ether");
          if (this.contractDetails.isContract) {
            this.contractDetails.etherBalance = balance;
            return retrieveContractDetails(this.walletAddress, this.contractDetails)
              .then(() => {
                this.$forceUpdate();
              });
          } else {
            this.contractDetails.balance = balance;
            this.contractDetails.balanceFiat = etherToFiat(balance);
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
    },
    stopLoading() {
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.stopLoadingTransactions();
      }
    }
  }
};
</script>