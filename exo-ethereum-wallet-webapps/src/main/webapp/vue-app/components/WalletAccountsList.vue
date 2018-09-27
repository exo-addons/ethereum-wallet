<template>
  <v-card flat>
    <!-- Ether account actions -->
    <send-ether-modal
      :account="walletAddress"
      :balance="selectedItem && selectedItem.balance"
      :open="sendEtherModal"
      no-button
      @sent="addSendEtherTransaction"
      @close="sendEtherModal = false"
      @error="$emit('error', $event)" />

    <!-- Contract actions -->
    <send-tokens-modal
      :account="walletAddress"
      :contract-details="selectedItem"
      :open="sendTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="sendTokenModal = false"
      @error="$emit('error', $event)" />
    <delegate-tokens-modal
      :contract="selectedItem && selectedItem.contract"
      :contract-details="selectedItem"
      :open="delegateTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="delegateTokenModal = false"
      @error="$emit('error', $event)" />
    <send-delegated-tokens-modal
      :contract="selectedItem && selectedItem.contract"
      :contract-details="selectedItem"
      :open="sendDelegatedTokenModal"
      no-button
      @sent="addSendTokenTransaction"
      @close="sendDelegatedTokenModal = false"
      @error="$emit('error', $event)" />

    <v-container v-if="accountsList.length" id="accountListContainer" flat fluid grid-list-lg>
      <v-layout
        row
        wrap>
        <div 
          v-for="(item, index) in accountsList"
          :key="index"
          class="accountItemContainer">

          <v-hover>
            <v-card
              slot-scope="{ hover }"
              :class="`elevation-${hover ? 9 : 2}`"
              width="400px"
              max-width="100%"
              height="210px">

              <v-card-title dark class="primary">
                <v-icon :class="!item.error && 'clickable'" dark @click="!item.error && $emit('account-details-selected', item)">{{ item.icon }}</v-icon>

                <v-spacer />
                <v-card-sub-title :class="item.error ? 'errorHeadline' : 'headline clickable'" @click="!item.error && $emit('account-details-selected', item)">{{ item.title }}</v-card-sub-title>
                <v-spacer v-if="!item.error" />

                <v-menu v-if="!item.error" :ref="`walletAccountCard${index}`" :attach="`.walletAccountMenuItem${index}`" :class="`walletAccountMenuItem${index}`" content-class="walletAccountMenu">
                  <v-btn slot="activator" dark icon>
                    <v-icon>more_vert</v-icon>
                  </v-btn>

                  <v-list class="pt-0 pb-0">
                    <v-list-tile v-if="!item.isContract && item.balance && item.balance !== '0'" @click="selectedItem = item; sendEtherModal = true">
                      <v-list-tile-title>
                        Send Ether
                      </v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="item.isContract && item.balance > 0 && item.etherBalance > 0" @click="selectedItem = item; sendTokenModal = true">
                      <v-list-tile-title>
                        Send token
                      </v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="item.isContract && item.balance > 0 && item.etherBalance > 0" @click="selectedItem = item; delegateTokenModal = true">
                      <v-list-tile-title>
                        Delegate tokens
                      </v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="item.isContract && item.balance > 0 && item.etherBalance > 0" @click="selectedItem = item; sendDelegatedTokenModal = true">
                      <v-list-tile-title>
                        Send delegated tokens
                      </v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="!isSpace && item.isContract && !item.isDefault" @click="deleteContract(item, $event)">
                      <v-list-tile-title>
                        Remove from list
                      </v-list-tile-title>
                    </v-list-tile>
                  </v-list>
                </v-menu>
              </v-card-title>
              <v-card-title :class="!item.error && 'clickable'" class="accountItemContent" @click="!item.error && $emit('account-details-selected', item)">
                <v-spacer></v-spacer>
                <div class="text-xs-center">
                  <h4 v-if="item.error" class="mb-0">{{ item.error }}</h4>
                  <h3 v-if="!item.error && (item.balanceFiat === 0 || item.balanceFiat)" class="headline mb-0">{{ `${item.balanceFiat} ${fiatSymbol}` }}</h3>
                  <h4 v-if="!item.error && (item.balance === 0 || item.balance)">{{ `${item.balance} ${item.symbol}` }}</h4>
                </div>
                <v-spacer v-if="!item.error" />
                <v-btn v-if="!item.error" icon class="mr-2" @click="!item.error && $emit('account-details-selected', item)">
                  <v-icon>fa-angle-right</v-icon>
                </v-btn>
              </v-card-title>
            </v-card>
          </v-hover>
        </div>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendTokensModal from './SendTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import {deleteContractFromStorage} from '../WalletToken.js';
import {watchTransactionStatus} from '../WalletUtils.js';
import {addTransaction} from '../WalletTransactions.js';

export default {
  components: {
    DelegateTokensModal,
    SendDelegatedTokensModal,
    SendTokensModal,
    SendEtherModal
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    fiatSymbol: {
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
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    }
  },
  data() {
    return {
      sendEtherModal: false,
      sendTokenModal: false,
      delegateTokenModal: false,
      sendDelegatedTokenModal: false,
      selectedItem: null
    };
  },
  computed: {
    accountsList() {
      // A trick to force Refresh list
      if (!this.refreshIndex) {
        return;
      }
      const accountsList = [];
      Object.keys(this.accountsDetails).forEach(key => accountsList.push(this.accountsDetails[key]));
      return accountsList;
    }
  },
  methods: {
    addSendEtherTransaction(transaction) {
      addTransaction(this.networkId,
        this.walletAddress,
        this.accountsDetails[this.account],
        [],
        transaction);

      this.$emit('transaction-sent', transaction);
    },
    addSendTokenTransaction(transaction, contract) {
      addTransaction(this.networkId,
        this.walletAddress,
        contract,
        [],
        transaction);

      this.$emit('transaction-sent', transaction);
    },
    deleteContract(item, event) {
      if(deleteContractFromStorage(this.walletAddress, this.networkId, item.address)) {
        delete this.accountsDetails[item.address];
        this.$emit('refresh-contracts');
      }
      event.preventDefault();
      event.stopPropagation();
    }
  }
};
</script>