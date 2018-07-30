<template>
  <v-flex v-if="transactions.length" xs12>
    <v-card class="card--flex-toolbar">
      <v-list two-line>
        <template v-for="(item) in transactions">
          <v-list-tile :key="item.hash" avatar>
            <v-list-tile-avatar>
              <v-icon :class="item.color" dark>{{ item.icon }}</v-icon>
            </v-list-tile-avatar>
            <v-list-tile-content>
              <v-list-tile-title v-html="item.title"></v-list-tile-title>
              <v-list-tile-sub-title>{{ item.amount }} (Fee: {{ item.fee }})</v-list-tile-sub-title>
            </v-list-tile-content>
          </v-list-tile>
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
    }
  },
  data () {
    return {
      transactionsPerPage: 10,
      transactionsToLoad: 10,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      transactions: []
    };
  },
  watch: {
    account(account) {
      if (account) {
        this.transactions = [];
        this.loadedBlocks = 0;
        this.init();
      }
    }
  },
  created() {
    if (this.account) {
      this.init();
    }
  },
  methods: {
    init() {
      window.localWeb3.eth.getBlockNumber()
        .then(lastBlockNumber => window.localWeb3.eth.getBlock(lastBlockNumber, true))
        .then(this.addBlockTransactions)
        .catch(e => console.error(`Error retrieving last block`, e));
    },
    addBlockTransactions(block) {
      if (!block) {
        throw new Error("Block not found");
      }
      this.loadedBlocks++;
      if (block.number === 0
          || this.transactionsToLoad <= this.transactions.length
          || this.loadedBlocks > this.maxBlocksToLoad) {
        return;
      }
      if (block.transactions && block.transactions.length) {
        const thiss = this;
        block.transactions.forEach(transaction => {
          if (transaction.to && transaction.from && (transaction.to.toLowerCase() === thiss.account.toLowerCase() || transaction.from && transaction.from.toLowerCase() === thiss.account.toLowerCase())) {
            const transactionFeeInWei = transaction.gas * transaction.gasPrice;
            const transactionFeeInEth = window.localWeb3.utils.fromWei(transactionFeeInWei.toString(), 'ether');
            console.log(transaction);
            thiss.transactions.push({
              hash: transaction.hash,
              title: transaction.to && transaction.to.toLowerCase() === thiss.account.toLowerCase() ? `Received from ${transaction.from}`: `Sent to ${transaction.to}`,
              color: transaction.to && transaction.to.toLowerCase() === thiss.account.toLowerCase() ? 'green' : 'red',
              icon: 'fa-exchange-alt',
              amount: window.localWeb3.utils.fromWei(transaction.value, 'ether'),
              fee: transactionFeeInEth
            });
          }
        });
      }
      window.localWeb3.eth.getBlock(block.parentHash, true)
        .then(this.addBlockTransactions)
        .catch((e) => console.error(`Error retrieving block: ${block.parentHash}`, e));
    }
  }
};
</script>