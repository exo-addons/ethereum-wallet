<template>
  <v-card>
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>
      <v-form>
        <auto-complete
          ref="autocomplete"
          :disabled="loading"
          input-label="Recipient"
          input-placeholder="Select a user, a space or an address to send to"
          @item-selected="recipient = $event.address" />
        <v-text-field
          v-model.number="amount"
          :disabled="loading"
          name="amount"
          label="Amount"
          placeholder="Select an amount of tokens to send"
          class="mt-4" />
      </v-form>
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
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button :disabled="loading || !recipient || !amount" :loading="loading" class="btn btn-primary mr-1" @click="sendTokens">Send</button>
      <button :disabled="loading || !recipient || !amount" class="btn" color="secondary" @click="showQRCodeModal = true">QRCode</button>
      <v-spacer />
    </v-card-actions>
  </v-card>
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
      type: String,
      default: function() {
        return null;
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
      warning: null,
      error: null
    };
  },
  methods: {
    init() {
      this.$refs.autocomplete.clear();
      this.loading = false;
      this.showQRCodeModal = false;
      this.recipient = null;
      this.amount = null;
      this.warning = null;
      this.error = null;
    },
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
                this.$emit("close");
              })
              .on('error', (error, receipt) => {
                // The transaction has failed
                this.error = `Error sending tokens: ${error}`;
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

