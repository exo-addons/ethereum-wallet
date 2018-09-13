<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <v-btn v-if="icon" slot="activator" :disabled="disabled" class="mt-1 mb-1" fab dark small title="Send ether" color="primary" icon>
      <v-icon size="20">send</v-icon>
    </v-btn>
    <button v-else slot="activator" :disabled="disabled" :dark="!disabled" class="btn btn-primary mt-1 mb-1">
      Send Ether
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Ether</span>
      </div>
      <send-ether-form ref="sendEtherForm"
                       :account="account"
                       :balance="balance"
                       @sent="$emit('sent', $event)"
                       @close="dialog = false"
                       @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendEtherForm from './SendEtherForm.vue';

export default {
  components: {
    SendEtherForm
  },
  props: {
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    icon: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    balance: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data () {
    return {
      dialog: null
    };
  },
  computed: {
    disabled() {
      return !this.balance
             || this.balance === 0
             || (typeof this.balance === "string" 
                 && (!this.balance.length || this.balance.trim() === "0"));
    }
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.sendEtherForm.init();
        this.loading = false;
        this.recipient = null;
        this.amount = null;
        this.error = null;
      }
    }
  }
};
</script>

