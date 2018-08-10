<template>
  <v-dialog v-model="dialog" width="300px" max-width="100vw">
    <v-btn slot="activator" color="primary" dark ripple>Send Ether</v-btn>
    <qr-code-modal :from="account"
                   :to="recipient"
                   :amount="amount"
                   :open="showQRCodeModal"
                   title="Send Ether QR Code"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Send Ether</v-toolbar-title>
        <v-spacer />
        <v-btn icon dark @click.native="dialog = false">
          <v-icon>close</v-icon>
        </v-btn>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-form>
          <auto-complete input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="loading" :loading="loading" color="primary" @click="sendEther">Send</v-btn>
        <v-btn :disabled="!recipient || !account || !amount" color="secondary" @click="showQRCodeModal = true">QRCode</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AutoComplete from './AutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import {gasToEther} from '../WalletUtils.js';

export default {
  components: {
    QrCodeModal,
    AutoComplete
  },
  props: {
    account: {
      type: String,
      default: function() {
        return {};
      }
    },
    balance: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data () {
    return {
      showQRCodeModal: false,
      loading: false,
      recipient: null,
      amount: null,
      dialog: null,
      error: null
    };
  },
  methods: {
    sendEther() {
      this.error = null;
      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = "Invalid recipient address";
        return;
      }

      if (!this.amount || isNaN(parseInt(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = "Invalid amount";
        return;
      }

      const gas = window.walletSettings.userDefaultGas ? window.walletSettings.userDefaultGas : 35000;
      if (this.amount + gasToEther(gas) >= this.balance) {
        this.error = "Unsufficient ethers";
        return;
      }

      this.loading = true;
      try {
        // Send an amount of ether to a third person
        window.localWeb3.eth.sendTransaction({
          from: this.account.toLowerCase(),
          to: this.recipient,
          value: window.localWeb3.utils.toWei(this.amount.toString(), "ether"),
          gas: gas,
          gasPrice: window.walletSettings.gasPrice
        })
          .on('transactionHash', hash => {
            // The transaction has been hashed and will be sent
            this.$emit("loading");
            this.recipient = null;
            this.amount = null;
            this.dialog = false;
          })
          .on('confirmation', (confirmationNumber, receipt) => {
            this.loading = false;
            // The transaction has been mined and confirmed
            this.$emit("loaded");
          })
          .on('error', (error, receipt) => {
            // The transaction has failed
            this.error = `Error sending ether: ${error}`;
            this.loading = false;
            this.$emit("end-loading");
          });
      } catch(e) {
        this.loading = false;
        this.error = `Error sending ether: ${e}`;
        this.$emit("end-loading");
      }
    }
  }
};
</script>

