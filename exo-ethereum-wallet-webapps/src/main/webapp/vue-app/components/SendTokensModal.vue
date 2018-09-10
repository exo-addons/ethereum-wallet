<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <v-btn v-if="icon" slot="activator" :disabled="disabled" class="mt-1 mb-1" fab dark small title="Send tokens" color="primary" icon>
      <v-icon size="20">send</v-icon>
    </v-btn>
    <button v-else slot="activator" :disabled="disabled" class="btn btn-primary mt-1 mb-1">Send Tokens</button>
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
                   information="You can scan this QR code by using a different application that supports QR code transaction generation to send tokens"
                   @close="showQRCodeModal = false" />
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Tokens</span>
      </div>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
          <i class="uiIconWarning"></i>{{ warning }}
        </div>
        <v-form>
          <auto-complete ref="autocomplete" :disabled="loading" input-label="Recipient" @item-selected="recipient = $event.address"></auto-complete>
          <v-text-field v-model.number="amount" :disabled="loading" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading || !recipient || !amount" :loading="loading" class="btn btn-primary mr-1" @click="sendTokens">Send</button>
        <button :disabled="loading || !recipient || !amount" class="btn" color="secondary" @click="showQRCodeModal = true">QRCode</button>
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
    icon: {
      type: Boolean,
      default: function() {
        return false;
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
        this.contract.methods.transfer(this.recipient, this.amount.toString()).estimateGas({gas: window.walletSettings.userPreferences.userDefaultGas, gasPrice: window.walletSettings.gasPrice})
          .then(result => {
            if (result > window.walletSettings.userPreferences.userDefaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.userDefaultGas} while the estimation of necessary gas is ${result}`;
            }
            return this.contract.methods.transfer(this.recipient, this.amount.toString()).send({from: this.account})
              .on('transactionHash', hash => {
                const gas = window.walletSettings.userPreferences.userDefaultGas ? window.walletSettings.userPreferences.userDefaultGas : 35000;

                // The transaction has been hashed and will be sent
                this.$emit("sent", {
                  hash: hash,
                  from: this.account,
                  to: this.recipient,
                  value : this.amount,
                  gas: gas,
                  gasPrice: window.walletSettings.gasPrice,
                  pending: true,
                  type: 'sendToken',
                  timestamp: Date.now()
                });
                this.dialog = false;
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
          });
      } catch(e) {
        console.debug("Web3 contract.transfer method - error", e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
      }
    }
  }
};
</script>

