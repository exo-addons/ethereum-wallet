<template>
  <v-dialog v-model="dialog" width="300px" max-width="100vw">
    <v-btn slot="activator" color="primary" dark ripple>Send Tokens</v-btn>
    <qr-code-modal :to="recipient"
                   :is-contract="true"
                   :args-names="['_to', '_value']"
                   :args-types="['address', 'uint256']"
                   :args-values="[recipient, amount]"
                   :open="showQRCodeModal"
                   :function-payable="false"
                   function-name="transfer"
                   title="Send Tokens QR Code"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Send Tokens</v-toolbar-title>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-alert v-show="!error && warning && warning.length" :value="warning" type="warning" class="v-content">
          {{ warning }}
        </v-alert>
        <v-form>
          <auto-complete input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="primary" @click="sendTokens">Send</v-btn>
        <v-btn :disabled="!recipient || !amount" color="secondary" @click="showQRCodeModal = true">QRCode</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AutoComplete from './AutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';

export default {
  components: {
    QrCodeModal,
    AutoComplete
  },
  props: {
    contract: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      showQRCodeModal: false,
      recipient: null,
      amount: null,
      dialog: null,
      warning: null,
      error: null
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.showQRCodeModal = false;
        this.recipient = null;
        this.amount = null;
        this.warning = null;
        this.error = null;
      }
    }
  },
  methods: {
    sendTokens() {
      this.error = null;
      this.warning = null;
      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = "Invalid recipient address";
        return;
      }

      if (!this.amount || isNaN(parseInt(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = "Invalid amount";
        return;
      }

      this.contract.transfer.estimateGas(this.recipient, this.amount.toString(), {gas: 300000})
        .then(result => {
          if (result <= window.walletSettings.userDefaultGas) {
            return this.contract.transfer(this.recipient, this.amount.toString());
          } else {
            this.warning = `You have set a low gas ${window.walletSettings.userDefaultGas} while the estimation of necessary gas is ${result}`;
            return this.contract.transfer(this.recipient, this.amount.toString());
          }
        })
        .then(resp => {
          if (resp.tx) {
            this.recipient = null;
            this.amount = null;
            this.dialog = false;
            this.$emit("loading");
          } else {
            this.error = 'Error while proceeding transaction';
            console.error('Error while proceeding transaction', resp);
            this.$emit("end-loading");
          }
        })
        .catch (e => {
          this.error = `Error while proceeding: ${e}`;
          this.$emit("end-loading");
        });
    }
  }
};
</script>

