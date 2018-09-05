<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="340px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <v-btn v-if="icon" slot="activator" :disabled="disabled" class="mt-1 mb-1" fab dark small title="Send delegated tokens" color="primary" icon>
      <v-avatar size="20">DT</v-avatar>
    </v-btn>
    <button v-else slot="activator" :disabled="disabled" :dark="!disabled" class="btn btn-primary mt-1 mb-1">Send delegated Tokens</button>
    <qr-code-modal :to="recipient" :is-contract="true" :function-payable="false"
                   :args-names="['_from', '_to', '_value']"
                   :args-types="['address', 'address', 'uint256']"
                   :args-values="[from, recipient, amount]"
                   :open="showQRCodeModal"
                   title="Send delegated Token QR Code"
                   information="You can scan this QR code by using a different application that supports QR code transaction generation to send delegated tokens"
                   function-name="transferFrom"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send delegated Tokens</span>
      </div>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
          <i class="uiIconWarning"></i>{{ warning }}
        </div>
        <v-form>
          <auto-complete ref="autocompleteFrom" :disabled="loading" input-label="From" @item-selected="from = $event.address"></auto-complete>
          <auto-complete ref="autocompleteRecipient" :disabled="loading" input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" :disabled="loading" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading || !recipient || !amount || !from" :loading="loading" class="btn btn-primary mr-1" @click="sendTokens">Send</button>
        <button :disabled="loading || !recipient || !amount || !from" class="btn" color="secondary" @click="showQRCodeModal = true">QRCode</button>
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
    icon: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    hasDelegatedTokens: {
      type: Boolean,
      default: function() {
        return false;
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
      loading: false,
      from: null,
      recipient: null,
      amount: null,
      dialog: null,
      warning: null,
      error: null
    };
  },
  computed: {
    disabled() {
      return !this.hasDelegatedTokens
             || !this.etherBalance
             || this.etherBalance === 0
             || (typeof this.etherBalance === "string" 
                 && (!this.etherBalance.length || this.etherBalance.trim() === "0"));
    }
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.autocompleteFrom.clear();
        this.$refs.autocompleteRecipient.clear();
        this.showQRCodeModal = false;
        this.from = null;
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

      this.loading = true;
      try {
        this.contract.methods.transferFrom(this.from, this.recipient, this.amount.toString()).estimateGas({gas: window.walletSettings.userDefaultGas, gasPrice: window.walletSettings.gasPrice})
          .then(result => {
            if (result > window.walletSettings.userDefaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userDefaultGas} while the estimation of necessary gas is ${result}`;
            }
            this.$emit("loading");
            return this.contract.methods.transferFrom(this.from, this.recipient, this.amount.toString()).send({from: this.account})
              .on('confirmation', (confirmationNumber, receipt) => {
                console.debug("send transaction transferFrom - confirmation", confirmationNumber, receipt);
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
            console.debug("Web3 contract.transferFrom method - error", e);
            this.error = `Error while proceeding: ${e}`;
            this.loading = false;
            this.$emit("end-loading");
          });
      } catch(e) {
        console.debug("Web3 contract.transferFrom method - error", e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit("end-loading");
      }
    }
  }
};
</script>

