<template>
  <v-card>
    <v-card-text>
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-form>
        <auto-complete ref="autocomplete" :disabled="loading" input-label="Recipient" @item-selected="recipient = $event.address" />
        <v-text-field v-model.number="amount" :disabled="loading" name="amount" label="Amount" />
      </v-form>
      <qr-code-modal :from="account"
                     :to="recipient"
                     :amount="amount"
                     :open="showQRCodeModal"
                     title="Send Ether QR Code"
                     information="You can scan this QR code by using a different application that supports QR code transaction generation to send ethers"
                     @close="showQRCodeModal = false" />
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button :disabled="!account || loading || !recipient || !amount" :loading="loading" class="btn btn-primary mr-1" @click="sendEther">Send</button>
      <button :disabled="!account || loading || !recipient || !amount" class="btn" color="secondary" @click="showQRCodeModal = true">QRCode</button>
    </v-card-actions>
  </v-card>
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
        return null;
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
      error: null
    };
  },
  methods: {
    init() {
      this.$refs.autocomplete.clear();
      this.loading = false;
      this.recipient = null;
      this.amount = null;
      this.error = null;      
    },
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

      const gas = window.walletSettings.userPreferences.userDefaultGas ? window.walletSettings.userPreferences.userDefaultGas : 35000;
      if (this.amount + gasToEther(gas) >= this.balance) {
        this.error = "Unsufficient funds";
        return;
      }

      this.loading = true;
      try {
        const amount = window.localWeb3.utils.toWei(this.amount.toString(), "ether");
        const transaction = {
          from: this.account.toLowerCase(),
          to: this.recipient,
          value: window.localWeb3.utils.toWei(this.amount.toString(), "ether"),
          gas: gas,
          gasPrice: window.walletSettings.gasPrice
        };
        // Send an amount of ether to a third person
        window.localWeb3.eth.sendTransaction(transaction)
          .on('transactionHash', hash => {
            // The transaction has been hashed and will be sent
            this.$emit("sent", {
              hash: hash,
              from: transaction.from,
              to: transaction.to,
              value : transaction.value,
              gas: transaction.gas,
              gasPrice: transaction.gasPrice,
              pending: true,
              type: 'sendEther',
              timestamp: Date.now()
            });
            this.$emit("close");
          })
          .on('error', (error, receipt) => {
            // The transaction has failed
            this.error = `Error sending ether: ${error}`;
            this.loading = false;
            this.$emit("error", this.error);
          });
      } catch(e) {
        console.debug("Web3.eth.sendTransaction method - error", e);
        this.loading = false;
        this.error = `Error sending ether: ${e}`;
      }
    }
  }
};
</script>

