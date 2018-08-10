<template>
  <v-dialog v-model="dialog" :disabled="disabled" width="300px" max-width="100vw" persistent>
    <v-btn slot="activator" :disabled="disabled" :dark="!disabled" color="primary" ripple>Delegate Tokens</v-btn>
    <qr-code-modal :to="recipient" :is-contract="true" :function-payable="false"
                   :args-names="['_spender', '_value']"
                   :args-types="['address', 'uint256']"
                   :args-values="[recipient, amount]"
                   :open="showQRCodeModal"
                   title="Delegate Token QR Code"
                   function-name="transfer"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Delegate Tokens</v-toolbar-title>
        <v-spacer />
        <v-btn icon dark @click.native="dialog = false">
          <v-icon>close</v-icon>
        </v-btn>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-alert v-show="!error && warning && warning.length" :value="warning" type="warning" class="v-content">
          {{ warning }}
        </v-alert>
        <v-form>
          <auto-complete ref="autocomplete" input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn :disabled="loading" :loading="loading" color="primary" @click="sendTokens">Save</v-btn>
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
    },
    balance: {
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
    }
  },
  data () {
    return {
      showQRCodeModal: false,
      recipient: null,
      loading: false,
      amount: null,
      dialog: null,
      warning: null,
      error: null
    };
  },
  computed: {
    disabled() {
      return !this.balance
        || this.balance === 0
        || (typeof this.balance === "string" 
            && (!this.balance.length || this.balance.trim() === "0"))
        || !this.etherBalance
        || this.etherBalance === 0
        || (typeof this.etherBalance === "string" 
            && (!this.etherBalance.length || this.etherBalance.trim() === "0"));
    }
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.autocomplete.clear();
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
        this.error = "Invalid number";
        return;
      }

      this.loading = true;
      try {
        // Delegate an amount of tokens to the recipient 
        this.contract.approve.estimateGas(this.recipient, this.amount.toString(), {gas: window.walletSettings.userDefaultGas, gasPrice: window.walletSettings.gasPrice})
          .then(result => {
            if (result > window.walletSettings.userDefaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userDefaultGas} while the estimation of necessary gas is ${result}`;
            }
            return this.contract.approve(this.recipient, this.amount.toString());
          })
          .then(resp => {
            if (resp.tx) {
              this.recipient = null;
              this.amount = null;
              this.dialog = false;
              this.$emit("loading");
            } else {
              this.error = `Error while proceeding transaction`;
              console.error('Error while proceeding transaction', resp);
              this.$emit("end-loading");
            }
            this.loading = false;
          })
          .catch (e => {
            this.error = `Error while proceeding: ${e}`;
            this.loading = false;
            this.$emit("end-loading");
          });
      } catch(e) {
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit("end-loading");
      }
    }
  }
};
</script>

