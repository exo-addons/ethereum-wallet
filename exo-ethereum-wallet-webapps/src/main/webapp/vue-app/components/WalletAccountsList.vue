<template>
  <v-card>
    <v-container id="accountListContainer" fluid grid-list-lg>
      <v-layout
        row
        wrap>
        <v-spacer></v-spacer>
        <v-flex 
          v-for="(item, index) in accountsList"
          :key="index"
          class="accountItemContainer">

          <!-- Ether account actions -->
          <send-ether-modal
            :account="account"
            :balance="selectedItem && selectedItem.balance"
            :open="sendEtherModal"
            no-button
            @sent="addSendEtherTransaction"
            @close="sendEtherModal = false"
            @error="$emit('error', $event)" />
          <!-- Contract actions -->
          <send-tokens-modal
            :balance="selectedItem && selectedItem.balance"
            :ether-balance="selectedItem && selectedItem.etherBalance"
            :account="account"
            :contract="selectedItem && selectedItem.contract"
            :open="sendTokenModal"
            no-button
            @sent="addSendTokenTransaction($event, item)"
            @close="sendTokenModal = false"
            @error="$emit('error', $event)" />
          <delegate-tokens-modal
            :balance="selectedItem && selectedItem.balance"
            :ether-balance="selectedItem && selectedItem.etherBalance"
            :contract="selectedItem && selectedItem.contract"
            :open="delegateTokenModal"
            no-button
            @sent="addDelegateTokenTransaction($event, item)"
            @close="delegateTokenModal = false"
            @error="$emit('error', $event)" />
          <send-delegated-tokens-modal
            :ether-balance="selectedItem && selectedItem.etherBalance"
            :contract="selectedItem && selectedItem.contract"
            :has-delegated-tokens="true"
            :open="sendDelegatedTokenModal"
            no-button
            @close="sendDelegatedTokenModal = false"
            @error="$emit('error', $event)" />

          <v-hover>
            <v-card
              slot-scope="{ hover }"
              :class="`elevation-${hover ? 12 : 2}`"
              width="400px"
              height="210px">
  
              <v-card-title class="blue white--text">
                <v-icon dark>{{ item.icon }}</v-icon>

                <v-spacer></v-spacer>
                <span class="headline">{{ item.title }}</span>
                <v-spacer></v-spacer>
          
                <v-menu :ref="`walletAccountCard${index}`" :attach="`walletAccountCard${index}`" content-class="walletAccountMenu">
                  <v-btn slot="activator" dark icon>
                    <v-icon>more_vert</v-icon>
                  </v-btn>

                  <v-list>
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
              <v-card-title class="accountItemContent">
                <v-spacer></v-spacer>
                <div class="text-xs-center">
                  <h3 v-if="item.error" class="headline mb-0">{{ item.error }}</h3>
                  <h3 v-if="!item.error && (item.balanceFiat === 0 || item.balanceFiat)" class="headline mb-0">{{ `${item.balanceFiat} ${fiatSymbol}` }}</h3>
                  <h4 v-if="!item.error && (item.balance === 0 || item.balance)">{{ `${item.balance} ${item.symbol}` }}</h4>
                </div>
                <v-spacer></v-spacer>
                <v-btn icon class="mr-0" @click="$emit('account-details-selected', item)">
                  <v-icon>fa-angle-right</v-icon>
                </v-btn>
              </v-card-title>
            </v-card>
          </v-hover>
        </v-flex>
        <v-spacer></v-spacer>
      </v-layout>
    </v-container>
  </v-card>
</template>

<script>
import DelegateTokensModal from './DelegateTokensModal.vue';
import SendDelegatedTokensModal from './SendDelegatedTokensModal.vue';
import SendTokensModal from './SendTokensModal.vue';
import SendEtherModal from './SendEtherModal.vue';

import {addPendingTransactionToStorage, removePendingTransactionFromStorage} from '../WalletToken.js';
import {watchTransactionStatus} from '../WalletUtils.js';
import {addTransaction} from '../WalletEther.js';

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
    account: {
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
    addDelegateTokenTransaction(transaction, contract) {
      addPendingTransactionToStorage(this.networkId, this.account, contract.address, {
        from: transaction.from,
        to: transaction.to,
        value: transaction.value,
        hash: transaction.hash,
        timestamp: Date.now(),
        labelFrom: 'Delegated from',
        labelTo: 'Delegated to',
        icon: 'fa-users',
        pending: true
      });

      watchTransactionStatus(transaction.hash, (receipt, block) => {
        removePendingTransactionFromStorage(this.networkId, this.account, contract.address, transaction.hash);
      });
    },
    addSendTokenTransaction(transaction, contract) {
      addPendingTransactionToStorage(this.networkId, this.account, contract.address, {
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
        removePendingTransactionFromStorage(this.networkId, this.account, contract.address, transaction.hash);
      });
    },
    addSendEtherTransaction(transaction) {
      addTransaction(this.networkId,
        this.account,
        [],
        transaction);
    }
  }
};
</script>