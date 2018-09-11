<template>
  <v-flex v-if="Object.keys(transactions).length">
    <v-list two-line class="pt-0 pb-0">
      <template v-for="(item, index) in sortedTransaction">
        <v-list-tile :key="item.hash" avatar>
          <v-progress-circular v-if="item.pending" indeterminate color="primary" class="mr-4" />
          <v-list-tile-avatar v-else>
            <v-icon :color="item.color ? item.color: 'black'">{{ item.icon }}</v-icon>
          </v-list-tile-avatar>
          <v-list-tile-content>
            <v-list-tile-title>
              <span>{{ item.titlePrefix }}</span>
              <v-chip v-if="item.avatar" :title="item.displayAddress" class="mt-0 mb-0" small>
                <v-avatar size="23px !important">
                  <img :src="item.avatar">
                </v-avatar>
                <span v-html="item.displayName"></span>
              </v-chip>
              <wallet-address v-else :value="item.displayName" />
            </v-list-tile-title>
            <v-list-tile-sub-title>{{ item.amount }}</v-list-tile-sub-title>
          </v-list-tile-content>
          <v-list-tile-action v-if="!item.pending && item.date">
            <v-list-tile-action-text>{{ item.date ? item.date.toLocaleDateString() : '' }} - {{ item.date ? item.date.toLocaleTimeString() : '' }}</v-list-tile-action-text>
          </v-list-tile-action>
        </v-list-tile>
        <v-divider v-if="index + 1 < sortedTransaction.length" :key="index"></v-divider>
      </template>
    </v-list>
  </v-flex>
  <v-flex v-else-if="!loading" class="text-xs-center">
    <v-chip color="white">
      <span>No transactions</span>
    </v-chip>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import {searchFullName, getContactFromStorage} from '../WalletAddressRegistry.js';
import {watchTransactionStatus} from '../WalletUtils.js';
import {addPendingTransactionToStorage, removePendingTransactionFromStorage, getPendingTransactionFromStorage} from '../WalletToken.js';

export default {
  components: {
    WalletAddress
  },
  props: {
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    contract: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      refreshIndex: 1,
      latestBlockNumber: 0,
      latestDelegatedBlockNumber: 0,
      transactions: {},
      loading: true
    };
  },
  computed: {
    sortedTransaction() {
      // A trick to force update computed list
      if (!this.refreshIndex) {
        return {};
      }
      // Freeze the list of keys to treat here while the transactions list
      // is changed
      const transactions = Object.assign({}, this.transactions);
      const sortedTransactions = {};
      Object.keys(transactions)
        .sort((key1, key2) => transactions[key2].date - transactions[key1].date)
        .forEach(key => {
          sortedTransactions[key] = transactions[key];
        });
      return sortedTransactions;
    }
  },
  watch: {
    contract(contract) {
      if (contract) {
        this.init();
      }
    }
  },
  created() {
    if (this.contract) {
      this.init();
    }
  },
  methods: {
    init() {
      if (this.contract) {
        this.refreshNewwestTransactions();
      }
    },
    refreshNewwestTransactions() {
      this.loading = true;
      this.$emit("loading");
      return this.refreshPendingTransactions()
        .then(() => this.refreshTransferList())
        .then(() => this.refreshApprovalList())
        .then(() => this.$emit("end-loading"))
        .finally(() => this.loading = false);
    },
    refreshPendingTransactions() {
      const pendingTransactions = getPendingTransactionFromStorage(this.networkId, this.account, this.contract._address.toLowerCase());
      if (!pendingTransactions || !Object.keys(pendingTransactions).length) {
        return Promise.resolve({});
      }
      return Promise.resolve(pendingTransactions)
        .then(pendingTransactions => {
          Object.keys(pendingTransactions).forEach(transactionHash => {
            const transaction = pendingTransactions[transactionHash]; 
            this.addTransactionToList(
              transaction.from,
              transaction.to,
              transaction.value,
              transaction.hash,
              transaction.timestamp,
              transaction.labelFrom,
              transaction.labelTo,
              transaction.icon,
              transaction.pending,
              () => {
                this.$emit('refresh-balance');
              });
          });
          this.$forceUpdate();
          return pendingTransactions;
        });
    },
    refreshTransferList() {
      return this.contract.getPastEvents("Transfer", {
        fromBlock: this.latestBlockNumber + 1,
        toBlock: 'latest'
      }).then((events) => {
        if (events && events.length) {
          for (let i = 0; i < events.length; i++) {
            const event = events[i];

            this.latestBlockNumber = Math.max(this.latestBlockNumber, event.blockNumber);
            if (event.returnValues && event.returnValues._from && event.returnValues._to) {
              const from = event.returnValues._from.toLowerCase();
              const to = event.returnValues._to.toLowerCase();

              if (to === this.account || from === this.account) {
                window.localWeb3.eth.getBlock(event.blockNumber, false)
                  .then(block => {
                    this.addTransactionToList(
                      event.returnValues._from.toLowerCase(),
                      event.returnValues._to.toLowerCase(),
                      parseFloat(event.returnValues._value),
                      event.transactionHash,
                      block ? block.timestamp * 1000 : null,
                      'Received from',
                      'Sent to',
                      'fa-exchange-alt');
                  });
              }
            }
          }
        }
        return true;
      }).catch(e => {
        this.$emit("error", `Error loading contract transactions: ${e}`);
        return true;
      });
    },
    refreshApprovalList() {
      return this.contract.getPastEvents("Approval", {
        fromBlock: this.latestDelegatedBlockNumber + 1,
        toBlock: 'latest'
      }).then((events) => {
        if (events && events.length) {
          for (let i = 0; i < events.length; i++) {
            const event = events[i];

            this.latestDelegatedBlockNumber = Math.max(this.latestDelegatedBlockNumber, event.blockNumber);
            if (event.returnValues && event.returnValues._spender && event.returnValues._owner) {
              const from = event.returnValues._owner.toLowerCase();
              const to = event.returnValues._spender.toLowerCase();
              if (to === this.account || from === this.account) {
                window.localWeb3.eth.getBlock(event.blockNumber, false)
                  .then(block => {
                    const transactionDetails = this.addTransactionToList(
                      event.returnValues._owner.toLowerCase(),
                      event.returnValues._spender.toLowerCase(),
                      parseFloat(event.returnValues._value),
                      event.transactionHash,
                      block ? block.timestamp * 1000 : null,
                      'Delegated from',
                      'Delegated to',
                      'fa-users');
                    if (transactionDetails && transactionDetails.isReceiver) {
                      this.$emit('has-delegated-tokens');
                    }
                  });
              }
            }
          }
        }
        return true;
      }).catch(e => {
        console.debug("Error occurred while retrieving contract transactions", e);
        this.$emit("error", `Error loading contract transactions: ${e}`);
        return true;
      });
    },
    addDelegateTransaction(transaction) {
      this.addTransactionToList(
        transaction.from,
        transaction.to,
        transaction.value,
        transaction.hash,
        Date.now(),
        'Delegated from',
        'Delegated to',
        'fa-users',
        transaction.pending,
        () => {
          this.$emit('refresh-balance');
        });
      this.$forceUpdate();
    },
    addSendTransaction(transaction) {
      this.addTransactionToList(
        transaction.from,
        transaction.to,
        transaction.value,
        transaction.hash,
        Date.now(),
        'Received from',
        'Sent to',
        'fa-exchange-alt',
        transaction.pending,
        () => {
          this.$emit('refresh-balance');
        });
      this.$forceUpdate();
    },
    addTransactionToList(from, to, amount, transactionHash, timestamp, labelFrom, labelTo, icon, pending, pendingTransactionSuccess) {
      const isReceiver = to === this.account;
      const displayedAddress = isReceiver ? from : to;
      const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');
      const transactionDetails = {
        hash: transactionHash,
        titlePrefix: isReceiver ? labelFrom: labelTo,
        displayName: contactDetails.name ? contactDetails.name : displayedAddress,
        avatar: contactDetails.avatar,
        name: null,
        isReceiver: isReceiver,
        color: isReceiver ? 'green' : 'red',
        icon: icon,
        date: timestamp ? new Date(timestamp) : null,
        pending: pending,
        amount: amount
      };

      // Push transactions here in Promise to apply order after adding date
      // (Vue will not trigger computed values when changing attribute of an object inside an array)
      this.transactions[transactionDetails.hash] = transactionDetails;
      this.refreshIndex++;
      if (!contactDetails || !contactDetails.name) {
        return searchFullName(displayedAddress)
          .then(item => {
            if (item && item.name && item.name.length) {
              transactionDetails.displayName = item.name;
              transactionDetails.avatar = item.avatar;
              transactionDetails.name = item.id;

              // Force update list
              this.refreshIndex++;
            }
            return transactionDetails;
          });
      } else {
        // Force update list
        this.refreshIndex++;
      }
      if (pending) {
        addPendingTransactionToStorage(this.networkId, this.account, this.contract._address.toLowerCase(), {
          from: from,
          to: to,
          value: amount,
          hash: transactionHash,
          timestamp: timestamp,
          labelFrom: labelFrom,
          labelTo: labelTo,
          icon: icon,
          pending: pending
        });

        watchTransactionStatus(transactionHash, (receipt, block) => {
          this.addTransactionToList(from, to, amount, transactionHash, block.timestamp * 1000, labelFrom, labelTo, icon, false);
          removePendingTransactionFromStorage(this.networkId, this.account, this.contract._address.toLowerCase(), transactionHash);
          if (pendingTransactionSuccess) {
            pendingTransactionSuccess();
          }
        });
      }
      return transactionDetails;
    }
  }
};
</script>