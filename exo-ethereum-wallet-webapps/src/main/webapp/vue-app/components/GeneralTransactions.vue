<template>
  <v-flex xs12>
    <v-card class="card--flex-toolbar">
      <v-data-table :headers="headers" :items="transactions" hide-actions class="elevation-1">
        <template slot="items" slot-scope="props">
          <td class="text-xs-left">{{ props.item.to }}</td>
          <td class="text-xs-left">{{ props.item.from }}</td>
          <td class="text-xs-left">{{ props.item.amount }}</td>
        </template>
      </v-data-table>
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
      headers: [
        { align: 'left', text: 'To', value: 'to' },
        { align: 'left', text: 'From', value: 'from' },
        { align: 'left', text: 'Amount', value: 'amount' }
      ],
      transactionsPerPage: 10,
      transactionsToLoad: 10,
      transactionCount: 0,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      transactions: []
    };
  },
  watch: {
    account(account) {
      if (account) {
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
      window.localWeb3.eth.getTransactionCount(this.account)
        .then(transactionCount => this.transactionCount = transactionCount)
        .then(() => window.localWeb3.eth.getBlockNumber())
        .then(lastBlockNumber => window.localWeb3.eth.getBlock(lastBlockNumber, true))
        .then(this.addBlockTransactions);
    },
    addBlockTransactions(block) {
      this.loadedBlocks++;
      if (block.number === 0
          || this.transactionsToLoad <= this.transactions.length
          || this.transactionCount === this.transactions.length
          || this.loadedBlocks > this.maxBlocksToLoad) {
        return;
      }
      if (block.transactions && block.transactions.length) {
        const thiss = this;
        block.transactions.forEach(transaction => {
          if (transaction.to && transaction.from && (transaction.to.toLowerCase() === thiss.account.toLowerCase() || transaction.from && transaction.from.toLowerCase() === thiss.account.toLowerCase())) {
            thiss.transactions.push({
              to: transaction.to && transaction.to.toLowerCase() === thiss.account.toLowerCase() ? 'Me' : transaction.to,
              from: transaction.from && transaction.from.toLowerCase() === thiss.account.toLowerCase() ? 'Me' : transaction.from,
              amount: window.localWeb3.utils.fromWei(transaction.value, 'ether')
            });
          }
        });
        window.localWeb3.eth.getBlock(block.parentHash, true)
          .then(this.addBlockTransactions)
          .catch((e) => console.log(`Error retrieving block: ${block.parentHash}`));
      }
    }
  }
};
</script>