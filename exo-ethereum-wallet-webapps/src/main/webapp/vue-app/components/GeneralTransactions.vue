<template>
  <v-flex>
    <v-card class="card--flex-toolbar">
      <v-progress-circular
        v-if="!finishedLoading"
        :rotate="-90"
        :size="80"
        :width="15"
        :value="loadingPercentage"
        color="primary"
        buffer>
        {{ loadingPercentage }}%
      </v-progress-circular>
      <v-list v-if="Object.keys(transactions).length" two-line class="pt-0 pb-0">
        <template v-for="(item, index) in sortedTransaction">
          <v-list-tile :key="item.hash" avatar>
            <v-progress-circular v-if="item.pending" indeterminate color="primary" class="mr-4" />
            <v-list-tile-avatar v-else :title="item.error ? item.error : ''">
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
                <code v-else>{{ item.displayName }}</code>
              </v-list-tile-title>
              <v-list-tile-sub-title>
                <v-icon v-if="!item.status" color="orange" title="Transaction failed">warning</v-icon>
                <span>{{ item.amount }} ether</span>
                <span v-if="item.amountUSD"> / {{ item.amountUSD }} $</span>
                <span v-if="item.fee">
                  ( Fee: {{ item.fee }} ether
                  <span v-if="item.feeUSD">/ {{ item.feeUSD }} $</span>
                  )
                </span>
              </v-list-tile-sub-title>
            </v-list-tile-content>
            <v-list-tile-action v-if="item.date && !item.pending">
              <v-list-tile-action-text>{{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}</v-list-tile-action-text>
            </v-list-tile-action>
          </v-list-tile>
          <v-divider v-if="index + 1 < Object.keys(transactions).length" :key="index"></v-divider>
        </template>
      </v-list>
      <v-flex v-else-if="finishedLoading" class="text-xs-center">
        <v-chip color="white">
          <span>No transactions</span>
        </v-chip>
      </v-flex>
      <div class="">
        <a v-if="!loading && finishedLoading" href="#" @click="loadMore">Load more</a>
      </div>
    </v-card>
  </v-flex>
</template>

<script>
import {loadPendingTransactions, loadStoredTransactions, loadTransactions, addTransaction} from '../WalletEther.js';

export default {
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
    }
  },
  data () {
    return {
      // A trick to force update computed list
      // since the attribute this.atransactions is modified outside the component
      refreshIndex: 1,
      newestBlockNumber: 0,
      oldestBlockNumber: 0,
      finishedLoading: false,
      transactionsPerPage: 10,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      transactions: {}
    };
  },
  computed: {
    sortedTransaction() {
      // A trick to force update computed list
      // since the attribute this.atransactions is modified outside the component
      if (!this.refreshIndex) {
        return {};
      }
      const transactions = this.transactions;
      const sortedTransactions = {};
      Object.keys(transactions)
        .sort((key1, key2) => transactions[key2].date - transactions[key1].date)
        .forEach(key => {
          sortedTransactions[key] = transactions[key];
        });
      return sortedTransactions;
    },
    loadingPercentage() {
      return parseInt(this.loadedBlocks * 100 / this.maxBlocksToLoad);
    }
  },
  watch: {
    account(account) {
      if (account) {
        this.$emit("loading");
        this.init()
          .then(() => this.$emit("end-loading"))
          .catch(error => {
            console.debug("account field change event - error", error);
            this.$emit("error", `Account loading error: ${error}`);
          });
      }
    }
  },
  created() {
    if (this.account) {
      this.maxBlocksToLoad = window.walletSettings.defaultBlocksToRetrieve;
      this.init()
        .then(() => this.finishedLoading = true)
        .catch(error => {
          console.debug("init method - error", error);
          this.finishedLoading = true;
          this.$emit("error", `Account initialization error: ${error}`);
        });
    }
  },
  methods: {
    init() {
      this.transactions = {};
      this.loadedBlocks = 0;

      // Get transactions to latest block with maxBlocks to load
      return loadStoredTransactions(this.networkId, this.account, this.transactions, () => {
        this.forceUpdateList();
      })
        .then(() => loadPendingTransactions(this.networkId, this.account, this.transactions, () => {
          this.$emit("refresh-balance");
          this.forceUpdateList();
        }))
        .then(() => loadTransactions(this.networkId, this.account, this.transactions, null, null, this.maxBlocksToLoad, (loadedBlocks) => {
          this.loadedBlocks = loadedBlocks;
          this.forceUpdateList();
        }))
        .then(loadedBlocksDetails => {
          this.forceUpdateList();
          this.newestBlockNumber = loadedBlocksDetails.toBlock;
          this.oldestBlockNumber = loadedBlocksDetails.fromBlock;
        })
        .catch(e => {
          console.debug("loadTransactions - method error", e);
          this.$emit("error", `${e}`);
        });
    },
    loadMore() {
      this.loadedBlocks = 0;
      this.finishedLoading = false;
      return loadTransactions(this.networkId, this.account, this.transactions, null, this.oldestBlockNumber, this.maxBlocksToLoad, (loadedBlocks) => {
        this.loadedBlocks = loadedBlocks;
        this.forceUpdateList();
      })
        .then(loadedBlocksDetails => {
          this.finishedLoading = true;
          this.forceUpdateList();
          this.newestBlockNumber = loadedBlocksDetails.toBlock;
          this.oldestBlockNumber = loadedBlocksDetails.fromBlock;
        })
        .catch(e => {
          this.finishedLoading = true;
          console.debug("loadTransactions - method error", e);
          this.$emit("error", `${e}`);
        });
    },
    addTransaction(transaction) {
      this.transactions = addTransaction(this.networkId,
        this.account,
        this.transactions,
        transaction,
        null,
        null,
        () => {
          this.$emit("refresh-balance");
          this.forceUpdateList();
        },
        error => {
          transaction.icon = 'warning';
          transaction.error = `Error loading transaction ${error}`;
          this.forceUpdateList();
        });
      this.forceUpdateList();
    },
    forceUpdateList() {
      // A trick to force update computed list
      // since the attribute this.atransactions is modified outside the component
      this.refreshIndex ++;
      this.$forceUpdate();
    }
  }
};
</script>