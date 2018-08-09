<template>
  <v-dialog v-model="show" width="300px" max-width="100vw">
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Add Token address</v-toolbar-title>
        <v-spacer />
        <v-btn icon dark @click.native="show = false">
          <v-icon>close</v-icon>
        </v-btn>
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
        <v-spacer />
        <v-btn color="primary" @click="addToken">Save</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import {saveContractAddress} from '../WalletToken.js';

export default {
  props: {
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
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
    },
    isDefaultContract: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data () {
    return {
      show: false,
      error: null,
      address: null
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.show = true;
      }
    },
    show() {
      if(!this.show) {
        this.$emit("close");
      }
    }
  },
  methods: {
    addToken() {
      this.error = null;
      if (!this.address || !window.localWeb3.utils.isAddress(this.address)) {
        this.error = "Invalid address";
        return;
      }
      try {
        saveContractAddress(this.account, this.address.toLowerCase(), this.netId, this.isDefaultContract)
          .then((added, error) => {
            if (error) {
              throw error;
            }
            if (added) {
              this.$emit("added", this.address);
              this.show = false;
              this.address = null;
            } else {
              this.error = `Address is not recognized as ERC20 Token contract's address`;
            }
          })
          .catch(err => {
            this.error = `${err}`;
          });
      } catch (e) {
        this.error = `${e}`;
      }
    }
  }
};
</script>

