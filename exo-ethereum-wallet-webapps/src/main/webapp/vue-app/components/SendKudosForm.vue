<template>
  <v-card v-if="contractDetails">
    <v-flex v-if="loading" class="pt-0 text-xs-center">
      <v-progress-circular color="primary" indeterminate size="20" />
    </v-flex>
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
          :title="`${validRecipientsCount} transactions with a total amount to send ${totalTokens} ${contractDetails && contractDetails.symbol}. Overall transactions fee:`"
          :disabled="loading"
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
import {savePeriodKudosTransactions, savePeriodKudosTransaction} from '../WalletKudosServices.js';

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
    },
    periodType: {
      type: String,
      default: function() {
        return null;
      }
    },
    startDateInSeconds: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    endDateInSeconds: {
      type: Number,
      default: function() {
        return 0;
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
      isContractOwner: false,
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
    kudosDefaultLabel() {
      const startDate = new Date(this.startDateInSeconds * 1000).toLocaleDateString(eXo.env.portal.language);
      const endDate = new Date(this.endDateInSeconds * 1000).toLocaleDateString(eXo.env.portal.language);
      return `Kudos reward for period: ${startDate} to ${endDate}`;
    },
    kudosDefaultMessage() {
      return this.kudosDefaultLabel;
    },
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
      return this.estimatedGas && this.gasPrice ? parseInt(this.estimatedGas * this.gasPrice * this.validRecipientsCount) : 0;
    },
    transactionFeeEther() {
      return this.transactionFeeInWei ? window.localWeb3.utils.fromWei(String(this.transactionFeeInWei), "ether") : 0;
    },
    transactionFeeFiat() {
      return this.transactionFeeEther ? etherToFiat(this.transactionFeeEther) : 0;
    },
    transactionFeeToken() {
      return !this.contractDetails || this.contractDetails.isOwner || !this.transactionFeeInWei || !this.sellPriceInWei ? 0 : this.toFixed(this.transactionFeeInWei / this.sellPriceInWei);
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
      this.isContractOwner = this.contractDetails && this.contractDetails.contract && this.contractDetails.contract.options && this.contractDetails.owner && this.contractDetails.owner === this.contractDetails.contract.options.from;
      if(this.isContractOwner) {
        this.information = 'You are using the contract owner addres, thus the transaction fee will be payed in ether.';
      } else {
        this.information = null;
      }
    }
  },
  methods: {
    init() {
      if (this.$refs.autocomplete) {
        this.$refs.autocomplete.clear();
      }
      this.loadingCount = 1;
      this.transactionsSent = 0;
      this.showQRCodeModal = false;
      this.warning = null;
      this.errors = null;
      this.transactionLabel = this.kudosDefaultLabel;
      this.transactionMessage = this.kudosDefaultMessage;
      this.$emit("error", "");
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
      if(!this.periodType) {
        this.errors = 'Unable to determine Kudos period type (Week, Month...)';
      }
      if(!this.startDateInSeconds) {
        this.errors = 'Unable to determine selected start date of Kudos period';
      }
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

      const kudosTransactions = [];
      const kudosTransactionsPromises = [];
      this.recipientsHavingAddress.forEach(recipientWallet => {
        this.transactionsSent++;
        if (!window.localWeb3.utils.isAddress(recipientWallet.address)) {
          this.appendError("Invalid recipient address");
        } else if (!Number(recipientWallet.tokensToSend)) {
          this.appendError("Invalid recipient address");
        } else {
          this.loadingCount++;
          kudosTransactionsPromises.push(this.sendTokens(recipientWallet, kudosTransactions));
        }
      });
      Promise.all(kudosTransactionsPromises)
        .then(() => {
          if(kudosTransactions.length) {
            // save all kudos transactions again for current period (for consistency check)
            savePeriodKudosTransactions(kudosTransactions);
          }
        });
    },
    sendTokens(recipientWallet, kudosTransactions) {
      let errorAppended = false;
      try {
        const amountToSendForReceiver = recipientWallet.tokensToSend;
        return this.contractDetails.contract.methods.transfer(recipientWallet.address, convertTokenAmountToSend(amountToSendForReceiver, this.contractDetails.decimals).toString())
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
                saveTransactionDetails(pendingTransaction, contractDetails);

                // *async* save kudos transaction for current period
                const kudosTransaction = {
                  networkId: window.walletSettings.defaultNetworkId,
                  periodType: this.periodType,
                  startDateInSeconds: this.startDateInSeconds,
                  hash: pendingTransaction.hash,
                  receiverType: recipientWallet.type,
                  receiverId: recipientWallet.id,
                  receiverIdentityId: recipientWallet.identityId,
                  tokensAmountSent: String(Number(amountToSendForReceiver))
                };
                kudosTransactions.push(kudosTransaction);
                savePeriodKudosTransaction(kudosTransaction);

                // The transaction has been hashed and will be sent
                this.$emit("sent", pendingTransaction, contractDetails);

                if(this.transactionsSent === this.recipientsHavingAddress.length) {
                  this.$emit("close");
                }
              })
              .on('error', (error, receipt) => {
                console.debug("Web3 contract.transfer method - error", error);
                // The transaction has failed
                errorAppended = true;
                this.appendError(`Error sending tokens: ${truncateError(error)}`);
                this.$emit("error", `Error sending tokens: ${truncateError(error)}`);
                if(this.transactionsSent === this.recipientsHavingAddress.length) {
                  this.$emit("close");
                }
              });
          })
          .catch (e => {
            console.debug("Web3 contract.transfer method - error", e);
            if(!errorAppended) {
              this.appendError(`Error sending tokens: ${truncateError(e)}`);
            }
          })
          .finally(() => {
            this.loadingCount--;
            if(!this.useMetamask) {
              lockBrowerWallet();
            }
            if(this.transactionsSent === this.recipientsHavingAddress.length) {
              this.$emit("close");
            }
          });
      } catch(e) {
        console.debug("Web3 contract.transfer method - error", e);
        this.loadingCount--;
        if(!errorAppended) {
          this.appendError(`Error sending tokens: ${truncateError(e)}`);
        }
      }
      return Promise.resolve(null);
    },
    appendError(error) {
      if(error) {
        this.errors += `<br />${error}`;
      }
    }
  }
};
</script>
