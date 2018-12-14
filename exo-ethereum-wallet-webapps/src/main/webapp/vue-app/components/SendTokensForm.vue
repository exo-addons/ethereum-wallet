<template>
  <v-card>
    <v-card-text class="pt-0">
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>
      <div v-if="!error && !warning && information && information.length" class="alert alert-info v-content">
        <i class="uiIconInfo"></i>{{ information }}
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
          autocomplete="current-passord"
          @click:append="walletPasswordShow = !walletPasswordShow"
        />
        <gas-price-choice
          :estimated-fee="transactionFeeString"
          @changed="gasPrice = $event" />
        <v-text-field
          v-model="transactionLabel"
          :disabled="loading"
          type="text"
          name="transactionLabel"
          label="Label (Optional)"
          placeholder="Enter label for your transaction" />
        <v-textarea
          v-model="transactionMessage"
          :disabled="loading"
          name="tokenTransactionMessage"
          label="Message (Optional)"
          placeholder="Enter a custom message to send to the receiver with your transaction"
          class="mt-4"
          rows="3"
          flat
          no-resize />
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
      <button :disabled="loading || !recipient || !amount || !canSendToken" :loading="loading" class="btn btn-primary mr-1" @click="sendTokens">Send</button>
      <button :disabled="loading || !recipient || !amount || !canSendToken" class="btn" color="secondary" @click="showQRCodeModal = true">QRCode</button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import AddressAutoComplete from './AddressAutoComplete.vue';
import QrCodeModal from './QRCodeModal.vue';
import GasPriceChoice from './GasPriceChoice.vue';

import {unlockBrowerWallet, lockBrowerWallet, truncateError, hashCode, convertTokenAmountToSend, etherToFiat} from '../WalletUtils.js';
import {saveTransactionDetails} from '../WalletTransactions.js';
import {retrieveContractDetails} from '../WalletToken.js';

export default {
  components: {
    QrCodeModal,
    GasPriceChoice,
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
      useMetamask: false,
      recipient: null,
      isApprovedRecipient: true,
      canSendToken: true,
      amount: null,
      gasPrice: 0,
      estimatedGas: 0,
      fiatSymbol: null,
      warning: null,
      information: null,
      error: null
    };
  },
  computed: {
    transactionFeeString() {
      if (this.transactionFeeToken) {
        if (this.contractDetails) {
          return `${this.toFixed(this.transactionFeeToken)} ${this.contractDetails && this.contractDetails.symbol}`;
        } else {
          return '';
        }
      } else if (this.transactionFeeFiat) {
        return `${this.toFixed(this.transactionFeeFiat)} ${this.fiatSymbol}`;
      }
      return '';
    },
    sellPriceInWei() {
      return this.contractDetails && this.contractDetails.sellPrice ? window.localWeb3.utils.toWei(String(this.contractDetails.sellPrice), "ether") : 0;
    },
    transactionFeeInWei() { 
      return this.estimatedGas && this.gasPrice ? parseInt(this.estimatedGas * this.gasPrice) : 0;
    },
    transactionFeeEther() {
      return this.transactionFeeInWei ? window.localWeb3.utils.fromWei(String(this.transactionFeeInWei), "ether") : 0;
    },
    transactionFeeFiat() {
      return this.transactionFeeEther ? etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return !this.contractDetails || this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : this.toFixed(this.transactionFeeInWei / this.sellPriceInWei);
    }
  },
  watch: {
    contractDetails() {
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
      }
    },
    amount() {
      this.checkErrors();
    },
    recipient(newValue, oldValue) {
      this.error = null;
      if (newValue && oldValue !== newValue) {
        this.checkErrors();
        if(this.error) {
          return;
        }

        this.isApprovedRecipient = true;
        this.canSendToken = true;
        // Admin will implicitly approve account, so not necessary
        // to check if the receiver is approved or not
        if (this.contractDetails.contractType > 0) {
          this.contractDetails.contract.methods.isApprovedAccount(this.recipient).call()
            .then(isApproved => {
              this.isApprovedRecipient = isApproved;
              if (this.contractDetails && this.contractDetails.isPaused) {
                this.warning = `Contract '${this.contractDetails.name}' is paused, thus you can't send tokens`;
                this.canSendToken = false;
              } else if (!this.isApprovedRecipient && !this.contractDetails.adminLevel) {
                this.warning = `The recipient isn't approved by contract administrators to receive tokens`;
                this.canSendToken = false;
              } else if (!this.isApprovedRecipient && Number(this.contractDetails.adminLevel) > 0) {
                this.information = `The recipient isn't approved. You are an administrator of contract, thus the recipient will be implicitely approved.`;
                this.canSendToken = true;
              } else {
                this.warning = null;
                this.canSendToken = true;
              }
            });
        }
      }
    }
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
      if (!this.gasPrice) {
        this.gasPrice = window.walletSettings.minGasPrice;
      }
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.fiatSymbol = window.walletSettings.fiatSymbol;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.$nextTick(() => {
        if (this.contractDetails && this.contractDetails.isPaused) {
          this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
        } else {
          if(this.contractDetails && this.contractDetails.address && !this.contractDetails.hasOwnProperty("isApproved")) {
            // Load contract details in async mode for adminLevel test
            retrieveContractDetails(this.account, this.contractDetails, true);
          }
          this.estimateTransactionFee();
          this.warning = null;
        }
      });
    },
    estimateTransactionFee() {
      if (this.contractDetails && !this.contractDetails.isPaused && this.contractDetails.balance && this.contractDetails.sellPrice && this.contractDetails.owner && this.contractDetails.contractType) {
        const recipient = this.contractDetails.isOwner ? "0x1111111111111111111111111111111111111111" : this.contractDetails.owner;
        // Estimate gas
        this.contractDetails.contract.methods.transfer(recipient, String(Math.pow(10, this.contractDetails.decimals ? this.contractDetails.decimals : 0)))
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: window.walletSettings.userPreferences.defaultGas,
            gasPrice: this.gasPrice
          })
          .then(estimatedGas => {
            // Add 10% to ensure that the operation doesn't take more than the estimation
            this.estimatedGas = estimatedGas * 1.1;
          })
          .catch(e => {
            console.debug("Error while estimating gas", e);
          });
      }
    },
    sendTokens() {
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
        return;
      }

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

      if (this.contractDetails.balance < this.amount) {
        this.error = "Unsufficient funds";
        return;
      }

      if (!this.canSendToken) {
        this.error = "Can't send token, please verify previous errors or contact your administrator";
        return;
      }

      this.error = null;

      this.loading = true;
      try {
        this.contractDetails.contract.methods.transfer(this.recipient, convertTokenAmountToSend(this.amount, this.contractDetails.decimals).toString())
          .estimateGas({
            from: this.contractDetails.contract.options.from,
            gas: window.walletSettings.userPreferences.defaultGas,
            gasPrice: this.gasPrice
          })
          .catch(e => {
            console.error("Error estimating necessary gas", e);
            return 0;
          })
          .then(estimatedGas => {
            if (estimatedGas > window.walletSettings.userPreferences.defaultGas) {
              this.warning = `You have set a low gas ${window.walletSettings.userPreferences.defaultGas} while the estimation of necessary gas is ${estimatedGas}. Please change it in your preferences.`;
              return;
            }
            const sender = this.contractDetails.contract.options.from;
            const receiver = this.recipient;
            const contractDetails = this.contractDetails;
            return contractDetails.contract.methods.transfer(receiver, convertTokenAmountToSend(this.amount, contractDetails.decimals).toString())
              .send({
                from: sender,
                gas: window.walletSettings.userPreferences.defaultGas,
                gasPrice: this.gasPrice
              })
              .on('transactionHash', hash => {
                const gas = window.walletSettings.userPreferences.defaultGas ? window.walletSettings.userPreferences.defaultGas : 35000;

                const pendingTransaction = {
                  hash: hash,
                  from: sender.toLowerCase(),
                  to: receiver,
                  value : 0,
                  gas: window.walletSettings.userPreferences.defaultGas,
                  gasPrice: this.gasPrice,
                  pending: true,
                  contractAddress: contractDetails.address,
                  contractMethodName: 'transfer',
                  contractAmount : this.amount,
                  label: this.transactionLabel,
                  message: this.transactionMessage,
                  timestamp: Date.now(),
                  fee: this.transactionFeeEther,
                  feeFiat: this.transactionFeeFiat,
                  feeToken: this.transactionFeeToken
                };

                // *async* save transaction message for contract, sender and receiver
                saveTransactionDetails(pendingTransaction);

                // The transaction has been hashed and will be sent
                this.$emit("sent", pendingTransaction, contractDetails);
                this.$emit("close");
              })
              .on('error', (error, receipt) => {
                console.debug("Web3 contract.transfer method - error", error);
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
    },
    checkErrors() {
      this.error = null;

      if(this.recipient === this.account && this.contractDetails.contractType > 0) {
        this.error = `You can't send '${this.contractDetails.name}' to yourself`;
        this.canSendToken = false;
        return;
      }

      if(this.amount && $.isNumeric(this.amount)) {
        this.error = this.contractDetails.balance >= this.amount ? null : 'Unsufficient funds';
        return;
      } else if(this.amount && (isNaN(parseFloat(this.amount)) || !isFinite(this.amount) || this.amount <= 0)) {
        this.error = "Invalid amount";
        return;
      }
    }
  }
};
</script>
