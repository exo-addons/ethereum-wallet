<template>
  <v-dialog v-model="dialog" width="300px" max-width="100vh">
    <v-btn slot="activator" icon title="Add token">
      <v-icon>add</v-icon>
    </v-btn>
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Add ERC20 Token Contract</v-toolbar-title>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-form>
          <v-text-field v-model="address" name="address" label="Address" type="text"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="primary" @click="addToken">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import {saveContractAddress} from '../WalletToken.js';

export default {
  props: {
    account: {
      type: String,
      default: function() {
        return {};
      }
    },
    netId: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data () {
    return {
      dialog: null,
      error: null,
      address: null
    };
  },
  methods: {
    addToken() {
      this.error = null;
      if (!this.address || !window.localWeb3.utils.isAddress(this.address)) {
        this.error = "Invalid address";
        return;
      }
      try {
        const contractBalanceOfPromise = saveContractAddress(this.account, this.address, this.netId);
        if (contractBalanceOfPromise) {
          contractBalanceOfPromise
            .then((added) => {
              if (added) {
                this.$emit("added");
                this.dialog = false;
                this.address = null;
              } else {
                this.error = `Address is not recognized as contract's address`;
              }
            })
            .catch(err => {
              this.error = `Address is not recognized as contract's address ${err}`;
            });
        } else {
          this.error = `Address is not recognized as contract's address`;
        }
      } catch(e) {
        this.error = `Error encountered: ${e}`;
      }
    }
  }
};
</script>

