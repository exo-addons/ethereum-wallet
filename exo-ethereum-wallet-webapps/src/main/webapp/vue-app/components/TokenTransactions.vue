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
              <v-list-tile-action-text>{{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}</v-list-tile-action-text>
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
    contract: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      transactions: []
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
      this.contract.Transfer({}, { fromBlock: 0, toBlock: 'latest' }, (error, res) => {
        if (error) {
          console.error("Error listing Transfer transactions of contract", error);
          this.$emit("error", `Error listing Transfer transactions of contract: ${error}`);
          return;
        }
        if (res.args._to === this.account || res.args._from === this.account) {
          window.localWeb3.eth.getBlock(res.blockHash, false)
            .then(block => block.timestamp)
            .then(timestamp => {
              const isReceiver = res.args._to === this.account;
              const displayedAddress = isReceiver ? res.args._from : res.args._to;
              const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');
              const transactionDetails = {
                hash: res.transactionHash,
                titlePrefix: isReceiver ? `Received from`: `Sent to`,
                displayName: contactDetails.name ? contactDetails.name : displayedAddress,
                avatar: contactDetails.avatar,
                name: null,
                color: isReceiver ? 'green' : 'red',
                icon: 'fa-exchange-alt',
                amount: res.args._value.toNumber(),
                date: new Date(timestamp * 1000)
              };
              this.transactions.push(transactionDetails);
              if (!contactDetails || !contactDetails.name) {
                searchFullName(displayedAddress)
                  .then(item => {
                    if (item && item.name && item.name.length) {
                      transactionDetails.displayName = item.name;
                      transactionDetails.avatar = item.avatar;
                      transactionDetails.name = item.id;
                      this.$forceUpdate();
                    }
                  });
              }
              this.$emit("loaded");
            })
            .catch(error => {
              this.$emit("error", `Error listing Transfer transactions of contract: ${error}`);
            });
        }
      });

      this.contract.Approval({}, { fromBlock: 0, toBlock: 'latest' }, (error, res) => {
        if (error) {
          console.error("Error listing Approval transactions of contract", error);
          this.$emit("error", `Error listing Approval transactions of contract: ${error}`);
          return;
        }
        if (res.args._spender === this.account || res.args._owner === this.account) {
          if (res.args._spender === this.account) {
            this.$emit("has-delegated-tokens");
          }
          window.localWeb3.eth.getBlock(res.blockHash, false)
            .then(block => block.timestamp)
            .then(timestamp => {
              const isReceiver = res.args._spender === this.account;
              const displayedAddress = isReceiver ? res.args._owner : res.args._spender;
              const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');
              const transactionDetails = {
                hash: res.transactionHash,
                titlePrefix: isReceiver ? `Delegated from`: `Delegated to`,
                displayName: contactDetails.name ? contactDetails.name : displayedAddress,
                avatar: contactDetails.avatar,
                name: null,
                color: isReceiver ? 'green' : 'red',
                icon: 'fa-users',
                amount: res.args._value.toNumber(),
                date: new Date(timestamp * 1000)
              };
              this.transactions.push(transactionDetails);
              if (!contactDetails || !contactDetails.name) {
                searchFullName(displayedAddress)
                  .then(item => {
                    if (item && item.name && item.name.length) {
                      transactionDetails.displayName = item.name;
                      transactionDetails.avatar = item.avatar;
                      transactionDetails.name = item.id;
                      this.$forceUpdate();
                    }
                  });
              }
              this.$emit("loaded");
            })
            .catch(error => {
              this.$emit("error", `Error listing Approval transactions of contract: ${error}`);
            });
        }
      });
    }
  }
};
</script>