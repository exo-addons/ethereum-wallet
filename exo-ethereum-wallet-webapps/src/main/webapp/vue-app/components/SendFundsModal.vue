<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <v-btn v-if="icon" slot="activator" :disabled="disabled" class="bottomNavigationItem" title="Send funds" flat value="send">
      <span>Send</span>
      <v-icon>send</v-icon>
    </v-btn>
    <button v-else slot="activator" :disabled="disabled" class="btn btn-primary mr-1 mt-2">
      Send
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Funds</span>
      </div>

      <send-funds-form 
        ref="sendFundsForm"
        :wallet-address="walletAddress"
        :network-id="networkId"
        :selected-account="selectedOption && selectedOption.value"
        @success="$emit('success', $event)"
        @error="$emit('error', $event)"
        @sent="addPendingTransaction($event)"
        @pending="$emit('pending', $event)"
        @close="dialog = false">

        <div id="sendFundsAccount">
          <v-combobox
            v-model="selectedOption"
            :items="accountsList"
            attach="#sendFundsAccount"
            absolute
            label="Select currency"
            placeholder="Select a currency to use for requesting funds" />
        </div>

      </send-funds-form>
    </v-card>
  </v-dialog>
</template>

<script>
import SendFundsForm from './SendFundsForm.vue';

export default {
  components: {
    SendFundsForm
  },
  props: {
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    networkId: {
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
    icon: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    disabled: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      selectedOption: null,
      dialog: null
    };
  },
  computed: {
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
        this.$nextTick(() => {
          if (!this.selectedOption) {
            const contractAddress = this.principalAccount === 'ether' || this.principalAccount === 'fiat' ? null : this.principalAccount;
            this.prepareSendForm(null, null, null, contractAddress, true);
          }
        });
      } else {
        this.selectedOption = null;
      }
    }
  },
  methods: {
    prepareSendForm(receiver, receiver_type, amount, contractAddress, keepDialogOpen) {
      if (!this.accountsList || !this.accountsList.length) {
        console.debug("prepareSendForm error - no accounts found");
        return;
      }

      this.dialog = true;

      if (contractAddress && contractAddress.length) {
        this.selectedOption = null;
        let i = 0;
        while (i < this.accountsList.length && !this.selectedOption) {
          if (this.accountsList[i] && this.accountsList[i].value && this.accountsList[i].value.isContract
              && this.accountsList[i].value.address === contractAddress.toLowerCase()) {
            this.selectedOption = this.accountsList[i];
          }
          i++;
        }
      } else {
        this.selectedOption = this.accountsList[0];
      }

      this.$nextTick(() => {
        this.$refs.sendFundsForm.prepareSendForm(receiver, receiver_type, amount, contractAddress, keepDialogOpen);
      });
    }
  }
};
</script>

