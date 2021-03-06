<template>
  <v-dialog
    id="walletBackupModal" 
    v-model="dialog"
    :disabled="disabled"
    attach="#walletDialogsParent"
    class="fixLinkHeight"
    content-class="uiPopup with-overflow"
    width="500px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <a
      slot="activator"
      href="javascript:void(0);"
      @click="dialog = true">
      Backup your wallet
    </a>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix" draggable="true">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Backup wallet
          </span>
      </div>
      <v-card-text>
        <div v-if="error" class="alert alert-error">
          <i class="uiIconError"></i> {{ error }}
        </div>

        <div
          v-if="displayCompleteMessage"
          id="walletBackupDetailedWarning"
          class="alert alert-warning v-content">
          <p>
            <i class="uiIconWarning"></i> Currently your wallet is accessible only on this browser and only on this machine. <br>
            You should copy the following private key to be able to: <br>
            - restore your wallet in other devices/browsers <br>
            - and/or restore your wallet in case you can't access your browser anymore (OS recovery, machine disk formatted, browser uninstalled ...)
          </p>
          <p>
            Please make sure to keep your wallet in a safe place where nobody else can get it (Write it in a piece of paper and hide it for example). <br>
            If the following code is hacked by someone, <strong>
              he will be able to own all your funds
            </strong>.
          </p>
        </div>
        <div 
          v-else 
          id="walletBackupSimpleWarning"
          class="alert alert-warning v-content">
          <p>
            <i class="uiIconWarning"></i> Please make sure to keep this private key in a safe place where nobody else can get it (Write it in a piece of paper and hide it for example). <br>
            If the following code is hacked by someone, <strong>
              he will be able to own all your funds
            </strong>.
          </p>
        </div>

        <v-form
          ref="form"
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-if="dialog && !autoGeneratedPassword && !walletPrivateKey"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            name="walletPassword"
            label="Current wallet password"
            placeholder="Enter your current wallet password"
            counter
            autocomplete="current-passord"
            autofocus
            @click:append="walletPasswordShow = !walletPasswordShow" />
        </v-form>
      </v-card-text>
      <v-card-text v-if="walletPrivateKey" class="text-xs-center">
        <code v-text="walletPrivateKey"></code>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          v-if="walletPrivateKey"
          class="btn btn-primary mr-1"
          @click="walletBackedUp">
          Backed up!
        </button>
        <button
          v-if="!walletPrivateKey"
          :disabled="loading"
          class="btn btn-primary mr-1"
          @click="showPrivateKey">
          Display Private Key
        </button> <button
          v-if="!walletPrivateKey"
          :disabled="loading"
          class="btn"
          @click="dialog = false">
          Close
        </button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>

import {setDraggable, unlockBrowserWallet, lockBrowserWallet, getCurrentBrowserWallet, hashCode} from '../js/WalletUtils.js';

export default {
  props: {
    displayCompleteMessage: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      dialog: false,
      error: null,
      walletPassword: '',
      walletPasswordShow: false,
      walletPrivateKey: null,
      autoGeneratedPassword: false,
      backedUp: false,
      loading: false,
      rules: {
        min: (v) => (v && v.length >= 8) || 'At least 8 characters',
      },
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        if (this.$refs.form) {
          this.$refs.form.reset();
        }
        this.init();
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.error = null;
      this.walletPrivateKey = null;
      this.walletPassword = '';
      this.walletPasswordShow = false;
      this.autoGeneratedPassword = window.walletSettings.userPreferences.autoGenerated;
      this.backedUp = window.walletSettings.userPreferences.backedUp;
      this.loading = false;
    },
    walletBackedUp() {
      this.$emit('copied');
      this.backedUp = window.walletSettings.userPreferences.backedUp;
      this.loading = false;
      this.dialog = false;
    },
    showPrivateKey() {
      this.error = null;
      if (!this.$refs.form.validate()) {
        return;
      }
      this.loading = true;
      window.setTimeout(this.unlockBrowserWallet, 200);
    },
    unlockBrowserWallet() {
      const unlocked = unlockBrowserWallet(this.autoGeneratedPassword ? window.walletSettings.userP : hashCode(this.walletPassword));
      try {
        if (unlocked) {
          const wallet = getCurrentBrowserWallet();
          if (!wallet || !wallet.privateKey) {
            this.error = "Can't find your wallet in current browser";
            return;
          }
          this.loading = false;
          this.walletPrivateKey = wallet.privateKey;
        } else if (this.autoGeneratedPassword) {
          this.error = 'Error while retrieving the private key';
        } else {
          this.error = 'Wrong password';
        }
      } finally {
        this.loading = false;
        if (unlocked) {
          lockBrowserWallet();
        }
      }
    },
  },
};
</script>
