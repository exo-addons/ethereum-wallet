<template>
  <v-app>
    <main>
      <v-layout row>
        <v-flex xs12 sm6 offset-sm3>
          <v-card class="elevation-12">
            <v-toolbar dark color="primary">
              <v-toolbar-title>Wallet application administration</v-toolbar-title>
            </v-toolbar>
            <v-alert :value="error" type="error" class="v-content">
              {{ error }}
            </v-alert>
            <v-form v-if="!error && createNewContract" ref="form" v-model="valid" lazy-validation xs12>
              <v-text-field v-model="newContractName" :counter="10" label="Token name" required></v-text-field>
              <v-text-field v-model="newContractSymbol" :counter="10" label="Token symbol" required></v-text-field>
              <span>Default gas to spend on transactions (Maximum fee per transaction)</span>
              <v-slider v-model="newContractGas"
                        :label="`Decimals: ${newContractGas}${newContractGasInUSD ? ' (' + newContractGasInUSD + ' \$)' : ''}`"
                        :max="90000"
                        :min="21000"
                        :step="1000"
                        type="number"
                        required />
              <v-slider v-model="newContractInitialCoins"
                        :label="`Initial total tokens: ${newContractInitialCoins}`"
                        :min="0"
                        :max="1000000"
                        :step="1000"
                        type="number"
                        required />
              <v-slider v-model="newContractDecimals"
                        :label="`Token decimals: ${newContractDecimals}`"
                        :max="10"
                        :min="0"
                        :step="1"
                        type="number"
                        required />
              <v-checkbox v-model="newContractSetAsDefault" label="Install contract as default one for all wallets?"></v-checkbox>
              <v-btn :disabled="!valid" color="primary" @click="saveContract">
                Deploy
              </v-btn>
              <v-btn @click="createNewContract = false">
                Cancel
              </v-btn>
            </v-form>
            <v-btn v-if="!error && !createNewContract" @click="createNewContract = true">
              Deploy new Token
            </v-btn>
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
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsAddresses} from '../WalletToken.js';
import {searchAddress} from '../WalletAddressRegistry.js';
import {gasToUSD,initWeb3,initSettings} from '../WalletUtils.js';

export default {
  data () {
    return {
      error: '',
      newContractName: '',
      newContractSymbol: '',
      newContractGas: 21000,
      newContractGasInUSD: 0,
      newContractDecimals: 0,
      newContractInitialCoins: 0,
      newContractSetAsDefault: true,
      createNewContract: false,
      valid: false,
      contracts: []
    };
  },
  watch: {
    newContractGas() {
      this.newContractGasInUSD = gasToUSD(this.defaultGas);
    }
  },
  created() {
    try {
      initSettings().then(initWeb3);

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
            this.error = 'Error getting contracts';
          }
        })
        .then(contracts => this.contracts = contracts);
    } catch(e) {
      this.error = `An error encouterd: ${e}`;
    }
  },
  methods: {
    resetContractForm() {
      this.newContractName = '';
      this.newContractSymbol = '';
      this.newContractGas = 0;
      this.newContractSetAsDefault = true;
    },
    saveContract() {
      this.error = null;

      const newContractInstance = window.localWeb3.eth.Contract(ERC20_COMPLIANT_CONTRACT_ABI);
      newContractInstance.options = {
        from: window.localWeb3.eth.defaultAccount,
        gas: `${this.newContractGas}`
      };
      let newContractAddress;
      newContractInstance.deploy({
        data: '',// TODO byte code
        arguments: [this.newContractInitialCoins, this.newContractName, this.newContractDecimals, this.newContractSymbol]
      })
        .send({
          from: window.localWeb3.eth.defaultAccount,
          gas: this.newContractGas
        })
        .on('transactionHash', (transactionHash) => {
          // loading
        })
        .on('confirmation', (confirmationNumber, receipt) => {
          console.log(receipt);
        })
        .on('error', function(error){
          this.error = `An error encouterd: ${error}`;
        })
        .then((newContractInstance) => {
          newContractAddress = newContractInstance.options.address;
          console.log(`newly created address : ${newContractAddress}`);
          if (this.newContractSetAsDefault && newContractAddress && !this.error) {
            fetch('/portal/rest/wallet/api/contract/save', {
              method: 'POST',
              headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                defaultGas: this.defaultGas
              })
            }).then(resp => {
              if (resp && resp.ok) {
                window.walletSettings.defaultGas = this.defaultGas;
                this.$emit('settings-changed', {
                  defaultGas: this.defaultGas
                });
                this.show = false;
              } else {
                this.error = 'Error saving preferences';
              }
            });
          }
        });
    }
  }
};
</script>