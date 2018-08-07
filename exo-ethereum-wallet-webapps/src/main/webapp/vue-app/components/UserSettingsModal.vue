<template>
  <v-dialog v-model="show" width="500px" max-width="100vw">
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>User Preferences</v-toolbar-title>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-form>
          <v-flex>
            <span>Default gas to spend on transactions (Maximum fee per transaction)</span>
            <v-slider v-model="defaultGas"
                      :label="`${defaultGas}${defaulGasPriceUsd ? ' (' + defaulGasPriceUsd + ' $)' : ''}`"
                      :max="90000"
                      :min="21000"
                      :step="1000"
                      type="number" />
          </v-flex>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn color="primary" @click="savePreferences">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import {gasToUSD} from '../WalletUtils.js';

export default {
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
      });
    }
  }
};
</script>