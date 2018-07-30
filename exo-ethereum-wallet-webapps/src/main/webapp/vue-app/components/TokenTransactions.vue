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
              <v-list-tile-sub-title v-html="item.amount"></v-list-tile-sub-title>
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
          if (res.args._to === this.account || res.args._from === this.account) {
            this.transactions.push({
              hash: res.transactionHash,
              title: res.args._to === this.account ? `Received from ${res.args._from}`: `Sent to ${res.args._from}`,
              color: res.args._to === this.account ? 'green' : 'red',
              icon: 'fa-exchange-alt',
              amount: res.args._value.toNumber()
            });
          }
        });
      }
    }
  }
};
</script>