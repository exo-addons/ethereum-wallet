<template>
  <v-card class="waletSummary elevation-0">
    <v-card-title v-if="pendingTransactionsCount" primary-title class="pb-0">
      <v-spacer />
      <v-badge right>
        <span slot="badge">{{ pendingTransactionsCount }}</span>
        <v-progress-circular title="A transaction is in progress" color="primary" indeterminate size="20"></v-progress-circular>
      </v-badge>
      
      <v-spacer />
    </v-card-title>
    <v-card-title :class="!isMaximized && 'pt-2'" primary-title class="pb-0" >
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
      <v-layout row wrap>
        <v-flex>
          <v-bottom-nav :value="true" color="white" class="elevation-0 buttomNavigation">
            <send-funds-modal
              ref="sendFundsModal"
              :accounts-details="accountsDetails"
              :refresh-index="refreshIndex"
              :network-id="networkId"
              :wallet-address="walletAddress"
              :disabled="disableSendButton"
              :icon="!isMaximized"
              @pending="loadPendingTransactions()"
              @error="loadPendingTransactions(); $emit('error', $event);" />
            <v-divider v-if="!isMaximized" vertical />
            <wallet-receive-modal
              :wallet-address="walletAddress"
              :icon="!isMaximized"
              @pending="loadPendingTransactions()"
              @error="$emit('error', $event);" />
            <v-divider v-if="!isMaximized" vertical />
            <wallet-request-funds-modal
              v-if="!isSpace || isSpaceAdministrator"
              :accounts-details="accountsDetails"
              :refresh-index="refreshIndex"
              :wallet-address="walletAddress"
              :icon="!isMaximized" />
          </v-bottom-nav>
        </v-flex>
      </v-layout>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import WalletReceiveModal from './WalletReceiveModal.vue';
import WalletRequestFundsModal from './WalletRequestFundsModal.vue';
import SendFundsModal from './SendFundsModal.vue';

import {loadPendingTransactions} from '../WalletEther.js';
import {getPendingTransactionFromStorage, removePendingTransactionFromStorage} from '../WalletToken.js';

export default {
  components: {
    SendFundsModal,
    WalletRequestFundsModal,
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
    isMaximized: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
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
    pendingTransactionsCount() {
      return this.updatePendingTransactionsIndex && Object.keys(this.pendingTransactions).length;
    }
  },
  created() {
    this.loadPendingTransactions();
  },
  methods: {
    checkSendingRequest(isReadOnly) {
      if (document.location.search && document.location.search.length) {
        const search = document.location.search.substring(1);
        const parameters = JSON.parse(`{"${decodeURI(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g,'":"')}"}`);
        if (parameters && parameters.receiver && parameters.receiver_type && parameters.amount) {
          if (isReadOnly) {
            throw new Error('Your wallet is in readonly state');
          }
          this.$refs.sendFundsModal.prepareSendForm(parameters.receiver, parameters.receiver_type, parameters.amount, parameters.contract);
        }
      }
    },
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