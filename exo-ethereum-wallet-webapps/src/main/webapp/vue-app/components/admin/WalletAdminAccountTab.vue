<template>
  <v-flex v-if="sameConfiguredNetwork" flat>
    <confirm-dialog
      ref="deleteAdminWalletConfirm"
      message="Would you like to delete admin wallet?"
      title="Delete admin wallet confirmation"
      ok-label="Delete"
      cancel-label="Cancel"
      @ok="removeAdminWallet()"
      @closed="useAdminWallet = true" />
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-container>
      <v-layout>
        <v-flex class="text-xs-left">
          <v-switch
            v-model="useAdminWallet"
            :disabled="creatingWallet || removingWallet || loading"
            label="Use admin wallet to administrate wallets" />
        </v-flex>
      </v-layout>
      <v-layout>
        <v-flex v-if="creatingWallet">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Creating admin wallet...
        </v-flex>
        <v-flex v-else-if="removingWallet">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Removing admin wallet...
        </v-flex>
        <v-flex v-else-if="loading">
          <v-progress-circular
            color="primary"
            indeterminate
            size="20" />
          Loading admin wallet...
        </v-flex>
        <v-flex v-else-if="useAdminWallet && !adminWalletExists" class="text-xs-left">
          <v-radio-group v-model="adminWalletCreationAction">
            <v-radio
              value="CREATE"
              label="Create new admin wallet" />
            <v-flex v-if="adminWalletCreationAction === 'CREATE'" class="mb-2">
              <button class="btn btn-primary" @click="createAdminWallet()">
                Create wallet
              </button>
            </v-flex>
            <v-radio
              value="IMPORT"
              label="Import admin wallet private key" />
            <v-layout v-if="adminWalletCreationAction === 'IMPORT'" class="mb-2">
              <v-flex xs6>
                <v-form ref="adminWalletImportForm">
                  <v-text-field
                    v-if="adminWalletCreationAction === 'IMPORT'"
                    v-model="walletPrivateKey"
                    :append-icon="walletPrivateKeyShow ? 'visibility_off' : 'visibility'"
                    :rules="[rules.priv]"
                    :type="walletPrivateKeyShow ? 'text' : 'password'"
                    :disabled="loading"
                    name="walletPrivateKey"
                    placeholder="Enter wallet admin private key"
                    autocomplete="off"
                    autofocus
                    required
                    @click:append="walletPrivateKeyShow = !walletPrivateKeyShow" />
                </v-form>
              </v-flex>
              <v-flex xs6 class="text-xs-left">
                <button class="btn btn-primary mt-2 ml-2" @click="importAminWallet()">
                  Import wallet
                </button>
              </v-flex>
            </v-layout>
          </v-radio-group>
        </v-flex>
      </v-layout>
    </v-container>
  </v-flex>
</template>

<script>
import ConfirmDialog from '../ConfirmDialog.vue';

import {searchWalletByTypeAndId} from '../..//WalletAddressRegistry.js';

export default {
  components: {
    ConfirmDialog,
  },
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    sameConfiguredNetwork: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      error: null,
      useAdminWallet: false,
      adminWallet: null,
      adminWalletExists: false,
      walletPrivateKey: '',
      walletPrivateKeyShow: false,
      loading: false,
      creatingWallet: false,
      removingWallet: false,
      adminWalletCreationAction: 'CREATE',
      rules: {
        priv: (v) => (v && (v.length === 66 || v.length === 64)) || 'Exactly 64 or 66 (with "0x") characters are required',
      },
    };
  },
  computed: {
  },
  watch: {
    adminWalletCreationAction() {
      this.walletPrivateKey = '';
      this.walletPrivateKeyShow = false;
    },
    useAdminWallet() {
      if (!this.useAdminWallet && this.adminWalletExists) {
        this.$refs.deleteAdminWalletConfirm.open();
      }
    },
  },
  methods: {
    init() {
      this.loading = true;
      return searchWalletByTypeAndId('admin', 'ADMIN')
      .then((wallet) => {
        this.adminWallet = wallet;
        this.useAdminWallet = this.adminWalletExists = !!(wallet && wallet.address);
      }).catch((error) => {
        this.error = String(error);
      })
      .finally(() => {
        this.loading = false;
      });
    },
    removeAdminWallet() {
      this.removingWallet = true;
      return fetch('/portal/rest/wallet/api/account/removeAdminWallet', {
        method: 'GET',
        credentials: 'include',
      }).then((resp) => {
        if (!resp || !resp.ok) {
          throw new Error('Error deleting admin wallet from server');
        }
        return this.init(); 
      }).catch((error) => {
        this.error = String(error);
      })
      .finally(() => {
        this.removingWallet = false;
      });
    },
    createAdminWallet(privateKey) {
      this.creatingWallet = true;
      return fetch('/portal/rest/wallet/api/account/createAdminAccount', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: $.param({
          privateKey: privateKey || '',
        }),
      }).then((resp) => {
        if (!resp || !resp.ok) {
          throw new Error('Error creating admin wallet on server');
        }
        return this.init(); 
      }).catch((error) => {
        this.error = String(error);
      })
      .finally(() => {
        this.creatingWallet = false;
      });
    },
    importAminWallet() {
      if (!this.$refs.adminWalletImportForm.validate()) {
        return;
      }
      return this.createAdminWallet(this.walletPrivateKey);
    },
  },
};
</script>
