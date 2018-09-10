<template>
  <v-flex>

    <div v-if="errorCode === constants.ERROR_WALLET_ADDRESS_NOT_CONFIGURED || errorCode === constants.ERROR_WALLET_CONFIGURED_ADDRESS_NOT_FOUND">
      <div v-if="errorCode === constants.ERROR_WALLET_CONFIGURED_ADDRESS_NOT_FOUND" class="alert alert-warning v-content">
        <i class="uiIconWarning"></i>
        Wallet address <wallet-address :value="walletAddress" /> was configured for your account but the private key wasn't found.
        You can either use Metamask, import a private key or create new wallet.
      </div>
      <button class="btn btn-primary" @click="createWalletDialog = true">Create Wallet</button>
      <div>Or</div>
      <a href="#" @click="importWalletDialog = true">Import wallet</a>
      <div>Or</div>
      <a v-if="!useMatamaskForCurrentUser" href="#" @click="useMetamask">Use metamask</a>
      <div v-else class="alert alert-info">
        <i class="uiIconInfo"></i>
        To be able to access your account using Metamask, you should enable/install it and connect into it
      </div>
    </div>

    <v-dialog v-model="createWalletDialog" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="createWalletDialog = false">
      <v-card class="elevation-12">
        <div class="popupHeader ClearFix">
          <a class="uiIconClose pull-right" aria-hidden="true" @click="createWalletDialog = false"></a>
          <span class="PopupTitle popupTitle">Create new wallet</span>
        </div>
        <v-card-text v-if="walletCreated">
          <div v-if="newTokenAddress" class="alert alert-success v-content">
            <i class="uiIconSuccess"></i>
            Wallet created with address: 
            <wallet-address :value="walletAddress" />
          </div>
        </v-card-text>
        <v-card-text v-else>
          <div v-if="errorMessage" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ errorMessage }}
          </div>
          <label for="createWalletPassword">This password will be used to encrypt your keys in current browser. You 'll have to keep it safe</label>
          <v-text-field
            v-model="createWalletPassword"
            :append-icon="createWalletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.required, rules.min]"
            :type="createWalletPasswordShow ? 'text' : 'password'"
            name="createWalletPassword"
            label="Wallet Password"
            counter
            @click:append="createWalletPasswordShow = !createWalletPasswordShow"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button class="btn btn-primary mr-1" @click="createWallet">Create</button>
          <button class="btn" @click="createWalletDialog = false">Close</button>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="importWalletDialog" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="importWalletDialog = false">
      <v-card class="elevation-12">
        <div class="popupHeader ClearFix">
          <a class="uiIconClose pull-right" aria-hidden="true" @click="importWalletDialog = false"></a>
          <span class="PopupTitle popupTitle">Import wallet private key</span>
        </div>
        <v-card-text>
          <div v-if="errorMessage" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ errorMessage }}
          </div>
          <label v-if="walletAddress" for="walletPrivateKey">This is the private key of account {{ walletAddress }}</label>
          <label v-else for="walletPrivateKey">This is the private key to import a new wallet address</label>
          <v-text-field
            v-model="walletPrivateKey"
            :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.required, rules.equals66]"
            :type="walletPrivateKeyShow ? 'text' : 'password'"
            name="walletPrivateKey"
            label="Wallet private key"
            counter
            @click:append="walletPrivateKeyShow = !walletPrivateKeyShow"
          />
          <label for="importWalletPassword">This password will be used to encrypt your keys in current browser. You 'll have to keep it safe</label>
          <v-text-field
            v-model="createWalletPassword"
            :append-icon="createWalletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.required, rules.min]"
            :type="createWalletPasswordShow ? 'text' : 'password'"
            name="importWalletPassword"
            label="Wallet Password"
            counter
            @click:append="createWalletPasswordShow = !createWalletPasswordShow"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button class="btn btn-primary mr-1" @click="importWallet">Import</button>
          <button class="btn" @click="importWalletDialog = false">Close</button>
        </v-card-actions>
      </v-card>
    </v-dialog>

  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import * as constants from '../WalletConstants.js';
import {enableMetamask, initEmptyWeb3Instance} from '../WalletUtils.js';
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
    errorCode: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data() {
    return {
      useMatamaskForCurrentUser: false,
      createWalletDialog: false,
      createWalletPassword: '',
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      createWalletPasswordShow: false,
      walletAddress: null,
      walletCreated: false,
      errorMessage: null,
      importWalletDialog: false,
      constants: constants,
      rules: {
        required: value => !!value || 'Required.',
        min: v => v.length >= 8 || 'At least 8 characters',
        equals66: v => v.length === 66 || 'Exactly 66 characters are required'
      }
    };
  },
  computed: {
  },
  watch: {
    errorCode() {
      if (this.errorCode && this.errorCode.length) {
        this.init();
      }
    },
    createWalletDialog() {
      if (this.createWalletDialog) {
        this.resetForm();
      }
    },
    importWalletDialog() {
      if (this.importWalletDialog) {
        this.resetForm();
      }
    }
  },
  created() {
    this.init();
  },
  methods: {
    resetForm() {
      this.useMatamaskForCurrentUser = false;
      this.createWalletPassword = '';
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
      this.createWalletPasswordShow = false;
      this.walletAddress = null;
      this.walletCreated = false;
      this.errorMessage = null;
    },
    init() {
      if (!window.localWeb3) {
        initEmptyWeb3Instance();
      }
      this.walletAddress = window.walletSettings.userPreferences.walletAddress;
      if (!window.walletSettings || !window.walletSettings.userPreferences) {
        // TODO
      } else {
        this.useMatamaskForCurrentUser = window.walletSettings.userPreferences.useMetamask;
      }
    },
    createWallet() {
      const wallet = window.localWeb3.eth.accounts.wallet.create(1, this.createWalletPassword + Math.random());
      this.saveWallet(wallet[0]);
    },
    importWallet() {
      const wallet = window.localWeb3.eth.accounts.wallet.add(this.walletPrivateKey);
      if (!this.walletAddress || wallet.address.toLowerCase() === this.walletAddress.toLowerCase()) {
        this.saveWallet(wallet);
      } else {
        this.errorMessage = `Private key doesn't match address ${this.walletAddress}`;
      }
    },
    saveWallet(wallet) {
      const account = window.localWeb3.eth.accounts.wallet.add(wallet);
      const address = account['address'].toLowerCase();
      window.localWeb3.eth.accounts.wallet.save(this.createWalletPassword, address);

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
            this.walletAddress = address;
            this.$emit("configured");
          } else {
            this.errorMessage = 'Error saving new Wallet address';
          }
        })
        .catch(e => {
          console.debug("saveNewAddress method - error", e);
          this.errorMessage = `Error saving new Wallet address`;
          window.localWeb3.eth.accounts.wallet.remove(address);
        });
    },
    useMetamask() {
      enableMetamask();
      this.useMatamaskForCurrentUser = true;
      this.$emit("configured");
    }
  }
};
</script>