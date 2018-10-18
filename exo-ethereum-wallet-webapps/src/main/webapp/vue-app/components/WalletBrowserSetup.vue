<template>
  <v-flex class="text-xs-center white">
    <div id="walletBrowserSetup">
      <button v-if="!walletAddress" class="btn btn-primary" @click="createWallet()">Create new wallet</button>
      <div v-if="!walletAddress">Or</div>

      <a v-if="walletAddress" href="javascript:void(0);" @click="importWalletDialog = true;">Import my wallet in current browser</a>
      <a v-else href="javascript:void(0);" @click="importWalletDialog = true;">Import existing wallet</a>

      <div>Or</div>
      <a href="javascript:void(0);" @click="switchToMetamask">Use metamask</a>
    </div>

    <v-dialog v-model="importWalletDialog" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="importWalletDialog = false">
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
          <button class="btn btn-primary mr-1" @click="importWallet">Import</button>
          <button class="btn" @click="importWalletDialog = false">Close</button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-dialog>

  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import * as constants from '../WalletConstants.js';
import {enableMetamask, disableMetamask, initEmptyWeb3Instance, saveBrowerWallet, setWalletBackedUp} from '../WalletUtils.js';
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
      importWalletDialog: false,
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
    importWalletDialog() {
      if (this.importWalletDialog) {
        this.resetForm();
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
          saveBrowerWallet(password, phrase, address, this.autoGenerateWalletPassword, this.autoGenerateWalletPassword, !this.importWalletDialog);
          setWalletBackedUp(address, this.importWalletDialog);

          disableMetamask();

          this.importWalletDialog = false;
          this.$emit("configured");
          this.walletAddress = address;
        })
        .catch(e => {
          console.debug("saveNewAddress method - error", e);
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