<template>
  <v-card>
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-form @submit="$event.preventDefault();$event.stopPropagation();">
        <address-auto-complete
          ref="autocomplete"
          :disabled="loading"
          input-label="Recipient"
          input-placeholder="Select a user, a space or an address to send to"
          title="Select a user, a space or an address to send to"
          @item-selected="recipient = $event.address; $emit('receiver-selected', $event)" />

        <v-container flat fluid grid-list-lg class="mt-4 pl-2">
          <v-layout row wrap>
            <v-text-field
              v-model.number="amount"
              :disabled="loading"
              name="amount"
              label="Amount"
              placeholder="Select an amount of ethers to send"
              @input="$emit('amount-selected', amount)" />
            <slot></slot>
          </v-layout>
        </v-container>
        <v-text-field
          v-if="!storedPassword"
          v-model="walletPassword"
          :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
          :type="walletPasswordShow ? 'text' : 'password'"
          :disabled="loading"
          name="walletPassword"
          label="Wallet password"
          placeholder="Enter your wallet password"
          counter
          @click:append="walletPasswordShow = !walletPasswordShow"
        />
        <v-text-field
          v-model="transactionLabel"
          :disabled="loading"
          type="text"
          name="transactionLabel"
          label="Label (Optional)"
          placeholder="Enter label for your transaction" />
        <v-textarea
          id="transactionMessage"
          v-model="transactionMessage"
          :disabled="loading"
          name="transactionMessage"
          label="Message (Optional)"
          placeholder="Enter a custom message to send to the receiver with your transaction"
          class="mt-4"
          rows="3"
          flat
          no-resize />
      </v-form>

      <qr-code-modal 
        :from="account"
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
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import {unlockBrowerWallet, lockBrowerWallet, gasToEther, hashCode, truncateError} from '../WalletUtils.js';
import {saveTransactionMessage} from '../WalletTransactions.js';

export default {
  components: {
    QrCodeModal,
    AddressAutoComplete
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
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
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
      this.transactionMessage = null;
      this.transactionLabel = null;
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
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

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = "Password field is mandatory";
        return;
      }

      const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;
      if (this.amount + gasToEther(gas) >= this.balance) {
        this.error = "Unsufficient funds";
        return;
      }

      const unlocked = this.useMetamask || unlockBrowerWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = "Wrong password";
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
            saveTransactionMessage(hash, this.transactionMessage, this.transactionLabel);
            // The transaction has been hashed and will be sent
            this.$emit("sent", {
              hash: hash,
              from: transaction.from,
              to: transaction.to,
              value : transaction.value,
              gas: transaction.gas,
              gasPrice: transaction.gasPrice,
              pending: true,
              label: this.transactionLabel,
              message: this.transactionMessage,
              type: 'sendEther',
              timestamp: Date.now()
            });
            this.$emit("close");
          })
          .on('error', (error, receipt) => {
            // The transaction has failed
            this.error = truncateError(`Error sending ether: ${error}`);
            this.loading = false;
            // Display error on main screen only when dialog is not opened
            if (!this.dialog) {
              this.$emit("error", this.error);
            }
          });
      } catch(e) {
        console.debug("Web3.eth.sendTransaction method - error", e);
        this.loading = false;
        this.error = truncateError(`Error sending ether: ${e}`);
      } finally {
        if (!this.useMetamask) {
          lockBrowerWallet();
        }
      }
    }
  }
};
</script>

