<template>
  <v-card>
    <v-card-text class="pt-0">
      <div v-if="errors" class="alert alert-error v-content">
        <i class="uiIconError"></i>
        <span v-html="errors"></span>
      </div>
      <div v-if="!errors && warning && warning.length" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>{{ warning }}
      </div>
      <div v-if="!errors && !warning && information && information.length" class="alert alert-info v-content">
        <i class="uiIconInfo"></i>{{ information }}
      </div>
      <v-form @submit="$event.preventDefault();$event.stopPropagation();">
        <gas-price-choice
          :estimated-fee="transactionFeeString"
          :title="`${validRecipientsCount} transactions with a total amount to send ${totalTokens} ${contractDetails.symbol}. Overall transactions fee:`"
          @changed="gasPrice = $event" />
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
          @click:append="walletPasswordShow = !walletPasswordShow" />
        <v-text-field
          v-model="transactionLabel"
          :disabled="loading"
          type="text"
          name="transactionLabel"
          label="Private transactions label"
          placeholder="Enter label for your transaction" />
        <v-textarea
          v-model="transactionMessage"
          :disabled="loading"
          name="tokenTransactionMessage"
          label="Message to send to receiver"
          placeholder="Enter a custom message to send to receivers with your transaction"
          class="mt-4"
          rows="3"
          flat
          no-resize />
      </v-form>
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button :disabled="loading || !totalTokens" :loading="loading" class="btn btn-primary mr-1" @click="sendRewards">Send</button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import GasPriceChoice from './GasPriceChoice.vue';

import {unlockBrowerWallet, lockBrowerWallet, truncateError, hashCode, convertTokenAmountToSend, etherToFiat} from '../WalletUtils.js';
import {saveTransactionDetails} from '../WalletTransactions.js';

export default {
  components: {
    GasPriceChoice
  },
  props: {
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    recipients: {
      type: Array,
      default: function() {
        return [];
      }
    }
  },
  data () {
    return {
      loadingCount: 0,
      storedPassword: false,
      transactionLabel: '',
      transactionMessage: '',
      walletPassword: '',
      walletPasswordShow: false,
      useMetamask: false,
      gasPrice: 0,
      estimatedGas: 0,
      fiatSymbol: null,
      warning: null,
      information: null,
      errors: null
    };
  },
  computed: {
    loading() {
      return this.loadingCount > 0;
    },
    totalKudosCount() {
      let totalKudos = 0;
      this.recipientsHavingAddress.forEach(recipient => {
        totalKudos += Number(recipient.received);
      });
      return totalKudos;
    },
    totalTokens() {
      let totalTokens = 0;
      this.recipientsHavingAddress.forEach(recipient => {
        totalTokens += Number(recipient.tokensToSend);
      });
      return totalTokens;
    },
    validRecipientsCount() {
      return this.recipientsHavingAddress.length;
    },
    transactionFeeString() {
      if (this.transactionFeeToken) {
        if (!this.contractDetails) {
          return '';
        } else {
          return `${this.transactionFeeToken} ${this.contractDetails.symbol}`;
        }
      } else if (this.transactionFeeFiat) {
        return `${this.transactionFeeFiat} ${this.fiatSymbol}`;
      }
      return '';
    },
    sellPriceInWei() {
      return this.contractDetails && this.contractDetails.sellPrice ? window.localWeb3.utils.toWei(String(this.contractDetails.sellPrice), "ether") : 0;
    },
    transactionFeeInWei() { 
      return this.estimatedGas && this.gasPrice ? parseInt(this.estimatedGas * this.gasPrice * this.validRecipientsCount) : 0;
    },
    transactionFeeEther() {
      return this.transactionFeeInWei ? window.localWeb3.utils.fromWei(String(this.transactionFeeInWei), "ether") : 0;
    },
    transactionFeeFiat() {
      return this.transactionFeeEther ? etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return !this.contractDetails || this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : Number(this.transactionFeeInWei / this.sellPriceInWei).toFixed(4);
    },
    recipientsHavingAddress() {
      const recipientsHavingAddress = this.recipients ? this.recipients.slice(0) : [];
      return recipientsHavingAddress.filter(recipientWallet => recipientWallet.address && Number(recipientWallet.tokensToSend) > 0);
    }
  },
  watch: {
    contractDetails() {
      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
      }
    }
  },
  methods: {
    init() {
      if (this.$refs.autocomplete) {
        this.$refs.autocomplete.clear();
      }
      this.loadingCount = 1;
      this.showQRCodeModal = false;
      this.warning = null;
      this.errors = null;
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
          this.estimateTransactionFee();
          this.warning = null;
        }
        this.loadingCount = 0;
      });
    },
    estimateTransactionFee() {
      if (this.contractDetails && !this.contractDetails.isPaused && this.contractDetails.balance && this.contractDetails.isApproved && this.contractDetails.sellPrice && this.contractDetails.owner && this.contractDetails.contractType) {
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
          });
      }
    },
    sendRewards() {
      this.errors = null;

      if (this.contractDetails && this.contractDetails.isPaused) {
        this.warning = `Contract '${this.contractDetails.name}' is paused, thus you will be unable to send tokens`;
        return;
      }

      if (!this.validRecipientsCount) {
        this.errors = "No selected address to send to";
        return;
      }

      if (!this.totalTokens) {
        this.errors = "Invalid amount of tokens to send";
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.errors = "Password field is mandatory";
        return;
      }

      const unlocked = this.useMetamask || unlockBrowerWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.errors = "Wrong password";
        return;
      }

      if (this.contractDetails.balance < this.totalTokens) {
        this.errors = "Unsufficient funds";
        return;
      }

      this.recipientsHavingAddress.forEach(recipientWallet => {
        const sent = this.sendTokens(recipientWallet);
        if(!sent) {
          this.appendError(`Can't send tokens to user '${recipientWallet.id}'`);
        }
      });
    },
    sendTokens(recipientWallet) {
      if (!window.localWeb3.utils.isAddress(recipientWallet.address)) {
        this.appendError("Invalid recipient address");
        return;
      }

      if (!Number(recipientWallet.tokensToSend)) {
        this.appendError("Invalid recipient address");
        return;
      }

      this.loadingCount++;
      try {
        const amountToSendForReceiver = recipientWallet.tokensToSend;
        this.contractDetails.contract.methods.transfer(recipientWallet.address, convertTokenAmountToSend(amountToSendForReceiver, this.contractDetails.decimals).toString())
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
            const contractDetails = this.contractDetails;
            return contractDetails.contract.methods.transfer(recipientWallet.address, convertTokenAmountToSend(amountToSendForReceiver, contractDetails.decimals).toString())
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
                  to: recipientWallet.address,
                  value : 0,
                  gas: window.walletSettings.userPreferences.defaultGas,
                  gasPrice: this.gasPrice,
                  pending: true,
                  contractAddress: contractDetails.address,
                  contractMethodName: 'transfer',
                  contractAmount : amountToSendForReceiver,
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
              })
              .on('error', (error, receipt) => {
                console.debug("Web3 contract.transfer method - error", error);
                // The transaction has failed
                this.appendError(`Error sending tokens: ${truncateError(error)}`);
              });
          })
          .catch (e => {
            console.debug("Web3 contract.transfer method - error", e);
            this.appendError(`Error sending tokens: ${truncateError(e)}`);
          })
          .finally(() => {
            this.loadingCount--;
            if(!this.useMetamask) {
              lockBrowerWallet();
            }
            if (!this.loadingCount && !this.error) {
              this.$emit("close");
            }
          });
        return true;
      } catch(e) {
        console.debug("Web3 contract.transfer method - error", e);
        this.loadingCount--;
        this.appendError(`Error sending tokens: ${truncateError(e)}`);
      }
    },
    appendError(error) {
      this.errors += `<br />${error}`;
    }
  }
};
</script>
