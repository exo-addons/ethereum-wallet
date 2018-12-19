<template>
  <v-dialog v-model="importWalletDialog" content-class="uiPopup with-overflow" width="500px" max-width="100vw" persistent @keydown.esc="importWalletDialog = false">
    <a slot="activator" href="javascript:void(0);" @click="importWalletDialog = true">{{ walletAddress ? 'Restore your wallet' : 'Restore existing wallet' }}</a>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix"><a class="uiIconClose pull-right" aria-hidden="true" @click="importWalletDialog = false"></a> <span class="PopupTitle popupTitle">Restore wallet</span></div>
      <v-card-text>
        <div v-if="error" class="alert alert-error v-content"><i class="uiIconError"></i>{{ error }}</div>
        <v-card-title v-show="loading" class="pb-0">
          <v-spacer />
          <v-progress-circular color="primary" indeterminate size="20" />
          <v-spacer />
        </v-card-title>
        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          "
        >
          <label v-if="walletAddress" for="walletPrivateKey" class="mb-3">
            Please enter the private key for the following wallet (Find your private key in Backup section): <br />
            <code>{{ walletAddress }}</code>
          </label>
          <label v-else for="walletPrivateKey">This is the private key to import a new wallet address</label>
          <v-text-field v-model="walletPrivateKey" :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'" :rules="[rules.priv]" :type="walletPrivateKeyShow ? 'text' : 'password'" :disabled="loading" name="walletPrivateKey" label="Wallet private key" placeholder="Enter your wallet private key" autocomplete="off" @click:append="walletPrivateKeyShow = !walletPrivateKeyShow" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading" class="btn btn-primary mr-1" @click="importWallet">Import</button> <button :disabled="loading" class="btn" @click="importWalletDialog = false">Close</button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import {setDraggable, enableMetamask, disableMetamask, initEmptyWeb3Instance, saveBrowerWalletInstance, setWalletBackedUp, generatePassword} from '../WalletUtils.js';
import {saveNewAddress} from '../WalletAddressRegistry.js';

export default {
  components: {
    WalletAddress,
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      importWalletDialog: false,
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      error: null,
      loading: false,
      rules: {
        min: (v) => (v && v.length >= 8) || 'At least 8 characters',
        priv: (v) => (v && (v.length === 66 || v.length === 64)) || 'Exactly 64 or 66 (with "0x") characters are required',
      },
    };
  },
  watch: {
    importWalletDialog() {
      if (this.importWalletDialog) {
        if (this.$refs.form) {
          this.$refs.form.reset();
        }
        this.resetForm();
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
  },
  methods: {
    resetForm() {
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
      this.error = null;
      this.loading = false;
    },
    importWallet() {
      this.error = null;
      this.loading = true;
      const thiss = this;
      window.setTimeout(() => {
        try {
          if (thiss.walletPrivateKey.indexOf('0x') < 0) {
            thiss.walletPrivateKey = `0x${thiss.walletPrivateKey}`;
          }
          const wallet = window.localWeb3.eth.accounts.wallet.add(thiss.walletPrivateKey);
          if (!thiss.walletAddress || wallet.address.toLowerCase() === thiss.walletAddress.toLowerCase()) {
            saveBrowerWalletInstance(wallet, generatePassword(), thiss.isSpace, true, true)
              .then(() => {
                thiss.loading = false;
                thiss.importWalletDialog = false;
                thiss.$nextTick(() => {
                  thiss.$emit('configured');
                });
              })
              .catch((e) => {
                thiss.loading = false;
                console.debug('saveBrowerWalletInstance method - error', e);
                thiss.error = `Error saving new Wallet address`;
              });
          } else {
            thiss.loading = false;
            thiss.error = `Private key doesn't match address ${thiss.walletAddress}`;
          }
        } catch (e) {
          thiss.loading = false;
          console.debug('Error importing private key', e);
          thiss.error = `Error saving new Wallet address`;
        }
      }, 200);
    },
  },
};
</script>
