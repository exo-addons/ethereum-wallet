<template>
  <v-card class="text-xs-center pr-3 pl-3 pt-2" flat>
    <div><v-switch v-model="enableDelegation" label="Enable token delegation operations for wallets by default"></v-switch></div>

    <v-slider v-model="defaultGas" :label="`Maximum gas: ${defaultGas}`" :min="35000" :max="200000" :step="1000" type="number" class="mt-4" required />

    <v-slider v-model="minGasPrice" :label="`Cheap transaction gas price: ${minGasPriceGwei} Gwei ${minGasFiatPrice}${minGasPriceToken}`" :min="1000000000" :max="20000000000" :step="1000000000" type="number" class="mt-4" required />

    <v-slider v-model="normalGasPrice" :label="`Normal transaction gas price: ${normalGasPriceGwei} Gwei ${normalGasFiatPrice}${normalGasPriceToken}`" :min="1000000000" :max="20000000000" :step="1000000000" type="number" class="mt-4" required />

    <v-slider v-model="maxGasPrice" :label="` Fast transaction gas price: ${maxGasPriceGwei} Gwei ${maxGasFiatPrice}${maxGasPriceToken}`" :min="1000000000" :max="20000000000" :step="1000000000" type="number" class="mt-4" required />

    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">Save</button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
import {gasToFiat} from '../WalletUtils.js';

export default {
  props: {
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
      minGasPrice: 4000000000,
      normalGasPrice: 8000000000,
      maxGasPrice: 15000000000,
      enableDelegation: true,
    };
  },
  computed: {
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
    },
    save() {
      const globalSettings = {
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
