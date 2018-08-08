<template>
  <v-dialog v-model="createNewToken" fullscreen hide-overlay transition="dialog-bottom-transition">
    <v-btn slot="activator" dark class="mt-3 primary" @click="createNewToken = true">
      Deploy new Token
    </v-btn>
    <v-card>
      <v-toolbar dark color="primary">
        <v-btn icon dark @click.native="createNewToken = false">
          <v-icon>close</v-icon>
        </v-btn>
        <v-toolbar-title>Deploy new ERC20 Token contract</v-toolbar-title>
        <v-spacer></v-spacer>
        <v-toolbar-items>
          <v-btn dark flat class="primary" @click.native="saveContract">Deploy</v-btn>
        </v-toolbar-items>
      </v-toolbar>

      <v-form ref="form" v-model="valid" class="pl-5 pr-5 pt-3">
        <div class="text-xs-center">
          <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
        </div>
        <v-alert :value="newTokenAddress" type="success" class="v-content" dismissible>
          Contract created under address: 
          <code>{{ newTokenAddress }}</code>
        </v-alert>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-alert v-show="!error && warning && warning.length" :value="warning" type="warning" class="v-content">
          {{ warning }}
        </v-alert>
        <v-text-field v-model="newTokenName" :rules="mandatoryRule" label="Token name" required></v-text-field>
        <v-text-field v-model="newTokenSymbol" :rules="mandatoryRule" label="Token symbol" required></v-text-field>
        <span>Default gas to spend on transactions (Maximum fee per transaction)</span>
        <v-slider v-model="newTokenGas"
                  :label="`Contract deployment Gas: ${newTokenGas}${newTokenGasInUSD ? ' (' + newTokenGasInUSD + ' \$)' : ''}`"
                  :min="50000"
                  :max="800000"
                  :step="1000"
                  type="number"
                  required />
        <v-slider v-model="newTokenGasPrice"
                  :label="`Contract deployment Gas Price (GWei): ${newTokenGasPriceGWEI}`"
                  :min="10000000000"
                  :max="60000000000"
                  :step="1000000000"
                  type="number"
                  required />
        <v-slider v-model="newTokenInitialCoins"
                  :label="`Initial total tokens: ${newTokenInitialCoins}`"
                  :min="0"
                  :max="1000000"
                  :step="10000"
                  type="number"
                  required />
        <v-slider v-model="newTokenDecimals"
                  :label="`Token decimals: ${newTokenDecimals}`"
                  :min="0"
                  :max="10"
                  :step="1"
                  type="number"
                  required />
        <v-checkbox v-model="newTokenSetAsDefault" label="Install contract as default one for all wallets?"></v-checkbox>
      </v-form>
    </v-card>
  </v-dialog>
</template>

<script>
import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from '../WalletConstants.js';
import {getContractsAddresses, saveContractAddress, saveContractAddressAsDefault, createNewERC20TokenContract} from '../WalletToken.js';
import {searchAddress} from '../WalletAddressRegistry.js';
import {gasToUSD,initWeb3,initSettings,retrieveUSDExchangeRate} from '../WalletUtils.js';

export default {
  props: {
    account: {
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
      newTokenName: '',
      newTokenSymbol: '',
      newTokenGas: 700000,
      newTokenGasPrice: 20000000000,
      newTokenGasPriceGWEI: 20,
      newTokenGasInUSD: 0,
      newTokenDecimals: 8,
      newTokenInitialCoins: 10000,
      newTokenSetAsDefault: true,
      newTokenAddress: '',
      createNewToken: false,
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
      this.calculateGasPriceInUSD();
    },
    newTokenGasPrice() {
      this.calculateGasPriceInUSD();
      this.newTokenGasPriceGWEI = window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'gwei');
    },
    createNewToken() {
      this.resetContractForm();
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
      this.newTokenGasPrice = 20000000000;
      this.newTokenGasPriceGWEI = 20;
      this.newTokenGasInUSD = 0;
      this.newTokenDecimals = 8;
      this.newTokenInitialCoins = 10000;
      this.newTokenSetAsDefault = true;
      this.newTokenAddress = '';
      this.valid = false;
      this.contracts = [];
    },
    calculateGasPriceInUSD() {
      if (this.newTokenGas && this.newTokenGasPrice) {
        const gasPriceInEther = window.localWeb3.utils.fromWei(this.newTokenGasPrice.toString(), 'ether');
        this.newTokenGasInUSD = gasToUSD(this.newTokenGas, gasPriceInEther);
      } else {
        this.newTokenGasInUSD = 0;
      }      
    },
    saveContract() {
      this.errorMessage = null;
      this.warning = null;

      if(!this.$refs.form.validate()) {
        return;
      }

      const NEW_TOKEN = createNewERC20TokenContract(this.account, this.newTokenGas, this.newTokenGasPrice);

      this.loading = true;

      window.localWeb3.eth.estimateGas({data: ERC20_COMPLIANT_CONTRACT_BYTECODE})
        .then(result => {
          if (result > this.newTokenGas) {
            this.warning = `You have set a low gas ${this.newTokenGas} while the estimation of necessary gas is ${result}`;
          }
        })
        .then(() =>  NEW_TOKEN.new(this.newTokenInitialCoins, this.newTokenName, this.newTokenDecimals, this.newTokenSymbol))
        .then((newTokenInstance) => {
          if (this.newTokenSetAsDefault && newTokenInstance.address && !this.error) {
            if (!newTokenInstance || !newTokenInstance.address) {
              throw new Error("Contract deployed without a returned address");
            }
            newTokenInstance.address = newTokenInstance.address.toLowerCase();
            // Save conract address to display for all users
            saveContractAddressAsDefault(newTokenInstance.address)
              .then(resp => {
                if (resp && resp.ok) {
                  this.$emit("list-updated", newTokenInstance.address);
                  this.createNewToken = false;
                } else {
                  this.loading = false;
                  this.errorMessage = `Contract deployed, but an error occurred while saving it as default contract to display for all users`;
                  this.newTokenAddress = newTokenInstance.address;
                }
              })
              .catch(e => {
                this.errorMessage = `Contract deployed, but an error occurred while saving it as default contract to display for all users: ${e}`;
                this.loading = false;
                this.newTokenAddress = newTokenInstance.address;
              });
          } else {
            // Save conract address to display for current user only
            saveContractAddress(this.account, newTokenInstance.address, this.networkId);
            this.newTokenAddress = newTokenInstance.address;
            this.loading = false;
          }
        })
        .catch(e => {
          this.loading = false;
          this.errorMessage = `Error during contract deployment: ${e}`;
        });
    }
  }
};
</script>