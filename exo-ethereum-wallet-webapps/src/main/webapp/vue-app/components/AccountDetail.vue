<template>
  <v-flex v-if="contractDetail && contractDetail.title" id="accountDetail" class="text-xs-center white">
    <v-card-title class="align-start">
      <v-layout column>
        <v-flex id="accountDetailTitle">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">{{ contractDetail.icon }}</v-icon>
            {{ contractDetail.title }}
          </div>
          <h3 v-if="contractDetail.balanceFiat" class="font-weight-light">{{ contractDetail.balanceFiat }} {{ fiatSymbol }}</h3>
          <h4 v-if="contractDetail.balanceFiat" class="grey--text font-weight-light">{{ contractDetail.balance }} {{ contractDetail.symbol }}</h4>
          <h3 v-else class="font-weight-light">{{ contractDetail.balance }} {{ contractDetail.symbol }}</h3>
        </v-flex>

        <v-layout id="accountDetailActions">
          <send-ether-modal
            v-if="!contractDetail.isContract"
            :is-readonly="isReadOnly"
            :account="walletAddress"
            :balance="contractDetail.balance"
            use-navigation
            @sent="newTransactionPending($event)"
            @error="error = $event" />

          <!-- Contract actions -->
          <send-tokens-modal
            v-if="contractDetail.isContract"
            :is-readonly="isReadOnly"
            :balance="contractDetail.balance"
            :ether-balance="contractDetail.etherBalance"
            :account="walletAddress"
            :contract="contractDetail.contract"
            use-navigation
            @sent="newTransactionPending($event)"
            @error="error = $event" />
          <v-divider v-if="contractDetail.isContract" vertical />
          <send-delegated-tokens-modal
            v-if="contractDetail.isContract"
            :is-readonly="isReadOnly"
            :ether-balance="contractDetail.etherBalance"
            :contract="contractDetail.contract"
            :has-delegated-tokens="hasDelegatedTokens"
            use-navigation
            @sent="newTransactionPending($event)"
            @error="error = $event" />
          <v-divider v-if="contractDetail.isContract" vertical />
          <delegate-tokens-modal
            v-if="contractDetail.isContract"
            :is-readonly="isReadOnly"
            :balance="contractDetail.balance"
            :ether-balance="contractDetail.etherBalance"
            :contract="contractDetail.contract"
            use-navigation
            @sent="newTransactionPending($event)"
            @error="error = $event" />
        </v-layout>
        <v-btn icon class="rightIcon" @click="stopLoading(); $emit('back')">
          <v-icon>close</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <token-transactions
      v-if="contractDetail.isContract"
      id="contractTransactionsContent"
      ref="contractTransactions"
      :network-id="networkId"
      :account="walletAddress"
      :contract-detail="contractDetail"
      :fiat-symbol="fiatSymbol"
      :error="error"
      @has-delegated-tokens="hasDelegatedTokens = true"
      @error="error = $event"
      @refresh-balance="refreshBalance" />
    <general-transactions
      v-else id="generalTransactionsContent"
      ref="generalTransactions"
      :contract-detail="contractDetail"
      :network-id="networkId"
      :account="walletAddress"
      :fiat-symbol="fiatSymbol"
      :error="error"
      @error="error = $event"
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
      // Avoid refreshing list and balance twice
      refreshing: false,
      error: null,
      hasDelegatedTokens: false
    };
  },
  watch: {
    contractDetail() {
      this.error = null;
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