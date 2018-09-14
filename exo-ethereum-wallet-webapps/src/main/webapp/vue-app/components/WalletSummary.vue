<template>
  <v-card class="waletSummary">
    <v-card-title v-if="pendingTransaction" primary-title class="pb-0">
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
        @pending="pendingTransaction = true"
        @success="pendingTransaction = false; $emit('refresh-balance');"
        @error="pendingTransaction = false; $emit('error', $event);" />
      <wallet-receive-modal :wallet-address="walletAddress" />
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import WalletReceiveModal from './WalletReceiveModal.vue';
import SendFundsModal from './SendFundsModal.vue';

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
    walletAddress: {
      type: String,
      default: function() {
        return null;
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
      pendingTransaction: false
    };
  },
  computed: {
    disableSendButton() {
      return this.isReadOnly || !this.etherBalance || !Number(this.etherBalance);
    }
  }
};
</script>