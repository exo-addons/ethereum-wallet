<template>
  <v-flex class="transactionsList">
    <v-card class="card--flex-toolbar" flat>
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="!stopLoading && (loading || displayLoadingPercentage)" class="grey--text">
        Loading recent transactions...
        <a v-if="displayLoadingPercentage" href="javascript:void(0);" @click="stopLoadingTransactions()">
          stop
        </a>
      </div>
      <div v-else-if="loading || displayLoadingPercentage" class="grey--text">Stopping...</div>

      <v-progress-linear
        v-if="!finishedLoading && displayLoadingPercentage"
        :rotate="-90"
        :size="80"
        :width="15"
        :value="loadingPercentage"
        color="primary"
        class="mb-0 mt-0"
        buffer>
      </v-progress-linear>
      <v-progress-linear v-else-if="loading" indeterminate color="primary" class="mb-0 mt-0" />

      <v-expansion-panel v-if="Object.keys(sortedTransactions).length">
        <v-expansion-panel-content
          v-for="(item, index) in sortedTransactions"
          :key="index">

          <v-list slot="header" two-line class="pt-0 pb-0">
            <v-list-tile :key="item.hash" avatar>
              <v-progress-circular v-if="item.pending" indeterminate color="primary" class="mr-4" />
              <v-list-tile-avatar v-else-if="item.error" :title="item.error">
                <v-icon color="red">warning</v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else-if="item.isReceiver">
                <v-icon color="green">fa-arrow-down</v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else>
                <v-icon color="red">fa-arrow-up</v-icon>
              </v-list-tile-avatar>

              <v-list-tile-content class="transactionDetailContent">
                <v-list-tile-title v-if="item.type === 'ether' && item.isReceiver">
                  <span>Received Ether from</span>
                  <v-chip v-if="item.fromAvatar" :title="item.fromUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.fromAvatar">
                    </v-avatar>
                    <span v-html="item.fromDisplayName"></span>
                  </v-chip>
                  <wallet-address v-else :value="item.fromAddress" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'ether' && !item.isReceiver">
                  <span>Sent to</span>
                  <v-chip v-if="item.toAvatar" :title="item.toUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.toAvatar">
                    </v-avatar>
                    <span v-html="item.toDisplayName"></span>
                  </v-chip>
                  <wallet-address v-else :value="item.toAddress" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.isContractCreation">
                  <span>Created contract </span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && (item.contractMethodName === 'transfer' || item.contractMethodName === 'approve')">
                  <span v-if="item.contractMethodName === 'transfer' && item.isReceiver">Received from</span>
                  <span v-else-if="item.contractMethodName === 'transfer' && !item.isReceiver">Sent to</span>
                  <span v-else-if="item.contractMethodName === 'approve' && item.isReceiver">Delegated from</span>
                  <span v-else>Delegated to</span>

                  <v-chip v-if="item.isReceiver && item.fromAvatar" :title="item.fromUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.fromAvatar">
                    </v-avatar>
                    <span v-html="item.fromDisplayName" ></span>
                  </v-chip>
                  <wallet-address v-else-if="item.isReceiver && !item.fromAvatar" :value="item.fromAddress" />
                  <v-chip v-else-if="!item.isReceiver && item.toAvatar" :title="item.toUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.toAvatar">
                    </v-avatar>
                    <span v-html="item.toDisplayName" ></span>
                  </v-chip>
                  <wallet-address v-else :value="item.toAddress" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'transferFrom'">
                  <span>Sent to</span>

                  <v-chip v-if="item.toAvatar" :title="item.toUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.toAvatar">
                    </v-avatar>
                    <span v-html="item.toDisplayName"></span>
                  </v-chip>
                  <wallet-address v-else :value="item.toAddress" />

                  on behalf of

                  <v-chip v-if="item.fromAvatar" :title="item.fromUsername" class="mt-0 mb-0" small>
                    <v-avatar size="23px !important">
                      <img :src="item.fromAvatar">
                    </v-avatar>
                    <span v-html="item.fromDisplayName"></span>
                  </v-chip>
                  <wallet-address v-else :value="item.fromAddress" />
                </v-list-tile-title>

                <v-list-tile-sub-title>
                  <v-icon v-if="!item.status" color="orange" title="Transaction failed">warning</v-icon>
                  <v-list-tile-action-text v-if="item.date && !item.pending">
                    {{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>

              </v-list-tile-content>

              <v-list-tile-content v-if="item.type === 'ether'" class="transactionDetailActions">
                <v-list-tile-title :class="item.isReceiver ? 'green--text' : 'red--text'">
                  <span>{{ Number(item.amount) }} ETH</span>
                </v-list-tile-title>
                <v-list-tile-sub-title v-if="item.amountFiat">
                  <v-list-tile-action-text>{{ Number(item.amountFiat) }} {{ fiatSymbol }}</v-list-tile-action-text>
                </v-list-tile-sub-title>
              </v-list-tile-content>

              <v-list-tile-content v-else-if="item.type === 'contract' && !item.isContractCreation" class="transactionDetailActions">
                <v-list-tile-title :class="item.isReceiver ? 'green--text' : 'red--text'"><span>{{ Number(item.contractAmount) }} {{ item.contractSymbol }}</span></v-list-tile-title>
                <v-list-tile-sub-title v-if="item.amountFiat"><v-list-tile-action-text>{{ Number(item.amountFiat) }} {{ fiatSymbol }}</v-list-tile-action-text></v-list-tile-sub-title>
                <v-list-tile-sub-title v-else />
              </v-list-tile-content>
            </v-list-tile>
          </v-list>

          <v-list class="pl-5 ml-2 pr-4" dense>
            <v-list-tile>
              <v-list-tile-content>Status</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <v-icon
                  :color="item.status ? 'success' : 'error'"
                  v-text="item.status ? 'fa-check-circle' : 'fa-exclamation-circle'" />
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.amount">
              <v-list-tile-content>Amount</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.amountFiat }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>
            <v-list-tile v-else-if="item.contractAmount">
              <v-list-tile-content>Amount</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.contractAmount }} {{ item.contractSymbol }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.fromAddress">
              <v-list-tile-content>From address</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="addressEtherscanLink"
                  :href="`${addressEtherscanLink}${item.fromAddress}`"
                  target="_blank"
                  title="Open on etherscan">{{ item.fromAddress }}</a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.toAddress">
              <v-list-tile-content>To address</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="addressEtherscanLink"
                  :href="`${addressEtherscanLink}${item.toAddress}`"
                  target="_blank"
                  title="Open on etherscan">{{ item.toAddress }}</a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="!contractDetails.isContract">
              <v-list-tile-content>Balance now</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ contractDetails.balanceFiat }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="!contractDetails.isContract && item.balanceAtDateFiat">
              <v-list-tile-content>Balance at date</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.balanceAtDateFiat }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.contractName">
              <v-list-tile-content>Contract name</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.contractName }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.contractAddress">
              <v-list-tile-content>Contract address</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="tokenEtherscanLink"
                  :href="`${tokenEtherscanLink}${item.contractAddress}`"
                  target="_blank"
                  title="Open on etherscan">{{ item.contractAddress }}</a>
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.fee">
              <v-list-tile-content>Transaction fee</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.feeFiat }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile>
              <v-list-tile-content>Transaction hash</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <a
                  v-if="transactionEtherscanLink"
                  :href="`${transactionEtherscanLink}${item.hash}`"
                  target="_blank" title="Open on etherscan">
                  {{ item.hash }}
                </a>
              </v-list-tile-content>
            </v-list-tile>
          </v-list>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-flex v-else-if="!displayLoadingPercentage && !loading" class="text-xs-center">
        <v-chip color="white">
          <span>No recent transactions</span>
        </v-chip>
      </v-flex>
      <div v-if="!contractDetails.isContract">
        <a v-if="!displayLoadingPercentage && !loading" href="javascript:void(0);" @click="loadMore()">Load more</a>
      </div>
    </v-card>
  </v-flex>

</template>

<script>
import TransactionsList from './TransactionsList.vue';
import WalletAddress from './WalletAddress.vue';

import {getTransactionEtherscanlink, getAddressEtherscanlink, getTokenEtherscanlink} from '../WalletUtils.js';
import {loadPendingTransactions, loadStoredTransactions, loadTransactions, addTransaction} from '../WalletTransactions.js';

export default {
  components: {
    WalletAddress,
    TransactionsList
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
    contractDetails: {
      type: Object,
      default: function() {
        return {};
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
      finishedLoading: true,
      transactionsPerPage: 10,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      transactions: {}
    };
  },
  computed: {
    displayLoadingPercentage() {
      return !this.contractDetails.isContract && !this.finishedLoading;
    },
    sortedTransactions() {
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
      if (this.networkId) {
        this.transactionEtherscanLink = getTransactionEtherscanlink(this.networkId);
        this.addressEtherscanLink = getAddressEtherscanlink(this.networkId);
        this.tokenEtherscanLink = getTokenEtherscanlink(this.networkId);
      }
    }
  },
  created() {
    if (!this.transactionEtherscanLink && this.networkId) {
      this.transactionEtherscanLink = getTransactionEtherscanlink(this.networkId);
      this.addressEtherscanLink = getAddressEtherscanlink(this.networkId);
      this.tokenEtherscanLink = getTokenEtherscanlink(this.networkId);
    }

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
  },
  methods: {
    init() {
      this.transactions = {};
      this.loadedBlocks = 0;
      this.stopLoading = false;
      this.finishedLoading = true;
      this.loading = true;

      // Get transactions to latest block with maxBlocks to load
      return loadPendingTransactions(this.networkId, this.account, this.contractDetails, this.transactions, () => {
        this.$emit("refresh-balance");
        this.forceUpdateList();
      })
        .then(pendingTransactions => {
          this.forceUpdateList();

          return loadStoredTransactions(this.networkId, this.account, this.contractDetails, this.transactions, () => {
            if (this.stopLoading) {
              throw new Error("stopLoading");
            }

            // Update by 10 transactions
            if (this.transactions && Object.keys(this.transactions).length % 10 === 0) {
              this.forceUpdateList();
            }
          });
        })
        .then(storedTansactions => {
          this.forceUpdateList();

          this.finishedLoading = false;

          return loadTransactions(this.networkId, this.account, this.contractDetails, this.transactions, null, null, this.maxBlocksToLoad, (loadedBlocks) => {
            if (this.stopLoading) {
              throw new Error("stopLoading");
            }
            this.loadedBlocks = loadedBlocks;
            this.forceUpdateList();
          });
        })
        .then(loadedBlocksDetails => {
          this.loading = false;

          this.forceUpdateList();
          if (loadedBlocksDetails && !this.contractDetails.isContract) {
            this.newestBlockNumber = loadedBlocksDetails.toBlock;
            this.oldestBlockNumber = loadedBlocksDetails.fromBlock;
          }
          this.loading = false;
          this.finishedLoading = true;
        })
        .catch(e => {
          console.debug("loadTransactions - method error", e);

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
      this.stopLoading = false;

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
    addTransaction(transaction, contactDetails) {
      addTransaction(this.networkId,
        this.account,
        contactDetails ? contactDetails : this.contactDetails,
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
        }).then(() => this.forceUpdateList());
    },
    stopLoadingTransactions() {
      this.stopLoading = true;
    },
    forceUpdateList() {
      try {
        // A trick to force update computed list
        // since the attribute this.atransactions is modified outside the component
        this.refreshIndex ++;
        this.$forceUpdate();
      } catch (e) {
        console.debug("forceUpdateList - method error", e);
      }
    }
  }
};
</script>