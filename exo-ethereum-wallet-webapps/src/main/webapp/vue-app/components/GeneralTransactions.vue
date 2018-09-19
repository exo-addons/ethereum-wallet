<template>
  <v-flex>
    <v-card class="card--flex-toolbar">
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="!finishedLoading">Loading recent Transactions...</div>

      <v-progress-circular v-if="loading" indeterminate color="primary" />
      <v-progress-circular
        v-else-if="!finishedLoading"
        :rotate="-90"
        :size="80"
        :width="15"
        :value="loadingPercentage"
        color="primary"
        buffer>
        {{ loadingPercentage }}%
      </v-progress-circular>

      <v-expansion-panel v-if="Object.keys(transactions).length">
        <v-expansion-panel-content v-for="(item, index) in sortedTransaction" :key="index">
          <v-list slot="header" two-line class="pt-0 pb-0">
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
                  <wallet-address v-else :allow-copy="!item.isContractName" :value="item.displayName" />
                </v-list-tile-title>
                <v-list-tile-sub-title>
                  <v-icon v-if="!item.status" color="orange" title="Transaction failed">warning</v-icon>
                  <span>{{ Number(item.amount) + Number(item.fee) }} ether</span>
                  <span v-if="item.amountFiat"> / {{ Number(item.amountFiat) + Number(item.feeFiat) }} {{ fiatSymbol }}</span>
                </v-list-tile-sub-title>
              </v-list-tile-content>
              <v-list-tile-action v-if="(item.date && !item.pending) || etherscanLink" class="transactionDetailActions" title="Open on etherscan">
                <v-list-tile-action-text v-if="item.date && !item.pending">{{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}</v-list-tile-action-text>
                <a v-if="etherscanLink" :href="`${etherscanLink}${item.hash}`" target="_blank">
                  <v-icon color="primary">info</v-icon>
                </a>
              </v-list-tile-action>
            </v-list-tile>
          </v-list>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-flex v-else-if="finishedLoading" class="text-xs-center">
        <v-chip color="white">
          <span>No recent transactions</span>
        </v-chip>
      </v-flex>
      <div class="">
        <a v-if="finishedLoading" href="javascript:void(0);" @click="loadMore">Load more</a>
      </div>
    </v-card>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

import {loadPendingTransactions, loadStoredTransactions, loadTransactions, addTransaction} from '../WalletEther.js';
import {getTransactionEtherscanlink} from '../WalletUtils.js';

export default {
  components: {
    WalletAddress
  },
  props: {
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    error: {
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
      loading: false,
      finishedLoading: false,
      transactionsPerPage: 10,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      etherscanLink: null,
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
        this.loading = true;
        this.init()
          .then(() => this.loading = false)
          .catch(error => {
            console.debug("account field change event - error", error);
            this.loading = false;
            this.$emit("error", `Account loading error: ${error}`);
          });
      } 
    },
    networkId() {
      this.etherscanLink = getTransactionEtherscanlink(this.networkId);
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
          this.loading = false;
          this.$emit("error", `Account initialization error: ${error}`);
        });
    }
    if (!this.etherscanLink) {
      this.etherscanLink = getTransactionEtherscanlink(this.networkId);
    }
  },
  methods: {
    init() {
      this.transactions = {};
      this.loadedBlocks = 0;
      this.stopLoading = false;
      this.finishedLoading = true;
      this.loading = true;

      // Get transactions to latest block with maxBlocks to load
      return loadPendingTransactions(this.networkId, this.account, this.transactions, () => {
        if (this.stopLoading) {
          throw new Error("stopLoading");
        }
        this.$emit("refresh-balance");
        this.forceUpdateList();
      })
        .then(() => loadStoredTransactions(this.networkId, this.account, this.transactions, () => {
          if (this.stopLoading) {
            throw new Error("stopLoading");
          }
          this.forceUpdateList();
          this.loading = false;
          this.finishedLoading = false;
        }))
        .then(() => loadTransactions(this.networkId, this.account, this.transactions, null, null, this.maxBlocksToLoad, (loadedBlocks) => {
          if (this.stopLoading) {
            throw new Error("stopLoading");
          }
          this.loadedBlocks = loadedBlocks;
          this.forceUpdateList();
        }))
        .then(loadedBlocksDetails => {
          this.forceUpdateList();
          this.newestBlockNumber = loadedBlocksDetails.toBlock;
          this.oldestBlockNumber = loadedBlocksDetails.fromBlock;
          this.loading = false;
        })
        .catch(e => {
          this.loading = false;
          this.finishedLoading = true;
          if (!this.stopLoading) {
            console.debug("loadTransactions - method error", e);
            this.$emit("error", `${e}`);
          }
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
          this.loading = false;
          this.forceUpdateList();
          this.newestBlockNumber = loadedBlocksDetails.toBlock;
          this.oldestBlockNumber = loadedBlocksDetails.fromBlock;
        })
        .catch(e => {
          this.finishedLoading = true;
          this.loading = false;
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
    stopLoadingTransactions() {
      this.stopLoading = true;
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