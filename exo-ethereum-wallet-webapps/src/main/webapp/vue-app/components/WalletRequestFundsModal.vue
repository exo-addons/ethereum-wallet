<template>
  <v-dialog v-model="dialog" content-class="uiPopup" width="450px" max-width="100vw" @keydown.esc="dialog = false">
    <button slot="activator" class="btn btn-primary ml-1 mt-2">Request funds</button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Receive funds</span>
      </div>
      <div v-if="error && !loading" class="alert alert-error v-content">
        <i class="uiIconError"></i>{{ error }}
      </div>
      <v-card-text class="text-xs-center ">
        <v-combobox
          v-model="selectedOption"
          :items="accountsList"
          label="Select account" />
        <auto-complete v-if="selectedAccount" ref="autocomplete" :disabled="loading" input-label="Recipient" @item-selected="recipient = $event" />
        <v-text-field v-if="selectedAccount" v-model.number="amount" :disabled="loading" name="amount" label="Amount" />
        <v-textarea
          v-if="selectedAccount"
          id="requestMessage"
          v-model="requestMessage"
          :disabled="loading"
          name="requestMessage"
          label="Request message (Optional)"
          class="mt-4"
          rows="7"
          flat
          no-resize />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button :disabled="loading" class="btn btn-primary" @click="requestFunds">Send request</button>
        <button :disabled="loading" class="btn ml-2" @click="dialog = false">Close</button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import AutoComplete from './AutoComplete.vue';

export default {
  components: {
    AutoComplete
  },
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
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
  },
  data() {
    return {
      selectedOption: null,
      recipient: null,
      amount: null,
      error: null,
      requestMessage: '',
      loading: false,
      dialog: false
    };
  },
  computed: {
    selectedAccount() {
      return this.selectedOption && this.selectedOption.value;
    },
    accountsList() {
      const accountsList = [];
      console.log(this.accountsDetails);
      if (this.accountsDetails && this.refreshIndex > 0) {
        Object.keys(this.accountsDetails).forEach(key =>{
          if(!this.accountsDetails[key].isContract || this.accountsDetails[key].isDefault) {
            accountsList.push({
              text: this.accountsDetails[key].title,
              value: this.accountsDetails[key]
            });
          }
        });
        console.log("accountsList", accountsList);
      }
      return accountsList;
    }
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.requestMessage = '';
        this.recipient = null;
        if (this.$refs && this.$refs.autocomplete) {
          this.$refs.autocomplete.clear();
        }
      }
    }
  },
  methods: {
    requestFunds() {
      if (!this.selectedAccount) {
        this.error = 'Please select a valid account';
        return;
      }

      console.log(this.recipient);

      if (!this.recipient) {
        this.error = 'Please select a receipient to your request';
        return;
      }

      this.loading = true;
      fetch('/portal/rest/wallet/api/account/requestFunds', {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          address: this.walletAddress,
          receipient: this.recipient.id,
          receipientType: this.recipient.type,
          contract: this.selectedAccount.isContract ? this.selectedAccount.address : null,
          amount: this.amount,
          message: this.requestMessage
        })
      }).then(resp => {
        if (resp && resp.ok) {
          this.dialog = false;
        } else {
          this.error = 'Error requesting funds';
        }
        this.loading = false;
      }).catch (e => {
        console.debug("requestFunds method - error", e);
        this.error = `Error while proceeding: ${e}`;
        this.loading = false;
      });
    }
  }
};
</script>