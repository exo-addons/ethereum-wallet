<template>
  <v-card>
    <v-subheader v-if="createNewToken">Deploy new contract</v-subheader>
    <v-form v-if="createNewToken" ref="form" v-model="valid" class="pl-5 pr-5 pt-3">
      <div class="text-xs-center">
        <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
      </div>
      <v-alert :value="newTokenAddress" type="success" class="v-content">
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
      <div class="text-xs-center">
        <v-btn :disabled="!valid" color="primary" @click="saveContract">
          Deploy
        </v-btn>
        <v-btn @click="createNewToken = false">
          Close
        </v-btn>
      </div>
    </v-form>
    <div class="text-xs-center">
      <v-btn v-if="!createNewToken" color="primary" class="mt-3" @click="createNewToken = true">
        Deploy new Token
      </v-btn>
    </div>
  </v-card>
</template>

<script>
import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from '../WalletConstants.js';
import {getContractsAddresses, saveContractAddress} from '../WalletToken.js';
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
        (v) => !!v || 'Name is required'
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

      const CONTRACT_DATA = {
        contractName: 'Standard ERC20 Token',
        abi: ERC20_COMPLIANT_CONTRACT_ABI,
        bytecode: ERC20_COMPLIANT_CONTRACT_BYTECODE
      };
      const NEW_TOKEN = TruffleContract(CONTRACT_DATA);

      NEW_TOKEN.defaults({
        from: this.account,
        gas: `${this.newTokenGas}`,
        gasPrice: `${this.newTokenGasPrice}`
      });

      NEW_TOKEN.setProvider(window.localWeb3.currentProvider);

      this.loading = true;

      window.localWeb3.eth.estimateGas({data: ERC20_COMPLIANT_CONTRACT_BYTECODE})
        .then(result => {
          if (result > this.newTokenGas) {
            this.warning = `You have set a low gas ${this.newTokenGas} while the estimation of necessary gas is ${result}`;
          }
        })
        .then(() =>  NEW_TOKEN.new(this.newTokenInitialCoins, this.newTokenName, this.newTokenDecimals, this.newTokenSymbol))
        .then((newTokenInstance) => {
          this.newTokenAddress = newTokenInstance.address;
          if (this.newTokenSetAsDefault && this.newTokenAddress && !this.error) {
            fetch('/portal/rest/wallet/api/contract/save', {
              method: 'POST',
              headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                address: this.address
              })
            }).then(resp => {
              if (resp && resp.ok) {
                window.walletSettings.defaultContractsToDisplay.push(this.newTokenAddress);
                this.$emit("list-updated");
              } else {
                this.errorMessage = 'Error saving contract as default';
              }
              this.loading = false;
            });
          } else {
            saveContractAddress(this.account, this.newTokenAddress, this.networkId);
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