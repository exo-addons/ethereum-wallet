<template>
  <v-dialog
    v-model="show"
    content-class="uiPopup"
    width="500px"
    max-width="100vw"
    @keydown.esc="show = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="show = false"></a>
        <span class="PopupTitle popupTitle">
          Add Token address
        </span>
      </div>
      <v-card-text>
        <div v-if="error && !loading" class="alert alert-error v-content">
          <i class="uiIconError"></i>{{ error }}
        </div>
        <v-form
          @submit="
            $event.preventDefault();
            $event.stopPropagation();
          ">
          <v-text-field
            v-model="address"
            name="address"
            label="Address"
            placeholder="Select ERC20 Token address"
            type="text" />
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button
          :disabled="loading"
          :loading="loading"
          class="btn btn-primary"
          @click="addToken">
          Save
        </button>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import {saveContractAddress} from '../../WalletToken.js';
import {setDraggable} from '../../WalletUtils.js';

export default {
  props: {
    open: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    account: {
      type: String,
      default: function() {
        return null;
      },
    },
    netId: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    isDefaultContract: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      show: false,
      loading: null,
      error: null,
      address: null,
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.show = true;
        this.$nextTick(() => {
          setDraggable();
        });
      }
    },
    show() {
      if (!this.show) {
        this.$emit('close');
      }
    },
  },
  methods: {
    addToken() {
      this.error = null;
      if (!this.address || !window.localWeb3.utils.isAddress(this.address)) {
        this.error = 'Invalid address';
        return;
      }
      this.loading = true;
      try {
        return saveContractAddress(this.account, this.address.toLowerCase(), this.netId, this.isDefaultContract)
          .then((added, error) => {
            if (error) {
              throw error;
            }
            if (added) {
              this.$emit('added', this.address);
              this.show = false;
              this.address = null;
            } else {
              this.error = `Address is not recognized as ERC20 Token contract's address`;
            }
            this.loading = false;
          })
          .catch((err) => {
            console.debug('saveContractAddress method - error', err);
            this.loading = false;
            this.error = `${err}`;
          });
      } catch (e) {
        console.debug('saveContractAddress method - error', e);
        this.loading = false;
        this.error = `${e}`;
      }
    },
  },
};
</script>
