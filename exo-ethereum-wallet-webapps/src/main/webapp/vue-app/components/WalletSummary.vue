<template>
  <v-card class="waletSummary">
    <v-card-title v-if="hasPendingTransaction" primary-title class="pb-0">
      <v-spacer />
      <v-progress-circular title="A transaction is in progress" color="primary" indeterminate size="20"></v-progress-circular>
      <v-spacer />
    </v-card-title>
    <v-card-title primary-title class="pb-0">
      <v-spacer />
      <h3 class="headline">{{ totalFiatBalance }} {{ fiatSymbol }}</h3>
      <v-spacer />
    </v-card-title>
    <v-card-title primary-title class="pt-0">
      <v-spacer />
      <div>{{ totalBalance }} ether</div>
      <v-spacer />
    </v-card-title>

    <v-card-actions>
      <v-spacer />
      <send-funds-modal
        :accounts-details="accountsDetails"
        :refresh-index="refreshIndex"
        :network-id="networkId"
        :wallet-address="walletAddress"
        :disabled="disableSendButton"
        @pending="loadPendingTransactions()"
        @error="loadPendingTransactions(); $emit('error', $event);" />
      <wallet-receive-modal
        :wallet-address="walletAddress"
        @pending="loadPendingTransactions()"
        @error="hasPendingTransaction = false; $emit('error', $event);" />
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import WalletReceiveModal from './WalletReceiveModal.vue';
import SendFundsModal from './SendFundsModal.vue';

import {loadPendingTransactions} from '../WalletEther.js';
import {getPendingTransactionFromStorage, removePendingTransactionFromStorage} from '../WalletToken.js';

export default {
  components: {
    SendFundsModal,
    WalletReceiveModal
  },
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
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
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    etherBalance: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    totalBalance: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    totalFiatBalance: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return '$';
      }
    },
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      updatePendingTransactionsIndex: 1,
      pendingTransactions: {}
    };
  },
  computed: {
    disableSendButton() {
      return this.isReadOnly || !this.etherBalance || !Number(this.etherBalance);
    },
    hasPendingTransaction() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    }
  },
  created() {
    this.loadPendingTransactions();
  },
  methods: {
    loadPendingTransactions() {
      Object.keys(this.pendingTransactions).forEach(key => delete this.pendingTransactions[key]);

      return loadPendingTransactions(this.networkId, this.walletAddress, this.pendingTransactions, transaction => {
        if (this.pendingTransactions[transaction.hash]) {
          delete this.pendingTransactions[transaction.hash];
        }
        this.updatePendingTransactionsIndex++;
        this.$emit("refresh-balance");
      },
      (error, transaction) => {
        if (this.pendingTransactions[transaction.hash]) {
          delete this.pendingTransactions[transaction.hash];
        }
        this.updatePendingTransactionsIndex++;
        this.$emit('error', error);
      })
        .then(() => {
          Object.keys(this.accountsDetails).forEach(key => {
            const accountDetail = this.accountsDetails[key];
            if (accountDetail && accountDetail.isContract) {
              const contractTransactions = getPendingTransactionFromStorage(this.networkId, this.walletAddress, accountDetail.address,
                (transactionHash, receipt) => {
                  if (this.pendingTransactions[transactionHash]) {
                    delete this.pendingTransactions[transactionHash];
                  }
                  this.updatePendingTransactionsIndex++;
                  if (receipt) {
                    this.$emit("refresh-token-balance", accountDetail);
                  } else {
                    this.$emit('error', `An error proceeding transaction on contract '${accountDetail.name}'`);
                  }
                });

              if (contractTransactions && contractTransactions) {
                Object.keys(contractTransactions).forEach(transactionHash => {
                  this.pendingTransactions[transactionHash] = contractTransactions[transactionHash];
                });
              }
            }
          });
        })
        .then(() => this.updatePendingTransactionsIndex++);
    }
  }
};
</script>