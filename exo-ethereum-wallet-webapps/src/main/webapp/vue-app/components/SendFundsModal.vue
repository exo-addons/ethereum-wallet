<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="300px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <button slot="activator" :disabled="disabled" class="btn btn-primary mr-1">
      Send Funds
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Ether</span>
      </div>

      <v-card-title primary-title class="pt-0">
        <v-combobox
          v-model="selectedOption"
          :items="accountsList"
          label="Select account" />
      </v-card-title>

      <v-divider v-if="selectedAccount" />

      <send-ether-form v-if="selectedAccount && !selectedAccount.isContract"
                       ref="sendEtherForm"
                       :account="walletAddress"
                       :balance="selectedAccount.balance"
                       @sent="$emit('sent', $event)"
                       @close="dialog = false"
                       @error="$emit('error', $event)" />

      <send-tokens-form v-if="selectedAccount && selectedAccount.isContract"
                        ref="sendTokensForm"
                        :account="walletAddress"
                        :contract="selectedAccount.contract"
                        :balance="selectedAccount.balance"
                        :ether-balance="selectedAccount.etherBalance"
                        @sent="$emit('sent', $event)"
                        @close="dialog = false"
                        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendEtherForm from './SendEtherForm.vue';
import SendTokensForm from './SendTokensForm.vue';

export default {
  components: {
    SendEtherForm,
    SendTokensForm
  },
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    disabled: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data () {
    return {
      selectedOption: null,
      dialog: null
    };
  },
  computed: {
    selectedAccount() {
      return this.selectedOption && this.selectedOption.value;
    },
    accountsList() {
      const accountsList = [];
      if (this.accountsDetails && this.refreshIndex > 0) {
        Object.keys(this.accountsDetails).forEach(key =>
          accountsList.push({
            text: this.accountsDetails[key].title,
            value: this.accountsDetails[key]
          })
        );
      }
      return accountsList;
    }
  },
  watch: {
    dialog() {
      if (this.dialog) {
        if (this.$refs.sendEtherForm) {
          this.$refs.sendEtherForm.init();
        }
        if (this.$refs.sendTokensForm) {
          this.$refs.sendTokensForm.init();
        }
      }
    }
  }
};
</script>

