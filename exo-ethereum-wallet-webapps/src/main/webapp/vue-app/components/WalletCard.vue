<template>
  <v-card>
    <v-card-title primary-title>
      <div>
        <div class="headline">{{ networkName }}</div>
        <div class="grey--text">{{ account }}</div>
        <div class="grey--text">{{ balance }} eth</div>
        <div class="grey--text">{{ tokensBalance }}</div>
      </div>
    </v-card-title>
  </v-card>
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
      networkName: null,
      balance: 0,
      tokensBalance: 0
    };
  },
  watch: {
    contract(value) {
      this.computeTokenBalance().catch((e) => {
        this.tokensBalance = "contract not deployed";
      });
    },
    account(value) {
      if (!value) {
        return;
      }
      this.computeBalance();
      this.computeNetwork();
    }
  },
  methods: {
    computeNetwork() {
      const thiss = this;
      window.localWeb3.eth.net.getNetworkType((err, netType) => {
        if(err) {
          thiss.errorMessage = err;
          return;
        }
        thiss.networkName = `${netType.toUpperCase()} Network`;
      });
    },
    computeBalance() {
      const thiss = this;
      window.localWeb3.eth.getCoinbase((err, account) => {
        if (!err && account) {
          window.localWeb3.eth.getBalance(account, (error, balance) => {
            thiss.balance = parseInt(balance) / 1000000000000000000;
          });
        }
      });
    },
    computeTokenBalance() {
      if (!this.contract || !this.contract.balanceOf) {
        return Promise.reject(new Error("Token contract not loaded"));
      }

      let tokensSymbol;
      return this.contract.symbol.call()
        .then(symbol => tokensSymbol = symbol)
        .then(() => this.contract.balanceOf.call(this.account))
        .then(balance => this.tokensBalance = `${balance} ${tokensSymbol}`);
    }
  }
};
</script>

