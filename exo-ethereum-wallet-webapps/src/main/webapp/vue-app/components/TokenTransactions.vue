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
      headers: [
        { align: 'left', text: 'To', value: 'to' },
        { align: 'left', text: 'From', value: 'from' },
        { align: 'left', text: 'Amount', value: 'amount' }
      ],
      transactions: []
    };
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
        this.contract.Transfer({}, { fromBlock: 0, toBlock: 'pending' }, (err, res) => {
          if (res.args._from === this.account || res.args._from === this.account) {
            this.transactions.push({
              to: res.args._to === this.account ? 'Me' : res.args._to,
              from: res.args._from === this.account ? 'Me' : res.args._from,
              amount: res.args._value.toNumber()
            });
          }
        });
      }
    }
  }
};
</script>