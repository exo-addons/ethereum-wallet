<template>
  <v-dialog v-model="importWalletDialog" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="importWalletDialog = false">
    <a v-if="walletAddress" slot="activator" href="javascript:void(0);" @click="importWalletDialog = true;">Import my wallet in current browser</a>
    <a v-else slot="activator" href="javascript:void(0);" @click="importWalletDialog = true;">Import existing wallet</a>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="importWalletDialog = false"></a>
        <span class="PopupTitle popupTitle">Import wallet private key</span>
      </div>
      <v-card-text>
        <div v-if="errorMessage" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ errorMessage }}
        </div>
        <v-form>
          <label v-if="walletAddress" for="walletPrivateKey">Please introduce the private key of {{ walletAddress }}</label>
          <label v-else for="walletPrivateKey">This is the private key to import a new wallet address</label>
          <v-text-field
            v-model="walletPrivateKey"
            :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.priv]"
            :type="walletPrivateKeyShow ? 'text' : 'password'"
            name="walletPrivateKey"
            label="Wallet private key"
            placeholder="Input your wallet private key"
            @click:append="walletPrivateKeyShow = !walletPrivateKeyShow"
          />

          <v-switch v-model="autoGenerateWalletPassword" label="Generate an automatic password" />
          <div v-if="autoGenerateWalletPassword" class="alert alert-warning v-content">
            <i class="uiIconWarning"></i>
            It is highly recommended to use a personal password to avoid security issues.
            If you choose to automatically generate a password, it will be stored in local browser to be able to access your wallet.
          </div>

          <label v-if="!autoGenerateWalletPassword" for="walletPassword">This password will be used to encrypt your keys in current browser. You should keep it in a safe place.</label>
          <v-text-field
            v-if="!autoGenerateWalletPassword"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            name="walletPassword"
            label="Wallet Password"
            counter
            @click:append="walletPasswordShow = !walletPasswordShow"
          />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading" class="btn btn-primary mr-1" @click="importWallet">Import</button>
        <button :disabled="loading" class="btn" @click="importWalletDialog = false">Close</button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import {enableMetamask, disableMetamask, initEmptyWeb3Instance, saveBrowerWalletInstance, setWalletBackedUp, generatePassword} from '../WalletUtils.js';
import {saveNewAddress} from '../WalletAddressRegistry.js';

export default {
  components: {
    WalletAddress
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data() {
    return {
      importWalletDialog: false,
      autoGenerateWalletPassword: true,
      walletPassword: '',
      walletPasswordShow: false,
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      errorMessage: null,
      loading: false,
      rules: {
        min: v => v.length >= 8 || 'At least 8 characters',
        priv: v => v.length === 66 || v.length === 64 || 'Exactly 64 or 66 (with "0x") characters are required'
      }
    };
  },
  watch: {
    importWalletDialog() {
      if (this.importWalletDialog) {
        this.resetForm();
      }
    }
  },
  methods: {
    resetForm() {
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.autoGenerateWalletPassword = true;
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
      this.errorMessage = null;
      this.loading = false;
    },
    getPassword() {
      if (this.autoGenerateWalletPassword) {
        return generatePassword();
      } else {
        return this.walletPassword;
      }
    },
    importWallet() {
      this.loading = true;
      try {
        if (this.walletPrivateKey.indexOf("0x") < 0) {
          this.walletPrivateKey = `0x${this.walletPrivateKey}`;
        }
        const wallet = window.localWeb3.eth.accounts.wallet.add(this.walletPrivateKey);
        if (!this.walletAddress || wallet.address.toLowerCase() === this.walletAddress.toLowerCase()) {
          const password = this.getPassword();
          saveBrowerWalletInstance(wallet, password, this.isSpace, this.autoGenerateWalletPassword, true)
            .then(() => {
              this.loading = false;
              this.importWalletDialog = false;
              this.$nextTick(() => {
                this.$emit('configured');
              });
            })
            .catch(e => {
              this.loading = false;
              console.debug("saveBrowerWalletInstance method - error", e);
              this.errorMessage = `Error saving new Wallet address`;
            });
        } else {
          this.loading = false;
          this.errorMessage = `Private key doesn't match address ${this.walletAddress}`;
        }
      } catch(e) {
        this.loading = false;
        console.debug('Error importing private key', e);
        this.errorMessage = `Error saving new Wallet address`;
      }
    }
  }
};
</script>