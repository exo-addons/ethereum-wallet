<template>
  <v-card class="text-xs-center pr-3 pl-3 pt-3" flat>
    <v-form ref="form">
      <v-flex id="selectedNetworkParent" class="selectBoxVuetifyParent">
        <v-combobox
          v-model="selectedNetwork"
          :items="networks"
          attach="#selectedNetworkParent"
          label="Select ethereum network" />
      </v-flex>
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        v-model="selectedNetwork.value"
        :rules="mandatoryRule"
        :label="`Ethereum Network ID (current id: ${networkId})`"
        type="number"
        name="defaultNetworkId" />
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        ref="providerURL"
        v-model="selectedNetwork.httpLink"
        :rules="mandatoryRule"
        type="text"
        name="providerURL"
        label="Ethereum Network HTTP URL used for static displaying spaces wallets (without Metamask)"
        autofocus />
  
      <v-text-field
        v-if="showSpecificNetworkFields"
        ref="websocketProviderURL"
        v-model="selectedNetwork.wsLink"
        type="text"
        name="websocketProviderURL"
        label="Ethereum Network Websocket URL used for notifications" />
  
      <v-slider
        v-if="sameConfiguredNetwork"
        v-model="defaultGas"
        :label="`Maximum gas: ${defaultGas}`"
        :min="35000"
        :max="200000"
        :step="1000"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-if="sameConfiguredNetwork"
        v-model="minGasPrice"
        :label="`Cheap transaction gas price: ${minGasPriceGwei} Gwei ${minGasFiatPrice}${minGasPriceToken}`"
        :min="1000000000"
        :max="20000000000"
        :step="1000000000"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-if="sameConfiguredNetwork"
        v-model="normalGasPrice"
        :label="`Normal transaction gas price: ${normalGasPriceGwei} Gwei ${normalGasFiatPrice}${normalGasPriceToken}`"
        :min="1000000000"
        :max="20000000000"
        :step="1000000000"
        type="number"
        class="mt-4"
        required />
  
      <v-slider
        v-if="sameConfiguredNetwork"
        v-model="maxGasPrice"
        :label="` Fast transaction gas price: ${maxGasPriceGwei} Gwei ${maxGasFiatPrice}${maxGasPriceToken}`"
        :min="1000000000"
        :max="20000000000"
        :step="1000000000"
        type="number"
        class="mt-4"
        required />
    </v-form>

    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
import {gasToFiat} from '../../WalletUtils.js';

export default {
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultNetworkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    sameConfiguredNetwork: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    principalContract: {
      type: Object,
      default: function() {
        return null;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      defaultGas: 50000,
      minGasPrice: null,
      normalGasPrice: null,
      maxGasPrice: null,
      enableDelegation: true,
      mandatoryRule: [(v) => !!v || 'Field is required'],
      selectedNetwork: {
        text: '',
        value: '',
      },
      networks: [
        {
          text: 'Ethereum Main Network',
          value: 1,
          wsLink: 'wss://mainnet.infura.io/ws',
          httpLink: 'https://mainnet.infura.io',
        },
        {
          text: 'Ropsten',
          value: 3,
          wsLink: 'wss://ropsten.infura.io/ws',
          httpLink: 'https://ropsten.infura.io',
        },
        {
          text: 'Other',
          value: 0,
          wsLink: 'ws://127.0.0.1:8546',
          httpLink: 'http://127.0.0.1:8545',
        },
      ],
    };
  },
  computed: {
    showSpecificNetworkFields() {
      return this.selectedNetwork && this.selectedNetwork.value !== 1 && this.selectedNetwork.value !== 3;
    },
    minGasPriceToken() {
      if (this.defaultGas && this.minGasPrice && this.principalContract && this.principalContract.isContract && this.principalContract.sellPrice) {
        const amount = (this.minGasPriceEther * this.defaultGas) / this.principalContract.sellPrice;
        return amount ? ` / ${this.toFixed(amount)} ${this.principalContract && this.principalContract.symbol}` : '';
      }
      return '';
    },
    minGasPriceEther() {
      return this.minGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.minGasPrice), 'ether').toString();
    },
    minGasPriceGwei() {
      return this.minGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.minGasPrice), 'gwei').toString();
    },
    minGasFiatPrice() {
      const amount = this.defaultGas && this.minGasPriceEther && gasToFiat(this.defaultGas, this.minGasPriceEther);
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
    normalGasPriceToken() {
      if (this.defaultGas && this.normalGasPrice && this.principalContract && this.principalContract.isContract && this.principalContract.sellPrice) {
        const amount = (this.normalGasPriceEther * this.defaultGas) / this.principalContract.sellPrice;
        return amount ? ` / ${this.toFixed(amount)} ${this.principalContract && this.principalContract.symbol}` : '';
      }
      return '';
    },
    normalGasPriceEther() {
      return this.normalGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.normalGasPrice), 'ether').toString();
    },
    normalGasPriceGwei() {
      return this.normalGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.normalGasPrice), 'gwei').toString();
    },
    normalGasFiatPrice() {
      const amount = this.defaultGas && this.normalGasPriceEther && gasToFiat(this.defaultGas, this.normalGasPriceEther);
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
    maxGasPriceToken() {
      if (this.defaultGas && this.maxGasPrice && this.principalContract && this.principalContract.isContract && this.principalContract.sellPrice) {
        const amount = (this.maxGasPriceEther * this.defaultGas) / this.principalContract.sellPrice;
        return amount ? ` / ${this.toFixed(amount)} ${this.principalContract && this.principalContract.symbol}` : '';
      }
      return '';
    },
    maxGasPriceEther() {
      return this.maxGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.maxGasPrice), 'ether').toString();
    },
    maxGasPriceGwei() {
      return this.maxGasPrice && window.localWeb3 && window.localWeb3.utils.fromWei(String(this.maxGasPrice), 'gwei').toString();
    },
    maxGasFiatPrice() {
      const amount = this.defaultGas && this.maxGasPriceEther && gasToFiat(this.defaultGas, this.maxGasPriceEther);
      return amount ? `${this.toFixed(amount)} ${this.fiatSymbol}` : '';
    },
  },
  methods: {
    init() {
      if (window.walletSettings.defaultGas) {
        this.defaultGas = window.walletSettings.defaultGas;
      }
      if (window.walletSettings.minGasPrice) {
        this.minGasPrice = window.walletSettings.minGasPrice;
      }
      if (window.walletSettings.normalGasPrice) {
        this.normalGasPrice = window.walletSettings.normalGasPrice;
      }
      if (window.walletSettings.maxGasPrice) {
        this.maxGasPrice = window.walletSettings.maxGasPrice;
      }
      this.enableDelegation = window.walletSettings.enableDelegation;
      if (window.walletSettings.defaultNetworkId === 1) {
        this.selectedNetwork = this.networks[0];
      } else if (window.walletSettings.defaultNetworkId === 3) {
        this.selectedNetwork = this.networks[1];
      } else {
        this.networks[2].value = window.walletSettings.defaultNetworkId;
        this.networks[2].wsLink = window.walletSettings.websocketProviderURL;
        this.networks[2].httpLink = window.walletSettings.providerURL;
        this.selectedNetwork = this.networks[2];
      }
    },
    save() {
      if (!this.$refs.form.validate()) {
        return;
      }
      const globalSettings = {
        providerURL: this.selectedNetwork.httpLink,
        websocketProviderURL: this.selectedNetwork.wsLink,
        defaultNetworkId: this.selectedNetwork.value,
        defaultGas: this.defaultGas,
        minGasPrice: this.minGasPrice,
        normalGasPrice: this.normalGasPrice,
        maxGasPrice: this.maxGasPrice,
        enableDelegation: this.enableDelegation,
      };
      this.$emit('save', globalSettings);
    },
  },
};
</script>
