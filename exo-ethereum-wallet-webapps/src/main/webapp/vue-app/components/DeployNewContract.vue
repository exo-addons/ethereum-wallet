<template>
  <v-card>
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
    <v-form v-if="createnewToken" ref="form" v-model="valid" class="pl-5 pr-5 pt-3">
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
        <v-btn @click="createnewToken = false">
          Cancel
        </v-btn>
      </div>
    </v-form>
    <div class="text-xs-center">
      <v-btn v-if="!error && !createnewToken" color="primary" class="mt-3" @click="createnewToken = true">
        Deploy new Token
      </v-btn>
    </div>
    <v-list v-if="!error" two-lines>
      <template v-for="(contract) in contracts">
        <v-list-tile :key="contract.symbol">
          <v-list-tile-content>
            <v-list-tile-title v-html="contract.name"></v-list-tile-title>
            <v-list-tile-sub-title v-html="contract.symbol"></v-list-tile-sub-title>
          </v-list-tile-content>
        </v-list-tile>
      </template>
    </v-list>
  </v-card>
</template>

<script>
import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from '../WalletConstants.js';
import {getContractsAddresses, saveContractAddress} from '../WalletToken.js';
import {searchAddress} from '../WalletAddressRegistry.js';
import {gasToUSD,initWeb3,initSettings,retrieveUSDExchangeRate} from '../WalletUtils.js';

export default {
  data () {
    return {
      loading: false,
      errorMessage: '',
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
      networkId: null,
      createnewToken: false,
      account: false,
      mandatoryRule: [
        (v) => !!v || 'Name is required'
      ],
      valid: false,
      contracts: []
    };
  },
  computed: {
    metamaskEnabled () {
      return web3 && web3.currentProvider && web3.currentProvider.isMetaMask;
    },
    metamaskConnected () {
      return this.metamaskEnabled && web3.currentProvider.isConnected();
    },
    error() {
      if(this.loading) {
        return null;
      } else if (this.errorMessage) {
        return this.errorMessage;
      } else if (!this.metamaskEnabled) {
        return 'Please install or Enable Metamask';
      } else if (!this.metamaskConnected) {
        return 'Please connect Metamask to a network';
      } else if (!this.account) {
        return 'Please select a valid account using Metamask';
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
    }
  },
  created() {
    try {
      initSettings()
        .then(initWeb3)
        .then(account => this.account = window.localWeb3.eth.defaultAccount)
        .then(this.computeNetwork)
        .then(retrieveUSDExchangeRate)
        .then(() => this.newTokenGasInUSD = gasToUSD(this.newTokenGas))
        .catch(e => this.errorMessage = `Error encountered: ${e}`);

      fetch('/portal/rest/wallet/api/contract', {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            this.errorMessage = 'Error getting contracts';
          }
        })
        .then(contracts => this.contracts = contracts);
    } catch(e) {
      this.errorMessage = `An error encouterd: ${e}`;
    }
  },
  methods: {
    computeNetwork() {
      return window.localWeb3.eth.net.getId()
        .then(netId => this.networkId = netId);
    },
    resetContractForm() {
      this.newTokenName = '';
      this.newTokenSymbol = '';
      this.newTokenGas = 21000;
      this.newTokenGasPrice = window.walletSettings.gasPrice;
      this.newTokenSetAsDefault = true;
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
      if(!this.$refs.form.validate()) {
        return;
      }
      this.errorMessage = null;

      const newTokenInstance = TruffleContract({
        contractName: 'Standard ERC20 Token',
        abi: ERC20_COMPLIANT_CONTRACT_ABI,
        bytecode: ERC20_COMPLIANT_CONTRACT_BYTECODE
      });

      newTokenInstance.defaults({
        from: this.account,
        gas: `${this.newTokenGas}`,
        gasPrice: `${this.newTokenGasPrice}`
      });

      newTokenInstance.setProvider(window.localWeb3.currentProvider);

      this.loading = true;

      newTokenInstance.new(this.newTokenInitialCoins, this.newTokenName, this.newTokenDecimals, this.newTokenSymbol)
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
                saveContractAddress(this.account, this.newTokenAddress, this.networkId);
              } else {
                this.errorMessage = 'Error saving contract as default';
              }
              this.loading = false;
            });
          } else {
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