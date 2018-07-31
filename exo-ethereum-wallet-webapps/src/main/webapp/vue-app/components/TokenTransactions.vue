<template>
  <v-flex v-if="transactions.length" xs12>
    <v-card class="card--flex-toolbar">
      <v-list two-line>
        <template v-for="(item, index) in sortedTransaction">
          <v-list-tile :key="item.hash" avatar>
            <v-list-tile-avatar>
              <v-icon :class="item.color" dark>{{ item.icon }}</v-icon>
            </v-list-tile-avatar>
            <v-list-tile-content>
              <v-list-tile-title v-html="item.title"></v-list-tile-title>
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
            this.transactions.push({
              hash: res.transactionHash,
              title: res.args._to === this.account ? `Received from ${res.args._from}`: `Sent to ${res.args._to}`,
              color: res.args._to === this.account ? 'green' : 'red',
              icon: 'fa-exchange-alt',
              amount: res.args._value.toNumber(),
              date: new Date(timestamp * 1000)
            });
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
              this.transactions.push({
                hash: res.transactionHash,
                title: res.args._spender === this.account ? `Delegated from ${res.args._owner}`: `Delegated to ${res.args._spender}`,
                color: res.args._spender === this.account ? 'green' : 'red',
                icon: 'fa-users',
                amount: res.args._value.toNumber(),
                date: new Date(timestamp * 1000)
              });
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