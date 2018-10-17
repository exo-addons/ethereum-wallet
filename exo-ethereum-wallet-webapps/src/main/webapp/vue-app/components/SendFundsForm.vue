<template>
  <v-flex class="pt-4">
    <div id="sendFundsFormSlot" class="pl-3 pr-3">
      <slot></slot>
    </div>

    <send-ether-form 
      v-if="formName === 'ether'"
      ref="sendEtherForm"
      :account="walletAddress"
      :balance="selectedAccount && selectedAccount.balance"
      @receiver-selected="receiver = $event.id; receiver_type = $event.type"
      @amount-selected="amount = $event"
      @sent="addPendingTransaction($event)"
      @close="$emit('close')">

      <div id="sendEtherFormSlot" class="ml-1"></div>

    </send-ether-form>
    <send-tokens-form
      v-else-if="formName === 'token'"
      ref="sendTokensForm"
      :account="walletAddress"
      :contract-details="selectedAccount"
      @receiver-selected="receiver = $event.id; receiver_type = $event.type"
      @amount-selected="amount = $event"
      @sent="addPendingTransaction($event)"
      @close="$emit('close')">

      <div id="sendTokensFormSlot" class="ml-1"></div>

    </send-tokens-form>
  </v-flex>
</template>

<script>
import SendEtherForm from './SendEtherForm.vue';
import SendTokensForm from './SendTokensForm.vue';

import {addTransaction} from '../WalletTransactions.js';

export default {
  components: {
    SendEtherForm,
    SendTokensForm
  },
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    selectedAccount: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      formName: null,
      receiver: null,
      receiver_type: null,
      amount: null
    };
  },
  watch: {
    selectedAccount() {
      // This is a workaround for a cyclic dependency problem using multiple slot locations
      this.$nextTick(() => {
        if (!this.selectedAccount) {
          $("#sendFundsAccount").appendTo("#sendFundsFormSlot");
          this.formName = 'standalone';
        } else if (!this.selectedAccount.isContract) {
          // Move to original location as temporary location
          $("#sendFundsAccount").appendTo("#sendFundsFormSlot");
          this.formName = 'ether';
          // Move combobox to final location
          this.$nextTick(() => {
            $("#sendFundsAccount").appendTo("#sendEtherFormSlot");
          });
        } else {
          // Move to original location as temporary location
          $("#sendFundsAccount").appendTo("#sendFundsFormSlot");
          this.formName = 'token';
          // Move combobox to final location
          this.$nextTick(() => {
            $("#sendFundsAccount").appendTo("#sendTokensFormSlot");
          });
        }

        if (this.receiver && this.receiver_type && this.amount) {
          this.$nextTick(() => {
            if (this.$refs.sendEtherForm) {
              this.$refs.sendEtherForm.$refs.autocomplete.selectItem(this.receiver, this.receiver_type);
              this.$refs.sendEtherForm.amount = Number(this.amount);
            } else if (this.$refs.sendTokensForm) {
              this.$refs.sendTokensForm.$refs.autocomplete.selectItem(this.receiver, this.receiver_type);
              this.$refs.sendTokensForm.amount = Number(this.amount);
            }
          });
        } else {
          this.$nextTick(() => {
            if (this.$refs.sendEtherForm) {
              this.$refs.sendEtherForm.init();
            } else if (this.$refs.sendTokensForm) {
              this.$refs.sendTokensForm.init();
            }
          });
        }
      });
    }
  },
  methods: {
    prepareSendForm(receiver, receiver_type, amount, contractAddress, keepDialogOpen) {
      if (this.selectedAccount) {
        this.receiver = receiver;
        this.receiver_type = receiver_type;
        this.amount = amount;
      } else {
        if (receiver && receiver_type && amount) {
          this.$emit("dialog-error", "Selected currency is not displayed switch your preferences");
        }

        if(!keepDialogOpen) {
          this.$emit("close");
        }
      }
    },
    addPendingTransaction(transaction) {
      if (!transaction) {
        console.debug("Pending transaction is empty");
        return;
      }

      this.error = null;

      const selectedAccount = this.selectedAccount;
      addTransaction(this.networkId,
        this.walletAddress,
        selectedAccount,
        [],
        transaction,
        null,
        null,
        () => this.$emit('success', selectedAccount),
        error => this.$emit('error', error));

      this.$emit('pending', transaction);
    }
  }
};
</script>

