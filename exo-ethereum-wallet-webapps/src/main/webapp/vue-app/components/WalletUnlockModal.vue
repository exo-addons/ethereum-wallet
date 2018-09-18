<template>
  <v-dialog v-model="dialog" :disabled="disabled" class="fixLinkHeight" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <a slot="activator" href="javascript:void(0);" @click="dialog = true">Unlock wallet</a>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Unlock wallet</span>
      </div>
      <v-card-text>
        <div v-if="errorMessage" class="alert alert-error">
          <i class="uiIconError"></i>
          {{ errorMessage }}
        </div>

        <div v-if="rememberPassword" class="alert alert-warning v-content">
          <i class="uiIconWarning"></i>
          You have chosen to remember your password, thus your password will be stored in current browser and you will not have to re-enter it again to unlock your wallet. 
        </div>
        <div v-else class="alert alert-info v-content">
          <i class="uiIconInfo"></i>
          You have chosen to not remember your password, thus you will have to unlock your wallet each time you want to make a transaction with your wallet.
        </div>
    
        <v-text-field
          v-model="walletPassword"
          :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
          :rules="[rules.min]"
          :type="walletPasswordShow ? 'text' : 'password'"
          name="walletPassword"
          label="Wallet Password"
          counter
          @click:append="walletPasswordShow = !walletPasswordShow"
        />
    
        <v-switch v-model="rememberPassword" label="Remember my password" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button class="btn btn-primary mr-1" @click="unlockWallet">Unlock</button>
        <button class="btn" @click="dialog = false">Close</button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import {unlockBrowerWallet} from '../WalletUtils.js';

export default {
  data() {
    return {
      dialog: false,
      errorMessage: null,
      walletPassword: '',
      walletPasswordShow: false,
      rememberPassword: false,
      rules: {
        min: v => v.length >= 8 || 'At least 8 characters'
      }
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.errorMessage = null;
        this.walletPassword = '';
        this.walletPasswordShow = false;
        this.rememberPassword = false;
      }
    }
  },
  methods: {
    unlockWallet() {
      const unlocked = unlockBrowerWallet(this.walletPassword, null, null, true, this.rememberPassword);
      if (unlocked) {
        this.$emit("refresh");
        this.dialog = false;
      } else {
        this.errorMessage = 'Wrong password';
      }
    }
  }
};
</script>