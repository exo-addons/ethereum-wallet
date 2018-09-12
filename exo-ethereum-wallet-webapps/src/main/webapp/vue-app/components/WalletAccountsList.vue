<template>
  <!-- list of contracts and ether account -->
  <v-list class="pb-0" two-line subheader>
    <v-list-tile
      v-for="(item, index) in accountsList"
      :key="index"
      :color="item.error ? 'red': ''"
      :title="item.title"
      class="accountItem"
      avatar
      ripple>
  
      <v-list-tile-avatar @click="$emit('account-details-selected', item)">
        <v-icon :class="item.error ? 'red--text':'uiIconBlue'" dark>{{ item.icon }}</v-icon>
      </v-list-tile-avatar>
      <v-list-tile-content @click="$emit('account-details-selected', item)">
        <v-list-tile-title v-if="item.error"><strike>{{ item.title }}</strike></v-list-tile-title>
        <v-list-tile-title v-else>{{ item.title }}</v-list-tile-title>
  
        <v-list-tile-sub-title v-if="item.error">{{ item.error }}</v-list-tile-sub-title>
        <v-list-tile-sub-title v-else>{{ item.balanceFiat ? `${item.balanceFiat} ${fiatSymbol}`: `${item.balance} ${item.symbol}` }}</v-list-tile-sub-title>
      </v-list-tile-content>

      <v-speed-dial 
        v-if="!isReadOnly && ((item.isContract && item.balance > 0 && item.etherBalance > 0) || (!item.isContract && item.balance && item.balance !== '0'))"
        v-model="item.openActions"
        direction="left"
        transition="slide-y-reverse-transition"
        right
        absolute>

        <v-btn slot="activator" icon fab color="blue-grey lighten-4">
          <v-icon size="20">more_horiz</v-icon>
        </v-btn>
  
        <!-- Contract actions -->
        <send-tokens-modal
          v-if="item.isContract && item.balance > 0 && item.etherBalance > 0"
          :balance="item.balance"
          :ether-balance="item.etherBalance"
          :account="account"
          :contract="item.contract"
          icon
          @sent="addSendTokenTransaction($event, item)"
          @error="$emit('error', $event)" />
  
        <delegate-tokens-modal
          v-if="item.isContract && item.balance > 0 && item.etherBalance > 0"
          :balance="item.balance"
          :ether-balance="item.etherBalance"
          :contract="item.contract"
          icon
          @sent="addDelegateTokenTransaction($event, item)"
          @error="$emit('error', $event)" />
  
        <send-delegated-tokens-modal
          v-if="item.isContract && item.balance > 0 && item.etherBalance > 0"
          :ether-balance="item.etherBalance"
          :contract="item.contract"
          :has-delegated-tokens="true"
          icon
          @error="$emit('error', $event)" />
  
        <!-- Ether account actions -->
        <send-ether-modal
          v-if="!item.isContract && item.balance && item.balance !== '0'"
          :account="account"
          :balance="item.balance"
          icon
          @sent="addSendEtherTransaction"
          @error="$emit('error', $event)" />
  
        <v-btn v-if="!isSpace && item.isContract && !item.isDefault" icon ripple @click="deleteContract(item, $event)">
          <i class="uiIconTrash uiIconBlue"></i>
        </v-btn>
      </v-speed-dial>
    </v-list-tile>
    <v-divider v-if="index + 1 < accountsDetails.length" :key="`divider-${index}`"></v-divider>
  </v-list>
</template>

<script>
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendTokensModal from './SendTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import {addPendingTransactionToStorage, removePendingTransactionFromStorage} from '../WalletToken.js';
import {watchTransactionStatus} from '../WalletUtils.js';
import {addTransaction} from '../WalletEther.js';

export default {
  components: {
    DelegateTokensModal,
    SendDelegatedTokensModal,
    SendTokensModal,
    SendEtherModal
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  computed: {
    accountsList() {
      // A trick to force Refresh list
      if (!this.refreshIndex) {
        return;
      }
      const accountsList = [];
      Object.keys(this.accountsDetails).forEach(key => accountsList.push(this.accountsDetails[key]));
      return accountsList;
    }
  },
  methods: {
    addDelegateTokenTransaction(transaction, contract) {
      addPendingTransactionToStorage(this.networkId, this.account, contract.address, {
        from: transaction.from,
        to: transaction.to,
        value: transaction.value,
        hash: transaction.hash,
        timestamp: Date.now(),
        labelFrom: 'Delegated from',
        labelTo: 'Delegated to',
        icon: 'fa-users',
        pending: true
      });

      watchTransactionStatus(transaction.hash, (receipt, block) => {
        removePendingTransactionFromStorage(this.networkId, this.account, contract.address, transaction.hash);
      });
    },
    addSendTokenTransaction(transaction, contract) {
      addPendingTransactionToStorage(this.networkId, this.account, contract.address, {
        from: transaction.from,
        to: transaction.to,
        value: transaction.value,
        hash: transaction.hash,
        timestamp: Date.now(),
        labelFrom: 'Received from',
        labelTo: 'Sent to',
        icon: 'fa-exchange-alt',
        pending: true
      });

      watchTransactionStatus(transaction.hash, (receipt, block) => {
        removePendingTransactionFromStorage(this.networkId, this.account, contract.address, transaction.hash);
      });
    },
    addSendEtherTransaction(transaction) {
      addTransaction(this.networkId,
        this.account,
        [],
        transaction);
    }
  }
};
</script>