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
          <v-toolbar color="transparent" tabs flat>
            <v-tabs ref="settingsTabs" v-model="selectedTab">
              <v-tabs-slider />
              <v-tab>Settings</v-tab>
              <v-tab v-if="!isSpace">Advanced settings</v-tab>
              <v-tab>Wallet details</v-tab>
            </v-tabs>
          </v-toolbar>
          <v-tabs-items v-model="selectedTab">
            <v-tab-item v-if="!isSpace">
              <v-card>
                <v-card-text>
                  <span>Currency</span>
                  <v-combobox
                    v-model="selectedCurrency"
                    :items="currencies"
                    label="Select fiat currency used to display ether amounts conversion" />
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item>
              <v-card>
                <v-card-text v-if="!isSpace">
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
                <v-card-text v-if="displayWalletResetOption">
                  <wallet-reset-modal @reseted="$emit('settings-changed')"/>
                </v-card-text>
                <v-card-text v-if="displayWalletResetOption">
                  <wallet-backup-modal :display-complete-message="false" @copied="$emit('copied')" />
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item>
              <v-card>
                <v-card-text>
                  <qr-code ref="qrCode"
                           :to="walletAddress"
                           title="Address QR Code"
                           information="You can send this Wallet address or QR code to other users to send you ether and tokens" />
        
                  <div class="text-xs-center">
                    <wallet-address :value="walletAddress" />
                  </div>
                </v-card-text>
              </v-card>
            </v-tab-item>
          </v-tabs-items>
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
import WalletResetModal from './WalletResetModal.vue';
import WalletBackupModal from './WalletBackupModal.vue';

import {gasToFiat, enableMetamask, disableMetamask} from '../WalletUtils.js';
import {FIAT_CURRENCIES} from '../WalletConstants.js';

export default {
  components: {
    QrCode,
    WalletResetModal,
    WalletBackupModal,
    WalletAddress
  },
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      }
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displayResetOption: {
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
    }
  },
  data () {
    return {
      loading: false,
      show: false,
      error: null,
      walletAddress: null,
      selectedTab: null,
      selectedCurrency: FIAT_CURRENCIES['usd'],
      currencies: [],
      defaultGas: 0,
      useMetamaskChoice: false,
      defaulGasPriceFiat: 0
    };
  },
  computed: {
    displayWalletResetOption() {
      return this.displayResetOption && !this.useMetamaskChoice;
    }
  },
  watch: {
    open() {
      if (this.open) {
        this.walletAddress = window.walletSettings.userPreferences.walletAddress;
        this.defaultGas = window.walletSettings.userPreferences.userDefaultGas ? window.walletSettings.userPreferences.userDefaultGas : 21000;
        this.defaulGasPriceFiat = gasToFiat(this.defaultGas);
        this.$refs.qrCode.computeCanvas();
        this.useMetamaskChoice = window.walletSettings.userPreferences.useMetamask;
        if (window.walletSettings.userPreferences.currency) {
          this.selectedCurrency = FIAT_CURRENCIES[window.walletSettings.userPreferences.currency];
        }
        this.show = true;

        // Workaround to display slider on first popin open
        this.$refs.settingsTabs.callSlider();
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
        if(this.isSpace) {
          this.saveCommonSettings();
          this.$emit('settings-changed');
        } else {
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

              this.saveCommonSettings();

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
        }
      } catch(e) {
        console.debug("savePreferences method - error", e);
        this.loading = false;
        this.error = `Error while proceeding: ${e}`;
        this.$emit("end-loading");
      }
    },
    saveCommonSettings() {
      if (this.useMetamaskChoice) {
        enableMetamask(this.isSpace);
      } else {
        disableMetamask(this.isSpace);
      }
    }
  }
};
</script>