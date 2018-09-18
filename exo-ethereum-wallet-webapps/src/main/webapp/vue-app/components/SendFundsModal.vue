<template>
  <v-dialog v-model="dialog" :disabled="disabled" content-class="uiPopup" width="500px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <v-btn v-if="icon" slot="activator" :disabled="disabled" class="bottomNavigationItem" title="Send funds" flat value="send">
      <span>Send</span>
      <v-icon>send</v-icon>
    </v-btn>
    <button v-else slot="activator" :disabled="disabled" class="btn btn-primary mr-1 mt-2">
      Send Funds
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send Funds</span>
      </div>

      <v-card-title id="sendFundsAccount" primary-title class="pb-0">
        <v-combobox
          v-model="selectedOption"
          :items="accountsList"
          attach="#sendFundsAccount"
          absolute
          label="Select currency"
          placeholder="Select a currency to use for requesting funds" />
      </v-card-title>

      <send-ether-form v-if="selectedAccount && !selectedAccount.isContract"
                       ref="sendEtherForm"
                       :account="walletAddress"
                       :balance="selectedAccount.balance"
                       @sent="addPendingTransaction($event)"
                       @close="dialog = false" />

      <send-tokens-form v-if="selectedAccount && selectedAccount.isContract"
                        ref="sendTokensForm"
                        :account="walletAddress"
                        :contract="selectedAccount.contract"
                        :balance="selectedAccount.balance"
                        :ether-balance="selectedAccount.etherBalance"
                        @sent="addPendingTransaction($event)"
                        @close="dialog = false" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendEtherForm from './SendEtherForm.vue';
import SendTokensForm from './SendTokensForm.vue';

import {watchTransactionStatus} from '../WalletUtils.js';
import {addTransaction} from '../WalletEther.js';
import {addPendingTransactionToStorage, removePendingTransactionFromStorage} from '../WalletToken.js';

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
  },
  methods: {
    prepareSendForm(receiver, receiver_type, amount, contractAddress) {
      if (!this.accountsList || !this.accountsList.length) {
        console.warn("prepareSendForm error -  no accounts found");
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

        if (this.selectedOption) {
          this.$nextTick(() => {
            if (this.$refs.sendTokensForm) {
              this.$refs.sendTokensForm.$refs.autocomplete.selectItem(receiver, receiver_type);
              this.$refs.sendTokensForm.amount = Number(amount);
            }
          });
        } else {
          this.dialog = false;
        }
      } else {
        this.selectedOption = this.accountsList[0];
        this.$nextTick(() => {
          if (this.$refs.sendEtherForm) {
            this.$refs.sendEtherForm.$refs.autocomplete.selectItem(receiver, receiver_type);
            this.$refs.sendEtherForm.amount = Number(amount);
          }
        });
      }
    },
    addPendingTransaction(transaction) {
      if (!transaction) {
        console.debug("Pending transaction is empty");
        return;
      }

      if (transaction.type === 'sendEther') {
        addTransaction(this.networkId,
          this.walletAddress,
          [],
          transaction,
          null,
          null,
          () => this.$emit('success'),
          error => this.$emit('error', error));
      } else if (transaction.type === 'sendToken') {
        const contractAddress = this.selectedAccount.address;

        if (contractAddress) {
          addPendingTransactionToStorage(this.networkId, this.walletAddress, contractAddress, {
            from: transaction.from,
            to: transaction.to,
            value: transaction.value,
            hash: transaction.hash,
            timestamp: Date.now(),
            labelFrom: 'Received from',
            labelTo: 'Sent to',
            icon: 'fa-exchange-alt',
            pending: true
          });

          watchTransactionStatus(transaction.hash, (receipt, block) => {
            removePendingTransactionFromStorage(this.networkId, this.account, contractAddress, transaction.hash);
            if (receipt) {
              this.$emit('success');
            } else {
              this.$emit('error', 'Token transaction has failed');
            }
          });
        } else {
          console.debug("Transaction status check failed: no contract address was found");
        }
      }

      this.$emit('pending', transaction);
    }
  }
};
</script>

