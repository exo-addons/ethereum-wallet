<template>
  <v-dialog v-model="dialog" width="300px" max-width="100vw">
    <v-btn slot="activator" color="primary" dark ripple>Send delegated Tokens</v-btn>
    <qr-code-modal :to="recipient" :is-contract="true" :function-payable="false"
                   :args-names="['_from', '_to', '_value']"
                   :args-types="['address', 'address', 'uint256']"
                   :args-values="[from, recipient, amount]"
                   :open="showQRCodeModal"
                   title="Send delegated Token QR Code"
                   function-name="transferFrom"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Send delegated Tokens</v-toolbar-title>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-form>
          <auto-complete input-label="From" @item-selected="from = $event.address"></auto-complete>
          <auto-complete input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="primary" @click="sendTokens">Send</v-btn>
        <v-btn :disabled="!recipient || !amount || !from" color="secondary" @click="showQRCodeModal = true">QRCode</v-btn>
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
      from: null,
      recipient: null,
      amount: null,
      dialog: null,
      error: null
    };
  },
  methods: {
    sendTokens() {
      this.error = null;
      if (!window.localWeb3.utils.isAddress(this.from)) {
        this.error = "Invalid from address";
        return;
      }

      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = "Invalid recipient address";
        return;
      }

      if (!this.amount || isNaN(parseInt(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = "Invalid amount";
        return;
      }

      // Send delegated amount of tokens to the recipient on behalf of a third person
      // (if he already delegated a certain amount to recipient)
      this.contract.transferFrom(this.from, this.recipient, this.amount.toString())
        .then(resp => {
          if (resp.tx) {
            this.from = null;
            this.recipient = null;
            this.amount = null;
            this.dialog = false;
          } else {
            this.error = `Error while proceeding transaction`;
            console.error('Error while proceeding transaction', resp);
          }
        })
        .catch (e => {
          this.error = `Error while proceeding: ${e}`;
        });
    }
  }
};
</script>

