<template>
  <v-dialog v-model="createNewToken" content-class="uiPopup createNewToken" fullscreen hide-overlay transition="dialog-bottom-transition" persistent>
    <button slot="activator" class="btn btn-primary mt-3" @click="createNewToken = true">
      Deploy new Token
    </button>
    <v-card flat>
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="createNewToken = false"></a>
        <span class="PopupTitle popupTitle">Deploy new ERC20 Token contract</span>
      </div>
      <div v-if="error" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-stepper v-model="step">
        <v-stepper-header flat>
          <v-stepper-step :complete="step > 1" step="1">Deployment of Data contract</v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 2" step="2">Deployment of Token contract</v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 3" step="3">Deployment of Proxy contract</v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 4" step="4">Transfer ownership</v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step > 5" step="5">ERC 20 initialization</v-stepper-step>
          <v-divider />
          <v-stepper-step :complete="step === 6" step="6">Completed</v-stepper-step>
        </v-stepper-header>
        <v-stepper-items>
          <v-stepper-content step="1">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="transactionFeeByStep[step]"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++" />
          </v-stepper-content>
          <v-stepper-content step="2">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="transactionFeeByStep[step]"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++" />
          </v-stepper-content>
          <v-stepper-content step="3">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :contract-address="contractAddressByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="transactionFeeByStep[step]"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Deploy"
              @proceed="proceedStep($event)"
              @next="step++" />
          </v-stepper-content>
          <v-stepper-content step="4">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="transactionFeeByStep[step]"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Send"
              @proceed="proceedStep($event)"
              @next="step++" />
          </v-stepper-content>
          <v-stepper-content step="5">
            <contract-deployment-step
              :network-id="networkId"
              :stored-password="storedPassword"
              :transaction-hash="transactionHashByStep[step]"
              :gas="gasByStep[step]"
              :processing="processingStep[step]"
              :processed="processedStep[step]"
              :transaction-fee="transactionFeeByStep[step]"
              :disabled-button="disabledButton"
              :fiat-symbol="fiatSymbol"
              button-title="Send"
              @proceed="proceedStep($event)"
              @next="step++">

              <v-form ref="form" class="pl-5 pr-5 pt-3 flex">
                <v-text-field
                  v-model="newTokenName"
                  :rules="mandatoryRule"
                  label="Token name"
                  placeholder="Enter the ERC20 token name"
                  required />
                <v-text-field
                  v-model="newTokenSymbol"
                  :rules="mandatoryRule"
                  label="Token symbol"
                  placeholder="Enter the token symbol to uses to display token amounts"
                  required />
                <v-text-field
                  v-model="newTokenInitialCoins"
                  :rules="mandatoryRule"
                  label="Initial token coins supply"
                  placeholder="Enter an amount of initial token supply"
                  required />
                <v-slider v-model="newTokenDecimals"
                          :label="`Token coins decimals: ${newTokenDecimals}`"
                          :min="0"
                          :max="18"
                          :step="1"
                          type="number"
                          required />
              </v-form>
            </contract-deployment-step>
          </v-stepper-content>
          <v-stepper-content step="6">
            <v-card flat>
              <v-card-title>
                The Token has been deployed.
                <a :href="tokenEtherscanLink" target="_blank"> See it on etherscan</a>
              </v-card-title>
              <v-card-actions>
                <v-btn :loading="processingStep[step]" :disabled="processingStep[step]" color="primary" @click="finishInstallation"><span class="ml-2 mr-2">Finish deployment</span></v-btn>
              </v-card-actions>
            </v-card>
          </v-stepper-content>
        </v-stepper-items>
      </v-stepper>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import ContractDeploymentStep from './ContractDeploymentStep.vue';

import {newContractInstanceByNameAndAddress, estimateContractDeploymentGas, newContractInstanceByName, deployContract, saveContractAddressAsDefault} from '../WalletToken.js';
import {getTokenEtherscanlink, gasToFiat, unlockBrowerWallet, lockBrowerWallet, hashCode, convertTokenAmountToSend} from '../WalletUtils.js';

export default {
  components: {
    ContractDeploymentStep,
    WalletAddress
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data () {
    return {
      error: '',
      storedPassword: false,
      newTokenName: '',
      newTokenSymbol: '',
      newTokenDecimals: 18,
      newTokenInitialCoins: 0,
      createNewToken: false,
      useMetamask: false,
      transactionHashByStep: {},
      contractAddressByStep: {},
      processedStep: {},
      step: 0,
      contractInstancesByStep: {},
      gasByStep: {},
      transactionFeeByStep: {},
      processingStep: {},
      mandatoryRule: [
        (v) => !!v || 'Field is required'
      ]
    };
  },
  computed: {
    contractNameByStep() {
      if (this.step === 1) {
        return 'ERTTokenDataV1';
      } else if (this.step === 2) {
        return 'ERTTokenV1';
      } else if (this.step === 3) {
        return 'ERTToken';
      }
      return null;
    },
    tokenEtherscanLink() {
      if (this.contractAddressByStep[2]) {
        return getTokenEtherscanlink(this.networkId) + this.contractAddressByStep[2];
      }
    },
    contractDeploymentParameters() {
      if (this.step === 2) {
        // ERTTokenV1 parameters
        return [this.contractAddressByStep[1], '0x0000000000000000000000000000000000000000'];
      } else if (this.step === 3) {
        // ERTToken parameters
        return [this.contractAddressByStep[2], this.contractAddressByStep[1]];
      }
      return [];
    },
    disabledButton() {
      return !this.transactionFeeByStep[this.step] || this.contractAddressByStep[this.step] || this.processingStep[this.step];
    }
  },
  watch: {
    newTokenGasPrice() {
      this.newTokenGasPriceGWEI = window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'gwei');
      this.calculateGasPriceInFiat();
    },
    createNewToken() {
      if (this.createNewToken) {
        this.resetContractForm();
      }
    },
    step() {
      this.initializeStep();
    }
  },
  methods: {
    resetContractForm() {
      this.step = 0;
      this.error = '';
      this.newTokenName = '';
      this.newTokenSymbol = '';
      this.newTokenDecimals = 18;
      this.newTokenInitialCoins = 1000000;
      this.contractInstancesByStep = {};
      this.gasByStep = {};
      this.transactionFeeByStep = {};
      this.processingStep = {};
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
      this.loadState();
    },
    initializeStep() {
      if (this.step && !this.gasByStep[this.step]) {
        if (this.step < 4) {
          if (this.contractAddressByStep[this.step]) {
            // Add it inside a constant in case it changes in parallel
            const step = this.step;
            return newContractInstanceByNameAndAddress(this.contractNameByStep, this.contractAddressByStep[step])
              .then(instance => this.$set(this.contractInstancesByStep, step, instance) && this.$set(this.processedStep, step, true))
              .catch(e => this.error = `Error getting contract with address ${this.contractAddressByStep[step]}: ${e}`);
          } else {
            const step = this.step;
            return newContractInstanceByName(this.contractNameByStep, ...this.contractDeploymentParameters)
              .then(instance => {
                this.$set(this.contractInstancesByStep, step, instance);
                return estimateContractDeploymentGas(instance);
              })
              .then(estimatedGas => {
                this.$set(this.gasByStep, step, parseInt(estimatedGas * 1.1));
                this.$set(this.transactionFeeByStep, step, this.calculateGasPriceInFiat(this.gasByStep[step]));
              })
              .catch(e => this.error = `Error processing contract deployment estimation: ${e}`);
          }
        } else if(this.step === 4 && !this.processedStep[this.step]) {
          this.contractInstancesByStep[1].methods.transferDataOwnership(this.contractAddressByStep[3], this.contractAddressByStep[2])
            .estimateGas({
              gas: 9000000,
              gasPrice: window.walletSettings.gasPrice
            })
            .then(estimatedGas => {
              this.$set(this.gasByStep, this.step, parseInt(estimatedGas * 1.1));
              this.$set(this.transactionFeeByStep, this.step, this.calculateGasPriceInFiat(this.gasByStep[this.step]));
            });
        } else if(this.step === 5 && !this.processedStep[this.step]) {
          this.contractInstancesByStep[2].methods.initialize(this.contractAddressByStep[3], convertTokenAmountToSend(1000000, 18).toString(), "Token name", 18, "T")
            .estimateGas({
              gas: 9000000,
              gasPrice: window.walletSettings.gasPrice
            })
            .then(estimatedGas => {
              this.$set(this.gasByStep, this.step, parseInt(estimatedGas * 1.1));
              this.$set(this.transactionFeeByStep, this.step, this.calculateGasPriceInFiat(this.gasByStep[this.step]));
            });
        }
      }
    },
    calculateGasPriceInFiat(gas) {
      const gasPriceInEther = window.localWeb3.utils.fromWei(String(window.walletSettings.gasPrice), 'ether');
      return gasToFiat(gas, gasPriceInEther);
    },
    proceedStep(password) {
      const gasLimit = this.gasByStep[this.step];
      if (!gasLimit) {
        this.error = 'Gas estimation isn\'t done';
        return;
      }

      // Increase gas limit by 10% to ensure that the transaction doesn't go 'Out of Gas'
      const gasPrice = window.walletSettings.gasPrice;

      this.error = null;

      if (!this.storedPassword && (!password || !password.length)) {
        this.error = "Password field is mandatory";
        return;
      }

      const unlocked = this.useMetamask || unlockBrowerWallet(this.storedPassword ? window.walletSettings.userP : hashCode(password));
      if (!unlocked) {
        this.error = "Wrong password";
        return;
      }

      try {
        const thiss = this;
        if (this.step < 4) {
          const contractInstance = this.contractInstancesByStep[this.step];
          if (!contractInstance) {
            this.error = 'Contract instance not initialized';
            return;
          }

          deployContract(contractInstance, this.account, gasLimit, gasPrice, this.updateTransactionHash)
            .then((newContractInstance, error) => {
              if (error) {
                throw error;
              }
              if (!newContractInstance || !newContractInstance.options || !newContractInstance.options.address) {
                throw new Error('Cannot find address of newly deployed address');
              }
              this.$set(this.contractInstancesByStep, this.step, newContractInstance);
              this.$set(this.contractAddressByStep, this.step, newContractInstance.options.address);
              this.$set(this.processedStep, this.step, true);
              this.saveState();
            })
            .catch(e => {
              console.debug("deployContract method - error", e);
              this.error = `Error during contract deployment: ${e}`;
            })
            .finally(() => this.$set(this.processingStep, this.step, false));
        } else if(this.step === 4) {
          this.contractInstancesByStep[1].methods.transferDataOwnership(this.contractAddressByStep[3], this.contractAddressByStep[2])
            .send({
              from: this.account,
              gasPrice: window.walletSettings.gasPrice,
              gas: gasLimit
            })
            .on('transactionHash', hash => {
              this.updateTransactionHash(hash);
            })
            .then(() => {
              this.$set(this.processedStep, this.step, true);
              this.saveState();
            })
            .finally(() => this.$set(this.processingStep, this.step, false));
        } else if(this.step === 5) {
          this.contractInstancesByStep[2].methods.initialize(this.contractAddressByStep[3], convertTokenAmountToSend(this.newTokenInitialCoins, this.newTokenDecimals).toString(), this.newTokenName, this.newTokenDecimals, this.newTokenSymbol)
            .send({
              from: this.account,
              gasPrice: window.walletSettings.gasPrice,
              gas: gasLimit
            })
            .on('transactionHash', hash => {
              this.updateTransactionHash(hash);
            })
            .then(() => {
              this.$set(this.processedStep, this.step, true);
              this.saveState();
            })
            .finally(() => this.$set(this.processingStep, this.step, false));
        } else if(this.step === 6) {
          this.saveState();
        }
      } catch(e) {
        lockBrowerWallet();
        console.debug("proceedStep method - error", e);
        this.$set(this.processingStep, this.step, false);
        this.error = `Error during contract deployment: ${e}`;
      }
    },
    clearState() {
      localStorage.removeItem(`exo-wallet-contract-deployment-${this.networkId}`);
    },
    loadState() {
      this.contractAddressByStep = {};
      this.processedStep = {};
      this.transactionHashByStep = {};
      this.step = 1;
      if (localStorage.getItem(`exo-wallet-contract-deployment-${this.networkId}`) != null) {
        const storedState = JSON.parse(localStorage.getItem(`exo-wallet-contract-deployment-${this.networkId}`));
        this.contractAddressByStep = storedState.contractAddressByStep;
        this.processedStep = storedState.processedStep;
        this.transactionHashByStep = storedState.transactionHashByStep;
        // To initialize steps
        for (this.step = 0; this.step < storedState.step; this.step++){
          this.initializeStep();
        }
      }
    },
    saveState() {
      localStorage.setItem(`exo-wallet-contract-deployment-${this.networkId}`, JSON.stringify({
        step: this.step,
        processedStep: this.processedStep,
        transactionHashByStep: this.transactionHashByStep,
        contractAddressByStep: this.contractAddressByStep
      }));
    },
    updateTransactionHash(hash) {
      this.$set(this.transactionHashByStep, this.step, hash);
      this.$set(this.processingStep, this.step, true);
    },
    finishInstallation() {
      const contractAddress = this.contractAddressByStep[3];
      const contractDetails = {
        networkId: this.networkId,
        address: contractAddress,
        name: this.newTokenName,
        symbol: this.newTokenSymbol
      };
      this.$set(this.processingStep, this.step, true);
      // Save conract address to display for all users
      return saveContractAddressAsDefault(contractDetails)
        .then(resp => {
          if (resp && resp.ok) {
            this.$emit("list-updated", contractAddress);
            this.createNewToken = false;
            this.clearState();
          } else {
            this.loading = false;
            this.error = `Contract deployed, but an error occurred while saving it as default contract to display for all users`;
          }
        })
        .catch(e => {
          console.debug("saveContractAddressAsDefault method - error", e);
          this.error = `An error occurred while saving it as default contract to display for all users: ${e}`;
        })
        .finally(() => this.$set(this.processingStep, this.step, false));
    }
  }
};
</script>