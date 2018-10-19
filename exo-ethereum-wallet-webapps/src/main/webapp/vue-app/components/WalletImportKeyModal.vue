<template>
  <v-dialog v-model="importWalletDialog" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="importWalletDialog = false">
    <a slot="activator" href="javascript:void(0);" @click="importWalletDialog = true;">{{ walletAddress ? 'Restore your wallet' : 'Restore existing wallet' }}</a>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="importWalletDialog = false"></a>
        <span class="PopupTitle popupTitle">Restore wallet</span>
      </div>
      <v-card-text>
        <div v-if="errorMessage" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ errorMessage }}
        </div>
        <v-form>
          <label v-if="walletAddress" for="walletPrivateKey" class="mb-3">
            Please enter the private key for the following wallet (Find your private key in Backup section):
            <br />
            <code>{{ walletAddress }}</code>
          </label>
          <label v-else for="walletPrivateKey">This is the private key to import a new wallet address</label>
          <v-text-field
            v-model="walletPrivateKey"
            :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.priv]"
            :type="walletPrivateKeyShow ? 'text' : 'password'"
            name="walletPrivateKey"
            label="Wallet private key"
            placeholder="Enter your wallet private key"
            @click:append="walletPrivateKeyShow = !walletPrivateKeyShow"
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
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
      this.errorMessage = null;
      this.loading = false;
    },
    importWallet() {
      this.errorMessage = null;
      this.loading = true;
      try {
        if (this.walletPrivateKey.indexOf("0x") < 0) {
          this.walletPrivateKey = `0x${this.walletPrivateKey}`;
        }
        const wallet = window.localWeb3.eth.accounts.wallet.add(this.walletPrivateKey);
        if (!this.walletAddress || wallet.address.toLowerCase() === this.walletAddress.toLowerCase()) {
          saveBrowerWalletInstance(wallet, generatePassword(), this.isSpace, true, true)
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