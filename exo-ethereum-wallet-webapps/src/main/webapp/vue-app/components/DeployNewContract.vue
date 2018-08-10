<template>
  <v-dialog v-model="createNewToken" fullscreen hide-overlay transition="dialog-bottom-transition" persistent>
    <v-btn slot="activator" dark class="mt-3 primary" @click="createNewToken = true">
      Deploy new Token
    </v-btn>
    <v-card>
      <v-toolbar dark color="primary">
        <v-btn :disabled="loading" icon dark @click.native="createNewToken = false">
          <v-icon>close</v-icon>
        </v-btn>
        <v-toolbar-title>Deploy new ERC20 Token contract</v-toolbar-title>
        <v-spacer></v-spacer>
        <v-toolbar-items>
          <v-btn :disabled="loading" dark flat class="primary" @click.native="saveContract">Deploy</v-btn>
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

        <h4>ERC20 Token contract details</h4>
        <v-divider class="mb-4"/>
        <v-text-field v-model="newTokenName" :rules="mandatoryRule" label="Token name" required />
        <v-text-field v-model="newTokenSymbol" :rules="mandatoryRule" label="Token symbol" required></v-text-field>
        <v-slider v-model="newTokenInitialCoins"
                  :label="`Token coins supply: ${newTokenInitialCoins}`"
                  :min="0"
                  :max="1000000"
                  :step="10000"
                  type="number"
                  required />
        <v-slider v-model="newTokenDecimals"
                  :label="`Token coins decimals: ${newTokenDecimals}`"
                  :min="0"
                  :max="10"
                  :step="1"
                  type="number"
                  disabled
                  required />

        <h4>Contract creation transaction fee</h4>
        <v-divider class="mb-4"/>
        <v-slider v-model="newTokenGas"
                  :label="`Gas limit: ${newTokenGas}${newTokenGasInUSD ? ' (' + newTokenGasInUSD + ' \$)' : ''}`"
                  :min="50000"
                  :max="800000"
                  :step="1000"
                  type="number"
                  required />
        <v-slider v-model="newTokenGasPrice"
                  :label="`Gas price (Gwei): ${newTokenGasPriceGWEI}`"
                  :min="10000000000"
                  :max="60000000000"
                  :step="1000000000"
                  type="number"
                  required />

        <h4>Contract address management</h4>
        <v-divider />
        <v-list three-line>
          <v-list-tile avatar>
            <v-list-tile-action>
              <v-checkbox v-model="newTokenSetAsDefault" />
            </v-list-tile-action>
            <v-list-tile-content>
              <v-list-tile-title>Install contract as default one for all wallets</v-list-tile-title>
              <v-list-tile-sub-title>
                This will display the contract in all users wallet by default without any additional action from users. Else, the contract will be added to the current user's wallet only.
              </v-list-tile-sub-title>
            </v-list-tile-content>
          </v-list-tile>
        </v-list>
      </v-form>
    </v-card>
  </v-dialog>
</template>

<script>
import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from '../WalletConstants.js';
import {getContractsAddresses, saveContractAddress, saveContractAddressAsDefault, createNewERC20TokenContract} from '../WalletToken.js';
import {searchAddress} from '../WalletAddressRegistry.js';
import {gasToUSD} from '../WalletUtils.js';

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
      newTokenDecimals: 0,
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
      this.newTokenDecimals = 0;
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

      let NEW_TOKEN = null;
      this.loading = true;
      try {
        createNewERC20TokenContract(this.account, this.newTokenGas, this.newTokenGasPrice)
          .then((contract, error) => {
            if (error) {
              throw error;
            }
            return NEW_TOKEN = contract;
          })
          .then(() => this.loading = true)
          .then(() => window.localWeb3.eth.estimateGas({data: ERC20_COMPLIANT_CONTRACT_BYTECODE}))
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
                  console.debug("saveContractAddressAsDefault method - error", e);
                  this.errorMessage = `Contract deployed, but an error occurred while saving it as default contract to display for all users: ${e}`;
                  this.loading = false;
                  this.newTokenAddress = newTokenInstance.address;
                });
            } else {
              // Save contract address to display for current user only
              saveContractAddress(this.account, newTokenInstance.address, this.networkId)
                .then((added, error) => {
                  if (error) {
                    throw error;
                  }
                  this.loading = false;
                  if (added) {
                    this.newTokenAddress = newTokenInstance.address;
                  } else {
                    this.errorMessage = `Error during contract address saving for all users`;
                  }
                })
                .catch(e => {
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
        console.debug("saveContractAddress method - error", e);
        this.loading = false;
        this.errorMessage = `Error during contract deployment: ${e}`;
      }
    }
  }
};
</script>