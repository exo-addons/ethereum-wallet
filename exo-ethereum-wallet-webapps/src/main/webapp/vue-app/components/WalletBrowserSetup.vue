<template>
  <v-flex class="text-xs-center white">
    <div id="walletBrowserSetup">
      <button v-if="!walletAddress" :disabled="loading" class="btn btn-primary" @click="createWallet()">Create new wallet</button>
      <div v-if="!walletAddress">Or</div>

      <wallet-import-key-modal
        :wallet-address="walletAddress"
        @configured="$emit('configured')" />

      <div>Or</div>
      <a href="javascript:void(0);" @click="switchToMetamask">Use metamask</a>
    </div>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import WalletImportKeyModal from './WalletImportKeyModal.vue';

import {enableMetamask, disableMetamask, initEmptyWeb3Instance, saveBrowerWalletInstance, generatePassword} from '../WalletUtils.js';

export default {
  components: {
    WalletAddress,
    WalletImportKeyModal
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    isSpaceAdministrator: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data() {
    return {
      walletAddress: null,
      loading: false,
      rules: {
        min: v => v.length >= 8 || 'At least 8 characters',
        priv: v => v.length === 66 || v.length === 64 || 'Exactly 64 or 66 (with "0x") characters are required'
      }
    };
  },
  watch: {
    refreshIndex(newValue, oldValue) {
      if (newValue > oldValue) {
        this.init();
      }
    }
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      if (!window.localWeb3) {
        initEmptyWeb3Instance();
      }
      if (window.walletSettings && window.walletSettings.userPreferences) {
        this.walletAddress = window.walletSettings.userPreferences.walletAddress;
      }
    },
    createWallet() {
      this.loading = true;

      const password = generatePassword();
      const entropy = password + Math.random();
      const wallet = window.localWeb3.eth.accounts.wallet.create(1, entropy);

      saveBrowerWalletInstance(wallet[0], password, this.isSpace, true, false)
        .then(() => {
          this.$emit("configured");
          this.loading = false;
        })
        .catch(e => {
          console.debug("saveBrowerWalletInstance method - error", e);
          this.$emit('error', 'Error saving new Wallet address');
          this.loading = false;
        });
    },
    switchToMetamask() {
      enableMetamask(this.isSpace);
      this.$emit("configured");
    }
  }
};
</script>