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
          <v-expansion-panel>
            <v-expansion-panel-content>
              <div slot="header">Settings</div>
              <v-card>
                <v-card-text>
                  <span>Currency</span>
                  <v-combobox
                    v-model="selectedCurrency"
                    :items="currencies"
                    label="Select fiat currency used to display ether amounts conversion" />
                </v-card-text>
              </v-card>
            </v-expansion-panel-content>
            <v-expansion-panel-content>
              <div slot="header">Advanced settings</div>
              <v-card>
                <v-card-text>
                  <span>Gas limit to spend on transactions (Maximum fee per transaction)</span>
                  <v-slider v-model="defaultGas"
                            :label="`${defaultGas}${defaulGasPriceFiat ? ' (' + defaulGasPriceFiat + ' ' + fiatSymbol + ')' : ''}`"
                            :max="90000"
                            :min="21000"
                            :step="1000"
                            type="number" />
                </v-card-text>
                <v-card-text>
                  <v-switch v-model="useMetamaskChoice" label="Use Metamask to access your wallet in current browser"></v-switch>
                </v-card-text>
              </v-card>
            </v-expansion-panel-content>
            <v-expansion-panel-content>
              <div slot="header">Wallet details</div>
              <v-card>
                <v-card-text>
                  <qr-code ref="qrCode"
                           :to="account"
                           :build="true"
                           title="Address QR Code"
                           information="You can send this Wallet address or QR code to other users to send you ether and tokens" />
        
                  <div class="text-xs-center">
                    <wallet-address :value="account" />
                  </div>
                  <button class="btn btn-primary">Display private key (TODO)</button>
                </v-card-text>
              </v-card>
            </v-expansion-panel-content>
          </v-expansion-panel>
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
import WalletAddress from './WalletAddress.vue';

import {gasToFiat, enableMetamask, disableMetamask} from '../WalletUtils.js';
import {FIAT_CURRENCIES} from '../WalletConstants.js';

export default {
  components: {
    QrCode,
    WalletAddress
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
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
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
      selectedCurrency: FIAT_CURRENCIES['usd'],
      currencies: [],
      defaultGas: 0,
      useMetamaskChoice: false,
      defaulGasPriceFiat: 0
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.defaultGas = window.walletSettings.userPreferences.userDefaultGas ? window.walletSettings.userPreferences.userDefaultGas : 21000;
        this.defaulGasPriceFiat = gasToFiat(this.defaultGas);
        this.$refs.qrCode.computeCanvas();
        this.show = true;
        this.useMetamaskChoice = window.walletSettings.userPreferences.useMetamask;
        if (window.walletSettings.userPreferences.currency) {
          this.selectedCurrency = FIAT_CURRENCIES[window.walletSettings.userPreferences.currency];
        }
      }
    },
    show() {
      if(!this.show) {
        this.$emit("close");
      }
    },
    defaultGas() {
      this.defaulGasPriceFiat = gasToFiat(this.defaultGas);
    }
  },
  created() {
    Object.keys(FIAT_CURRENCIES).forEach(key => this.currencies.push(FIAT_CURRENCIES[key]));
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
            defaultGas: this.defaultGas,
            currency: this.selectedCurrency.value
          })
        }).then(resp => {
          if (resp && resp.ok) {
            if (this.useMetamaskChoice) {
              enableMetamask();
            } else {
              disableMetamask();
            }

            window.walletSettings.userPreferences.userDefaultGas = this.defaultGas;
            window.walletSettings.userPreferences.currency = this.selectedCurrency.value;
            this.$emit('settings-changed', {
              defaultGas: this.defaultGas,
              currency: this.selectedCurrency.value
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