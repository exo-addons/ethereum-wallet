<template>
  <v-dialog v-model="dialog" :disabled="disabled" width="300px" max-width="100vw" @keydown.esc="dialog = false">
    <v-btn slot="activator" :disabled="disabled" :dark="!disabled" color="primary" ripple>Send Tokens</v-btn>
    <qr-code-modal :to="recipient"
                   :from="account"
                   :amount="0"
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
        <v-btn :disabled="loading" :loading="loading" color="primary" @click="sendTokens">Send</v-btn>
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
    account: {
      type: Object,
      default: function() {
        return {};
      }
    },
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
      loading: false,
      showQRCodeModal: false,
      recipient: null,
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
        this.error = "Invalid amount";
        return;
      }

      this.loading = true;
      try {
        this.contract.methods.transfer(this.recipient, this.amount.toString()).estimateGas({gas: window.walletSettings.userDefaultGas, gasPrice: window.walletSettings.gasPrice})
          .then(result => {
            if (result > window.walletSettings.userDefaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userDefaultGas} while the estimation of necessary gas is ${result}`;
            }
            this.$emit("loading");
            return this.contract.methods.transfer(this.recipient, this.amount.toString()).send({from: this.account})
              .on('confirmation', (confirmationNumber, receipt) => {
                // console.debug("send transaction transfer - confirmation", confirmationNumber, receipt);
                if (this.loading) {
                  this.dialog = false;
                  this.loading = false;
                }
                // The transaction has been mined and confirmed
                this.$emit("loaded");
              })
              .on('error', (error, receipt) => {
                // The transaction has failed
                this.error = `Error sending ether: ${error}`;
                this.loading = false;
                this.$emit("error", this.error);
              });
          })
          .catch (e => {
            console.debug("Web3 contract.transfer method - error", e);
            this.error = `Error while proceeding: ${e}`;
            this.loading = false;
            this.$emit("end-loading");
          });
      } catch(e) {
        console.debug("Web3 contract.transfer method - error", e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit("end-loading");
      }
    }
  }
};
</script>

