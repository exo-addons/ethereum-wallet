<template>
  <v-dialog v-model="createNewToken" content-class="uiPopup createNewToken" fullscreen hide-overlay transition="dialog-bottom-transition" persistent @keydown.esc="createNewToken = false">
    <button slot="activator" class="btn btn-primary mt-3" @click="createNewToken = true">
      Deploy new Token
    </button>
    <v-card>
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="createNewToken = false"></a>
        <span class="PopupTitle popupTitle">Deploy new ERC20 Token contract</span>
      </div>
      <v-form ref="form" v-model="valid" class="pl-5 pr-5 pt-3">
        <div class="text-xs-center">
          <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
        </div>

        <div v-if="newTokenAddress" class="alert alert-success v-content">
          <i class="uiIconSuccess"></i>
          Contract created under address: 
          <wallet-address :value="newTokenAddress" />
        </div>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <div v-if="!error && warning && warning.length" class="alert alert-warning v-content">
          <i class="uiIconWarning"></i>{{ warning }}
        </div>

        <h4>ERC20 Token contract details</h4>
        <v-divider class="mb-4"/>
        <v-text-field
          v-model="newTokenName"
          :rules="mandatoryRule"
          label="Token name"
          placeholder="Input the ERC20 token name"
          required />
        <v-text-field
          v-model="newTokenSymbol"
          :rules="mandatoryRule"
          label="Token symbol"
          placeholder="Input the token symbol to uses to display token amounts"
          required />
        <v-slider v-model="newTokenInitialCoins"
                  :label="`Initial token coins supply: ${newTokenInitialCoins}`"
                  :min="0"
                  :max="1000000"
                  :step="10000"
                  type="number"
                  required />

        <h4>Contract creation transaction fee</h4>
        <v-divider class="mb-4"/>
        <v-slider v-model="newTokenGas"
                  :label="`Gas limit: ${newTokenGas}${newTokenGasInFiat ? ' (' + newTokenGasInFiat + ' ' + fiatSymbol + ')' : ''}`"
                  :min="50000"
                  :max="800000"
                  :step="1000"
                  type="number"
                  required />
        <v-slider v-model="newTokenGasPrice"
                  :label="`Gas price (Gwei): ${newTokenGasPriceGWEI}`"
                  :min="0"
                  :max="60000000000"
                  :step="1000000000"
                  type="number"
                  required />

        <h4 v-if="!storedPassword">Your wallet password</h4>
        <v-text-field
          v-if="!storedPassword"
          v-model="walletPassword"
          :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
          :type="walletPasswordShow ? 'text' : 'password'"
          :disabled="loading"
          name="walletPassword"
          placeholder="Input your wallet password"
          counter
          @click:append="walletPasswordShow = !walletPasswordShow"
        />
        <v-list>
          <v-list-tile>
            <v-spacer />
            <v-list-tile-action>
              <button :disabled="loading" class="btn btn-primary" @click="saveContract">Deploy</button>
            </v-list-tile-action>
            <v-spacer />
          </v-list-tile>
        </v-list>
      </v-form>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from '../WalletConstants.js';
import {getContractsAddresses, saveContractAddress, saveContractAddressAsDefault, newContractInstance, deployContract} from '../WalletToken.js';
import {searchAddress} from '../WalletAddressRegistry.js';
import {gasToFiat, unlockBrowerWallet, lockBrowerWallet, hashCode} from '../WalletUtils.js';

export default {
  components: {
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
      loading: false,
      errorMessage: '',
      warning: null,
      storedPassword: false,
      newTokenName: '',
      newTokenSymbol: '',
      newTokenGas: 0,
      newTokenGasPrice: 0,
      newTokenGasPriceGWEI: 0,
      newTokenGasInFiat: 0,
      newTokenDecimals: 0,
      newTokenInitialCoins: 0,
      newTokenSetAsDefault: true,
      newTokenAddress: '',
      walletPassword: '',
      walletPasswordShow: false,
      createNewToken: false,
      useMetamask: false,
      mandatoryRule: [
        (v) => !!v || 'Field is required'
      ],
      valid: false
    };
  },
  computed: {
    error() {
      if(this.loading) {
        return null;
      } else if (this.errorMessage) {
        return this.errorMessage;
      }
      return null;
    }
  },
  watch: {
    newTokenGas() {
      this.calculateGasPriceInFiat();
    },
    newTokenGasPrice() {
      this.newTokenGasPriceGWEI = window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'gwei');
      this.calculateGasPriceInFiat();
    },
    createNewToken() {
      if (this.createNewToken) {
        this.resetContractForm();
      }
    }
  },
  methods: {
    resetContractForm() {
      this.loading = false;
      this.errorMessage = '';
      this.warning = null;
      this.newTokenName = '';
      this.newTokenSymbol = '';
      this.newTokenGas = 700000;
      this.newTokenGasPrice = window.walletSettings.gasPrice;
      this.newTokenDecimals = 0;
      this.newTokenInitialCoins = 10000;
      this.newTokenSetAsDefault = true;
      this.newTokenAddress = '';
      this.valid = false;
      this.contracts = [];
      this.useMetamask = window.walletSettings.userPreferences.useMetamask;
      this.storedPassword = this.useMetamask || (window.walletSettings.storedPassword && window.walletSettings.browserWalletExists);
    },
    calculateGasPriceInFiat() {
      if (this.newTokenGas && this.newTokenGasPrice) {
        const gasPriceInEther = window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'ether');
        this.newTokenGasInFiat = gasToFiat(this.newTokenGas, gasPriceInEther);
      } else {
        this.newTokenGasInFiat = 0;
      }      
    },
    saveContract(event) {
      event.preventDefault();
      event.stopPropagation();

      this.errorMessage = null;
      this.warning = null;

      if(!this.$refs.form.validate()) {
        return;
      }

      if (!this.storedPassword && (!this.walletPassword || !this.walletPassword.length)) {
        this.errorMessage = "Password field is mandatory";
        return;
      }

      this.loading = true;
      const unlocked = this.useMetamask || unlockBrowerWallet(this.storedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      if (!unlocked) {
        this.error = "Wrong password";
        this.loading = false;
        return;
      }

      try {
        const NEW_TOKEN_DEPLOYMENT_TX = newContractInstance(this.newTokenInitialCoins, this.newTokenName, this.newTokenDecimals, this.newTokenSymbol);
        deployContract(NEW_TOKEN_DEPLOYMENT_TX, this.networkId, this.newTokenName, this.newTokenSymbol, this.newTokenSetAsDefault, this.account, this.newTokenGas, this.newTokenGasPrice, () => {
          this.$emit("list-updated", null);
          this.createNewToken = false;
        })
          .then((newTokenInstance, error) => {
            if (error) {
              throw error;
            }
            if (this.newTokenSetAsDefault && newTokenInstance.options.address && !this.error) {
              if (!newTokenInstance || !newTokenInstance.options || !newTokenInstance.options.address) {
                throw new Error("Contract deployed without a returned address");
              }
              newTokenInstance.options.address = newTokenInstance.options.address.toLowerCase();
              const contractDetails = {
                networkId: this.networkId,
                address: newTokenInstance.options.address,
                name: this.newTokenName,
                symbol: this.newTokenSymbol
              };
              // Save conract address to display for all users
              return saveContractAddressAsDefault(contractDetails)
                .then(resp => {
                  if (resp && resp.ok) {
                    this.$emit("list-updated", newTokenInstance.options.address);
                    this.loading = false;
                    this.createNewToken = false;
                  } else {
                    this.loading = false;
                    this.errorMessage = `Contract deployed, but an error occurred while saving it as default contract to display for all users`;
                    this.newTokenAddress = newTokenInstance.options.address;
                  }
                  lockBrowerWallet();
                })
                .catch(e => {
                  console.debug("saveContractAddressAsDefault method - error", e);
                  this.errorMessage = `Contract deployed, but an error occurred while saving it as default contract to display for all users: ${e}`;
                  this.loading = false;
                  this.newTokenAddress = newTokenInstance.options.address;
                  lockBrowerWallet();
                });
            } else {
              // Save contract address to display for current user only
              return saveContractAddress(this.account, newTokenInstance.options.address, this.networkId)
                .then((added, error) => {
                  if (error) {
                    throw error;
                  }
                  this.loading = false;
                  if (added) {
                    this.$emit("list-updated", newTokenInstance.options.address);
                    this.newTokenAddress = newTokenInstance.options.address;
                  } else {
                    this.errorMessage = `Error during contract address saving for all users`;
                  }
                  lockBrowerWallet();
                })
                .catch(e => {
                  lockBrowerWallet();
                  console.debug("saveContractAddress method - error", e);
                  this.loading = false;
                  this.errorMessage = `Error during contract address saving for all users: ${e}`;
                });
            }
          })
          .catch(e => {
            console.debug("saveContractAddress method - error", e);
            this.loading = false;
            this.errorMessage = `Error during contract deployment: ${e}`;
          });
      } catch(e) {
        lockBrowerWallet();
        console.debug("saveContractAddress method - error", e);
        this.loading = false;
        this.errorMessage = `Error during contract deployment: ${e}`;
      }
    }
  }
};
</script>