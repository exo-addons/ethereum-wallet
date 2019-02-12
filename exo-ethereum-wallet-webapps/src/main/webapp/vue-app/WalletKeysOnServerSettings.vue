<template>
  <v-dialog
    v-model="show"
    content-class="uiPopup with-overflow not-draggable"
    width="300px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a>
          <span class="PopupTitle popupTitle">
            Save private key on server
          </span>
      </div>
      <v-card-text v-if="loading || appLoading" class="text-xs-center">
        <v-form ref="form">
          <v-switch
            slot="activator"
            v-model="storeOnServer"
            label="Store wallet keys on server"
            @click="open" />
          <information-bubble>
            <v-btn class="inlineIconButton" icon>
              <i class="uiIconInformation"></i>
            </v-btn>
            <p slot="content">
              When checking this, wallet private keys will be stored on server side encrypted with your password.
            </p>
          </information-bubble>
  
          <v-text-field
            v-if="dialog"
            v-model="walletPassword"
            :append-icon="walletPasswordShow ? 'visibility_off' : 'visibility'"
            :rules="[rules.min]"
            :type="walletPasswordShow ? 'text' : 'password'"
            :disabled="loading"
            :label="rememberPasswordToChange ? 'New wallet password'"
            name="walletPassword"
            placeholder="Enter your wallet password"
            autocomplete="current-passord"
            counter
            autofocus
            required
            @click:append="walletPasswordShow = !walletPasswordShow" />
          </v-form>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
import InformationBubble from './InformationBubble.vue';

export default {
  components: {
    InformationBubble,
  },
  data() {
    return {
      storeOnServer: false,
      dialog: false,
      error: null,
      rules: {
        min: (v) => (v && v.length >= 8) || 'At least 8 characters',
      },
    };
  },
  computed: {
    show() {
      return this.storeOnServer && this.dialog;
    }
  },
  methods: {
    open() {
      this.$nextTick(() => {
        if (this.storeOnServer) {
          this.dialog = true;
        } else {
          // TODO remove private key from server
        }
      });
    },
    saveWalletKey() {
      if(!this.$refs.form.validate()) {
        return false;
      }

      return saveWalletKey();
    },
  },
}
</script>