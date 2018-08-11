<template>
  <v-flex v-if="transactions.length">
    <v-card class="card--flex-toolbar">
      <v-list two-line class="pt-0 pb-0">
        <template v-for="(item, index) in sortedTransaction">
          <v-list-tile :key="item.hash" avatar>
            <v-list-tile-avatar>
              <v-icon :class="item.color" dark>{{ item.icon }}</v-icon>
            </v-list-tile-avatar>
            <v-list-tile-content>
              <v-list-tile-title>
                <span>{{ item.titlePrefix }}</span>
                <v-chip v-if="item.avatar" :alt="item.name" class="mt-0 mb-0" small>
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
    </v-card>
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
    newWeb3Contract: {
      type: Object,
      default: function() {
        return {};
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
      transactions: [],
      loading: false
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
        this.refreshList();
        const thiss = this;
      }
    },
    refreshList() {
      this.transactions = [];

      this.loading = true;

      this.newWeb3Contract.getPastEvents("Transfer", {
        fromBlock: 0,
        toBlock: 'latest'
      }).then((events) => {
        if (events && events.length) {
          for (let i = 0; i < events.length; i++) {
            const event = events[i];
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
      }).catch(e => {
        console.debug("Error loading contract transactions using new Web3", e);

        // Fallback to use Truffle
        this.contract.Transfer({}, { fromBlock: 0, toBlock: 'latest' }, (error, res) => {
          if (error) {
            console.error("Error listing Transfer transactions of contract", error);
            this.$emit("error", `Error listing Transfer transactions of contract: ${error}`);
            return;
          }
          this.addTransactionToList(
            res.args._from.toLowerCase(), 
            res.args._to.toLowerCase(), 
            res.args._value.toNumber(), 
            res.transactionHash,
            res.blockHash,
            'Received from',
            'Sent to',
            'fa-exchange-alt');
        });
      });

      this.newWeb3Contract.getPastEvents("Approval", {
        fromBlock: 0,
        toBlock: 'latest'
      }).then((events) => {
        if (events && events.length) {
          for (let i = 0; i < events.length; i++) {
            const event = events[i];
            
            if (event.returnValues && event.returnValues._spender && event.returnValues._owner) {
              this.addTransactionToList(
                event.returnValues._owner.toLowerCase(),
                event.returnValues._spender.toLowerCase(),
                parseFloat(event.returnValues._value),
                event.transactionHash,
                event.blockHash,
                'Delegated from',
                'Delegated to',
                'fa-users');
            }
          }
        }
      }).catch(e => {
        // Can't intercept event when all events are loaded with Truffle
        this.loading = false;

        console.debug("Error loading contract transactions using new Web3", e);
        // Fallback to use Truffle
        this.contract.Approval({}, { fromBlock: 0, toBlock: 'latest' }, (error, res) => {
          if (error) {
            console.error("Error listing Approval transactions of contract", error);
            this.$emit("error", `Error listing Approval transactions of contract: ${error}`);
            return;
          }
          this.addTransactionToList(
            res.args._owner.toLowerCase(), 
            res.args._spender.toLowerCase(), 
            res.args._value.toNumber(), 
            res.transactionHash,
            res.blockHash,
            'Delegated from',
            'Delegated to',
            'fa-exchange-alt');
        });
      });
    },
    addTransactionToList(from, to, amount, transactionHash, blockHash, labelFrom, labelTo, icon) {
      if (to === this.account || from === this.account) {
        const isReceiver = to === this.account;
        const displayedAddress = isReceiver ? from : to;
        const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');
        const transactionDetails = {
          hash: transactionHash,
          titlePrefix: isReceiver ? labelFrom: labelTo,
          displayName: contactDetails.name ? contactDetails.name : displayedAddress,
          avatar: contactDetails.avatar,
          name: null,
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
            this.$emit("loaded");
          })
          .catch(error => {
            console.debug("Web3 eth.getBlock method - error", error);
            this.$emit("error", `Error listing Transfer transactions of contract: ${error}`);
            return transactionDetails;
          });
      }
    }
  }
};
</script>