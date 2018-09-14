<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <button v-if="!noButton" slot="activator" :disabled="disabled" class="btn btn-primary mt-1 mb-1">
      Send Tokens
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Tokens</span>
      </div>
      <send-tokens-form ref="sendTokensForm"
                        :account="account"
                        :contract="contract"
                        :balance="balance"
                        :ether-balance="etherBalance"
                        @sent="$emit('sent', $event)"
                        @close="dialog = false"
                        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendTokensForm from './SendTokensForm.vue';

export default {
  components: {
    SendTokensForm
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    noButton: {
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
    contract: {
      type: Object,
      default: function() {
        return {};
      }
    },
    balance: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    etherBalance: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data () {
    return {
      dialog: null,
    };
  },
  computed: {
    disabled() {
      return !this.balance
        || this.balance === 0
        || (typeof this.balance === "string" 
            && (!this.balance.length || this.balance.trim() === "0"))
        || !this.etherBalance
        || this.etherBalance === 0
        || (typeof this.etherBalance === "string" 
            && (!this.etherBalance.length || this.etherBalance.trim() === "0"));
    }
  },
  watch: {
    open() {
      this.dialog = this.open;
    },
    dialog() {
      if (this.dialog) {
        this.$refs.sendTokensForm.init();
      } else {
        this.$emit('close');
      }
    }
  }
};
</script>

