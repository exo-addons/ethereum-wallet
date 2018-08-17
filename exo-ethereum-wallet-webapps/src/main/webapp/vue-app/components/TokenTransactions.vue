<template>
  <v-flex v-if="transactions.length">
    <v-list two-line class="pt-0 pb-0">
      <template v-for="(item, index) in sortedTransaction">
        <v-list-tile :key="item.hash" avatar>
          <v-list-tile-avatar>
            <v-icon :class="item.color" dark>{{ item.icon }}</v-icon>
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
              <code v-else>{{ item.displayName }}</code>
            </v-list-tile-title>
            <v-list-tile-sub-title>{{ item.amount }}</v-list-tile-sub-title>
          </v-list-tile-content>
          <v-list-tile-action>
            <v-list-tile-action-text>{{ item.date ? item.date.toLocaleDateString() : '' }} - {{ item.date ? item.date.toLocaleDateString() : '' }}</v-list-tile-action-text>
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
import {searchFullName, getContactFromStorage} from '../WalletAddressRegistry.js';

export default {
  props: {
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
      latestBlockNumber: 0,
      latestDelegatedBlockNumber: 0,
      transactions: [],
      loading: true
    };
  },
  computed: {
    sortedTransaction() {
      return this.transactions.slice(0).sort((t1, t2) => t2.date - t1.date);
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
      return this.refreshTransferList()
        .then(() => this.refreshApprovalList())
        .then(() => this.$emit("end-loading"))
        .finally(() => this.loading = false);
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
              this.addTransactionToList(
                event.returnValues._from.toLowerCase(),
                event.returnValues._to.toLowerCase(),
                parseFloat(event.returnValues._value),
                event.transactionHash,
                event.blockHash,
                'Received from',
                'Sent to',
                'fa-exchange-alt');
            }
          }
        }
        return this.transactions;
      }).catch(e => {
        this.$emit("error", `Error loading contract transactions: ${e}`);
        return this.transactions;
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
              this.addTransactionToList(
                event.returnValues._owner.toLowerCase(),
                event.returnValues._spender.toLowerCase(),
                parseFloat(event.returnValues._value),
                event.transactionHash,
                event.blockHash,
                'Delegated from',
                'Delegated to',
                'fa-users')
                .then(transactionDetails => {
                  if (transactionDetails && transactionDetails.isReceiver) {
                    this.$emit('has-delegated-tokens');
                  }
                });
            }
          }
        }
        return this.transactions;
      }).catch(e => {
        console.debug("Error occurred while retrieving contract transactions", e);
        this.$emit("error", `Error loading contract transactions: ${e}`);
        return this.transactions;
      });
    },
    addTransactionToList(from, to, amount, transactionHash, blockHash, labelFrom, labelTo, icon) {
      let transactionDetails = null;
      if (to === this.account || from === this.account) {
        const isReceiver = to === this.account;
        const displayedAddress = isReceiver ? from : to;
        const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');
        transactionDetails = {
          hash: transactionHash,
          titlePrefix: isReceiver ? labelFrom: labelTo,
          displayName: contactDetails.name ? contactDetails.name : displayedAddress,
          avatar: contactDetails.avatar,
          name: null,
          isReceiver: isReceiver,
          color: isReceiver ? 'green' : 'red',
          icon: icon,
          amount: amount
        };

        return window.localWeb3.eth.getBlock(blockHash, false)
          .then(block => block.timestamp)
          .then(timestamp => {
            transactionDetails.date = new Date(timestamp * 1000);
            // Push transactions here in Promise to apply order after adding date
            // (Vue will not trigger computed values when changing attribute of an object inside an array)
            this.transactions.push(transactionDetails);
            this.$forceUpdate();
            if (!contactDetails || !contactDetails.name) {
              return searchFullName(displayedAddress)
                .then(item => {
                  if (item && item.name && item.name.length) {
                    transactionDetails.displayName = item.name;
                    transactionDetails.avatar = item.avatar;
                    transactionDetails.name = item.id;
                    this.$forceUpdate();
                  }
                  return transactionDetails;
                });
            }
            return transactionDetails;
          })
          .catch(error => {
            console.debug("Web3 eth.getBlock method - error", error);
            this.$emit("error", `Error listing Transfer transactions of contract: ${error}`);
            return transactionDetails;
          });
      }
      return Promise.resolve(null);
    }
  }
};
</script>