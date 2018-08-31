<template>
  <v-dialog v-model="show" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="show = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="show = false"></a>
        <span class="PopupTitle popupTitle">User Preferences</span>
      </div>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-flex>
          <span>Default gas to spend on transactions (Maximum fee per transaction)</span>
          <v-slider v-model="defaultGas"
                    :label="`${defaultGas}${defaulGasPriceUsd ? ' (' + defaulGasPriceUsd + ' $)' : ''}`"
                    :max="90000"
                    :min="21000"
                    :step="1000"
                    type="number" />

          <qr-code ref="qrCode"
                   :to="account"
                   :build="true"
                   title="Address QR Code"
                   information="You can send this Wallet address or QR code to other users to send you ether and tokens" />

          <div class="text-xs-center">
            Wallet address: <code>{{ account }}</code>
          </div>
        </v-flex>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading" :loading="loading" class="btn btn-primary" @click="savePreferences">Save</button>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import QrCode from './QRCode.vue';

import {gasToUSD} from '../WalletUtils.js';

export default {
  components: {
    QrCode
  },
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      }
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    account: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data () {
    return {
      loading: false,
      show: false,
      error: null,
      defaultGas: 0,
      defaulGasPriceUsd: 0
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.defaultGas = window.walletSettings.userDefaultGas ? window.walletSettings.userDefaultGas : 21000;
        this.defaulGasPriceUsd = gasToUSD(this.defaultGas);
        this.$refs.qrCode.computeCanvas();
        this.show = true;
      }
    },
    show() {
      if(!this.show) {
        this.$emit("close");
      }
    },
    defaultGas() {
      this.defaulGasPriceUsd = gasToUSD(this.defaultGas);
    }
  },
  methods: {
    savePreferences() {
      this.loading = true;
      try {
        fetch('/portal/rest/wallet/api/account/savePreferences', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({
            defaultGas: this.defaultGas
          })
        }).then(resp => {
          if (resp && resp.ok) {
            window.walletSettings.userDefaultGas = this.defaultGas;
            this.$emit('settings-changed', {
              defaultGas: this.defaultGas
            });
            this.show = false;
          } else {
            this.error = 'Error saving preferences';
          }
          this.loading = false;
        }).catch (e => {
          console.debug("savePreferences method - error", e);
          this.error = `Error while proceeding: ${e}`;
          this.loading = false;
        });
      } catch(e) {
        console.debug("savePreferences method - error", e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit("end-loading");
      }
    }
  }
};
</script>