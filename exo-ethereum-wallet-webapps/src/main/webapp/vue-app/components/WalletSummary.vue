<template>
  <v-card class="waletSummary">
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
      <button :disabled="disableSendButton" class="btn btn-primary mr-1">Send</button>
      <wallet-receive-modal :wallet-address="walletAddress" />
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import WalletReceiveModal from './WalletReceiveModal.vue';

export default {
  components: {
    WalletReceiveModal
  },
  props: {
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
  computed: {
    disableSendButton() {
      return this.isReadOnly || !this.etherBalance;
    }
  }
};
</script>