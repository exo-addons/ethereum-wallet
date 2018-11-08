<template>
  <v-card>
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>
      <v-form @submit="$event.preventDefault();$event.stopPropagation();">
        <address-auto-complete
          ref="autocomplete"
          :disabled="loading"
          input-label="Recipient"
          input-placeholder="Select a user, a space or an address to send to"
          @item-selected="recipient = $event.address; $emit('receiver-selected', $event)" />

        <v-container flat fluid grid-list-lg class="mt-4 pl-2">
          <v-layout row wrap>
            <v-text-field
              v-model.number="amount"
              :disabled="loading"
              name="amount"
              label="Amount"
              placeholder="Select an amount of tokens to send"
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
          id="tokenTransactionMessage"
          v-model="transactionMessage"
          :disabled="loading"
          name="tokenTransactionMessage"
          label="Message (Optional)"
          placeholder="Enter a custom message to send to the receiver with your transaction"
          class="mt-4"
          rows="3"
          flat
          no-resize />
        <div v-if="contractDetails && contractDetails.sellPrice && transactionFee[contractDetails.address]">
          Estimated transaction fee: <code>{{ transactionFee[contractDetails.address] }} {{ contractDetails.symbol }}</code>
        </div>
      </v-form>
      <qr-code-modal 
        :to="recipient"
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
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';

import {unlockBrowerWallet, lockBrowerWallet, truncateError, hashCode, convertTokenAmountToSend} from '../WalletUtils.js';
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
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      loading: false,
      showQRCodeModal: false,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      transactionFee: {},
      useMetamask: false,
      recipient: null,
      amount: null,
      warning: null,
      error: null
    };
  },
  methods: {
    init() {
      if (this.$refs.autocomplete) {
        this.$refs.autocomplete.clear();
      }
      this.loading = false;
      this.showQRCodeModal = false;
      this.recipient = null;
      this.amount = null;
      this.warning = null;
      this.error = null;
      this.transactionMessage = null;
      this.transactionLabel = null;
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.estimateTransactionFee();
    },
    estimateTransactionFee() {
      if (this.contractDetails && this.contractDetails.sellPrice && !this.transactionFee[this.contractDetails.address] && this.contractDetails.owner && this.account && this.contractDetails.owner.toLowerCase() !== this.account.toLowerCase()) {
        // Estimate gas
        this.contractDetails.contract.methods.transfer(this.contractDetails.owner, "2")
          .estimateGas({
            gas: 900000,
            gasPrice: window.walletSettings.gasPrice
          })
          .then(estimatedGas => {
            const transactionFeeInWei = parseInt(estimatedGas * 1.1 * window.walletSettings.gasPrice);
            const sellPriceInWei = window.localWeb3.utils.toWei(String(this.contractDetails.sellPrice), "ether");
            this.$set(this.transactionFee, this.contractDetails.address, transactionFeeInWei / sellPriceInWei);
          });
      }
    },
    sendTokens() {
      this.error = null;
      this.warning = null;

      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = "Invalid recipient address";
        return;
      }

      if (!this.amount || isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = "Invalid amount";
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.error = "Password field is mandatory";
        return;
      }

      const unlocked = this.useMetamask || unlockBrowerWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = "Wrong password";
        return;
      }

      this.loading = true;
      try {
        this.contractDetails.contract.methods.transfer(this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
          .estimateGas({
            gas: 9000000,
            gasPrice: window.walletSettings.gasPrice
          })
          .then(estimatedGas => {
            if (estimatedGas > window.walletSettings.userPreferences.defaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.defaultGas} while the estimation of necessary gas is ${estimatedGas}. Please change it in your preferences.`;
              return;
            }
            return this.contractDetails.contract.methods.transfer(this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
              .send({
                from: this.account,
                gas: window.walletSettings.userPreferences.defaultGas
              })
              .on('transactionHash', hash => {
                saveTransactionMessage(hash, this.transactionMessage, this.transactionLabel);
                const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;

                // The transaction has been hashed and will be sent
                this.$emit("sent", {
                  hash: hash,
                  from: this.account,
                  to: this.recipient,
                  value : 0,
                  gas: window.walletSettings.userPreferences.defaultGas,
                  gasPrice: window.walletSettings.gasPrice,
                  pending: true,
                  contractAddress: this.contractDetails.address,
                  contractMethodName: 'transfer',
                  contractAmount : this.amount,
                  label: this.transactionLabel,
                  message: this.transactionMessage,
                  timestamp: Date.now(),
                  feeToken: this.transactionFee[this.contractDetails.address]
                }, this.contractDetails);
                this.$emit("close");
              })
              .on('error', (error, receipt) => {
                console.debug("Web3 contract.transferFrom method - error", error);
                // The transaction has failed
                this.error = `Error sending tokens: ${truncateError(error)}`;
                // Display error on main screen only when dialog is not opened
                if (!this.dialog) {
                  this.$emit("error", this.error);
                }
              });
          })
          .catch (e => {
            console.debug("Web3 contract.transfer method - error", e);
            this.error = `Error sending tokens: ${truncateError(e)}`;
          })
          .finally(() => {
            this.loading = false;
            if(!this.useMetamask) {
              lockBrowerWallet();
            }
          });
      } catch(e) {
        console.debug("Web3 contract.transfer method - error", e);
        this.loading = false;

        this.error = `Error sending tokens: ${truncateError(e)}`;
      }
    }
  }
};
</script>

