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
      @receiver-selected="receiver = $event.id; receiverType = $event.type"
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
      @receiver-selected="receiver = $event.id; receiverType = $event.type"
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
import {markFundRequestAsSent} from '../WalletUtils.js';

export default {
  components: {
    SendEtherForm,
    SendTokensForm
  },
  props: {
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    addPendingToReceiver: {
      type: Boolean,
      default: function() {
        return false;
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
      receiverType: null,
      notificationId: null,
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

        this.$nextTick(() => {
          if (this.$refs.sendEtherForm) {
            this.$refs.sendEtherForm.init();
            if (this.receiver) {
              this.$refs.sendEtherForm.$refs.autocomplete.selectItem(this.receiver, this.receiverType);
              this.$refs.sendEtherForm.amount = Number(this.amount);
            }
          } else if (this.$refs.sendTokensForm) {
            this.$refs.sendTokensForm.init();
            if (this.receiver) {
              this.$refs.sendTokensForm.$refs.autocomplete.selectItem(this.receiver, this.receiverType);
              this.$refs.sendTokensForm.amount = Number(this.amount);
            }
          }
        });
      });
    }
  },
  methods: {
    prepareSendForm(receiver, receiverType, amount, contractAddress, notificationId, keepDialogOpen) {
      if (this.selectedAccount) {
        this.receiver = receiver;
        this.receiverType = receiverType;
        this.notificationId = notificationId;
        if(amount) {
          this.amount = amount;
        }
      } else {
        if (receiver && receiverType) {
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

      // This assignement should be kept to avoid conflit in parallet execution
      const selectedAccount = this.selectedAccount;
      const recipient = transaction.to.toLowerCase();
      transaction.addLoadingToRecipient = this.addPendingToReceiver;
      addTransaction(this.networkId,
        this.walletAddress,
        selectedAccount,
        [],
        transaction,
        null,
        null,
        () => this.$emit('success', selectedAccount, recipient),
        error => this.$emit('error', error));

      this.$emit('pending', transaction);

      if (this.notificationId) {
        markFundRequestAsSent(this.notificationId);
      }
    }
  }
};
</script>

