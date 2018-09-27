<template>
  <v-flex class="text-xs-center white">
    <div id="walletBrowserSetup">
      <button v-if="!walletAddress" class="btn btn-primary" @click="createWalletDialog = true; dialog = true;">Create new wallet</button>
      <div v-if="!walletAddress">Or</div>

      <a v-if="walletAddress" href="javascript:void(0);" @click="importWalletDialog = true; dialog = true;">Import my wallet in current browser</a>
      <a v-else href="javascript:void(0);" @click="importWalletDialog = true; dialog = true;">Import existing wallet</a>

      <div v-if="!useMetamask">Or</div>
      <a v-if="!useMetamask" href="javascript:void(0);" @click="switchToMetamask">Use metamask</a>
    </div>
    <v-divider />

    <v-dialog v-model="dialog" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="dialog = false">
      <v-card class="elevation-12">
        <div class="popupHeader ClearFix">
          <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
          <span v-if="createWalletDialog" class="PopupTitle popupTitle">Create wallet</span>
          <span v-else class="PopupTitle popupTitle">Import wallet private key</span>
        </div>
        <v-card-text>
          <div v-if="errorMessage" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ errorMessage }}
          </div>
          <v-form>
            <label v-if="importWalletDialog && walletAddress" for="walletPrivateKey">This is the private key of account {{ walletAddress }}</label>
            <label v-else-if="importWalletDialog" for="walletPrivateKey">This is the private key to import a new wallet address</label>
            <v-text-field
              v-if="importWalletDialog"
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
          <button v-if="createWalletDialog" class="btn btn-primary mr-1" @click="createWallet">Create</button>
          <button v-else class="btn btn-primary mr-1" @click="importWallet">Import</button>
          <button class="btn" @click="dialog = false">Close</button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-dialog>

  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import * as constants from '../WalletConstants.js';
import {enableMetamask, disableMetamask, initEmptyWeb3Instance, saveBrowerWallet} from '../WalletUtils.js';
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
    useMetamask: {
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
      dialog: false,
      createWalletDialog: false,
      autoGenerateWalletPassword: true,
      walletPassword: '',
      walletPasswordShow: false,
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      walletAddress: null,
      errorMessage: null,
      constants: constants,
      rules: {
        min: v => v.length >= 8 || 'At least 8 characters',
        priv: v => v.length === 66 || v.length === 64 || 'Exactly 64 or 66 (with "0x") characters are required'
      }
    };
  },
  watch: {
    createWalletDialog() {
      if (this.createWalletDialog) {
        this.resetForm();
        this.importWalletDialog = false;
      }
    },
    importWalletDialog() {
      if (this.importWalletDialog) {
        this.resetForm();
        this.createWalletDialog = false;
      }
    },
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
    resetForm() {
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.autoGenerateWalletPassword = true;
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
      this.walletAddress = null;
      this.errorMessage = null;
    },
    init() {
      if (!window.localWeb3) {
        initEmptyWeb3Instance();
      }
      if (window.walletSettings && window.walletSettings.userPreferences) {
        this.walletAddress = window.walletSettings.userPreferences.walletAddress;
      }
    },
    getPassword() {
      if (this.autoGenerateWalletPassword) {
        return Math.random().toString(36).slice(2);
      } else {
        return this.walletPassword;
      }
    },
    createWallet() {
      const password = this.getPassword();
      const entropy = password + Math.random();
      const wallet = window.localWeb3.eth.accounts.wallet.create(1, entropy);
      this.saveWallet(wallet[0], password);
    },
    importWallet() {
      if (this.walletPrivateKey.indexOf("0x") < 0) {
        this.walletPrivateKey = `0x${this.walletPrivateKey}`;
      }
      const wallet = window.localWeb3.eth.accounts.wallet.add(this.walletPrivateKey);
      if (!this.walletAddress || wallet.address.toLowerCase() === this.walletAddress.toLowerCase()) {
        const password = this.getPassword();
        this.saveWallet(wallet, password);
      } else {
        this.errorMessage = `Private key doesn't match address ${this.walletAddress}`;
      }
    },
    saveWallet(wallet, password) {
      const account = window.localWeb3.eth.accounts.wallet.add(wallet);
      const address = account['address'].toLowerCase();

      return saveNewAddress(
        this.isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName,
        this.isSpace ? 'space' : 'user',
        address,
        true)
        .then((resp, error) => {
          if (error) {
            throw error;
          }
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving new Wallet address');
          }
        })
        .then((phrase, error) => {
          saveBrowerWallet(password, phrase, address, this.autoGenerateWalletPassword, this.autoGenerateWalletPassword, this.createWalletDialog);

          disableMetamask();

          this.dialog = false;

          this.$emit("configured");

          this.walletAddress = address;
        })
        .catch(e => {
          console.debug("saveNewAddress method - error", e);
          window.localWeb3.eth.accounts.wallet.remove(address);
          localStorage.removeItem(`exo-wallet-${address}-userp`);
          this.errorMessage = `Error saving new Wallet address`;
        });
    },
    switchToMetamask() {
      enableMetamask(this.isSpace);
      this.$emit("configured");
    }
  }
};
</script>