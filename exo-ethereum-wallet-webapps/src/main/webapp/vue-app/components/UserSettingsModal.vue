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
          <v-tabs ref="settingsTabs" v-model="selectedTab" class="pl-3 pr-3">
            <v-tabs-slider />
            <v-tab>Display</v-tab>
            <v-tab v-if="!isSpace">Advanced settings</v-tab>
            <v-tab v-if="walletAddress">Wallet details</v-tab>
          </v-tabs>
          <v-tabs-items v-model="selectedTab">
            <v-tab-item v-if="!isSpace">
              <v-card>
                <v-card-text>
                  <v-combobox
                    v-model="selectedCurrency"
                    :items="currencies"
                    label="Select fiat currency used to display ether amounts conversion" />
                  <v-combobox
                    v-model="selectedPrincipalAccount"
                    :items="accountsList"
                    item-disabled="itemDisabled"
                    label="Select principal currency displayed in wallet summary"
                    placeholder="Select principal currency displayed in wallet summary"
                    chips />
                  <v-combobox
                    v-model="selectedOverviewAccounts"
                    :items="accountsList"
                    label="List of balances to display on wallet summary (by order)"
                    placeholder="List of contracts, ether and fiat to display on wallet summary (by order)"
                    multiple
                    deletable-chips
                    clearable
                    chips />
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item>
              <v-card>
                <v-card-text v-if="!isSpace">
                  <div>
                    <span>Maximum transaction fee</span>
                    <v-slider v-model="defaultGas"
                              :label="`${defaultGas}${defaulGasPriceFiat ? ' (' + defaulGasPriceFiat + ' ' + fiatSymbol + ')' : ''}`"
                              :max="90000"
                              :min="21000"
                              :step="1000"
                              type="number" />
                  </div>
                  <div>
                    <v-switch v-model="useMetamaskChoice" label="Use Metamask to access your wallet in current browser"></v-switch>
                  </div>
                  <div v-if="displayWalletResetOption" class="mb-3">
                    <wallet-reset-modal @reseted="$emit('settings-changed')"/>
                  </div>
                  <div v-if="displayWalletResetOption">
                    <wallet-backup-modal :display-complete-message="false" @copied="$emit('copied')" />
                  </div>
                </v-card-text>
              </v-card>
            </v-tab-item>
            <v-tab-item v-if="walletAddress">
              <v-card>
                <v-card-text>
                  <qr-code
                    ref="qrCode"
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
        <button :disabled="loading" :loading="loading" class="btn btn-primary mr-1" @click="savePreferences">Save</button>
        <button :disabled="loading" :loading="loading" class="btn" @click="show = false">Close</button>
        <v-spacer />
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
    },
    overviewAccounts: {
      type: Object,
      default: function() {
        return {};
      }
    },
    principalAccount: {
      type: Object,
      default: function() {
        return {};
      }
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
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
      defaulGasPriceFiat: 0,
      selectedOverviewAccounts: [],
      selectedPrincipalAccount: null,
      etherAccount: {text: 'Ether', value: 'ether', disabled: false},
      fiatAccount: {text: 'Fiat ($, €...)', value: 'fiat', disabled: false},
      accountsList: []
    };
  },
  computed: {
    displayWalletResetOption() {
      return this.displayResetOption && !this.useMetamaskChoice;
    }
  },
  watch: {
    walletAddress() {
      if (this.walletAddress) {
        this.$nextTick(() => {
          this.$refs.qrCode.computeCanvas();
        });
      }
    },
    open() {
      if (this.open) {
        this.error = null;
        this.walletAddress = window.walletSettings.userPreferences.walletAddress;
        this.defaultGas = window.walletSettings.userPreferences.userDefaultGas ? window.walletSettings.userPreferences.userDefaultGas : 21000;
        this.defaulGasPriceFiat = gasToFiat(this.defaultGas);
        this.useMetamaskChoice = window.walletSettings.userPreferences.useMetamask;
        if (window.walletSettings.userPreferences.currency) {
          this.selectedCurrency = FIAT_CURRENCIES[window.walletSettings.userPreferences.currency];
        }

        this.accountsList = [];
        this.selectedOverviewAccounts = [];
        this.selectedPrincipalAccount = null;

        this.accountsList.push(Object.assign({}, this.etherAccount), Object.assign({}, this.fiatAccount));
        if (this.accountsDetails) {
          Object.keys(this.accountsDetails).forEach(key => {
            const accountDetails = this.accountsDetails[key];
            if (accountDetails.isContract) {
              this.accountsList.push({text: accountDetails.name, value: accountDetails.address, disabled: false});
            }
          });
        }

        this.overviewAccounts.forEach(selectedValue => {
          const selectedObject = this.getOverviewAccountObject(selectedValue);
          if (selectedObject) {
            this.selectedOverviewAccounts.push(selectedObject);
          }
        });

        this.selectedPrincipalAccount = this.getOverviewAccountObject(this.principalAccount);

        // Workaround to display slider on first popin open
        this.$refs.settingsTabs.callSlider();

        this.show = true;
      }
    },
    selectedPrincipalAccount() {
      if (this.selectedPrincipalAccount) {
        this.selectedOverviewAccounts.forEach(account => {
          account.disabled = false;
        });

        this.accountsList.forEach((account, index) => {
          if (this.selectedPrincipalAccount.value === account.value) {
            account.disabled = true;
            const accountIndex = this.selectedOverviewAccounts.findIndex(foundSelectedAccount => foundSelectedAccount.value === account.value);
            if (accountIndex >= 0) {
              this.selectedOverviewAccounts.splice(accountIndex, 1);
            }
            this.selectedOverviewAccounts.unshift(account);
          } else {
            account.disabled = false;
          }
        });
        this.$forceUpdate();
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
      try {
        if(this.isSpace) {
          this.saveCommonSettings();
          this.$emit('settings-changed');
          this.show = false;
        } else {
          this.loading = true;
          fetch('/portal/rest/wallet/api/account/savePreferences', {
            method: 'POST',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
              defaultGas: this.defaultGas,
              currency: this.selectedCurrency.value,
              principalAccount: this.selectedPrincipalAccount.value,
              overviewAccounts: this.selectedOverviewAccounts.map(item => item.value),
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
    },
    getOverviewAccountObject(selectedValue) {
      if (selectedValue === 'fiat') {
        return Object.assign({}, this.fiatAccount);
      } else if (selectedValue === 'ether') {
        return Object.assign({}, this.etherAccount);
      } else if(this.accountsList && this.accountsList.length) {
        const selectedContractAddress = this.accountsList.findIndex(contract => contract.value === selectedValue);
        if (selectedContractAddress >= 0) {
          const contract = this.accountsList[selectedContractAddress];
          if (!contract.error) {
            return {text: contract.text, value: contract.value, disabled: false};
          }
        }
      }
    }
  }
};
</script>