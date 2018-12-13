<template>
  <v-flex class="transactionsList">
    <v-card class="card--flex-toolbar" flat>
      <div v-if="error && !loading" class="alert alert-error">
        <i class="uiIconError"></i>{{ error }}
      </div>

      <div v-if="loading" class="grey--text">
        Loading recent transactions...
      </div>
      <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-0 mt-0" />

      <v-expansion-panel v-if="Object.keys(sortedTransactions).length">
        <v-expansion-panel-content
          v-for="(item, index) in sortedTransactions"
          :id="`transaction-${item.hash}`"
          :key="index"
          :value="item.selected">

          <v-list slot="header" :class="item.selected && 'blue lighten-5'" two-line class="pt-0 pb-0">
            <v-list-tile :key="item.hash" class="transactionDetailItem" avatar>
              <v-progress-circular v-if="item.pending" indeterminate color="primary" class="mr-4" />
              <v-list-tile-avatar v-else-if="item.error" :title="item.error">
                <v-icon color="red">warning</v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else-if="item.adminIcon">
                <v-icon color="grey">fa-cog</v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else-if="item.isReceiver">
                <v-icon color="green">fa-arrow-down</v-icon>
              </v-list-tile-avatar>
              <v-list-tile-avatar v-else>
                <v-icon color="red">fa-arrow-up</v-icon>
              </v-list-tile-avatar>

              <v-list-tile-content class="transactionDetailContent">

                <v-list-tile-title v-if="item.type === 'contract' && item.isContractCreation">
                  <span>Created contract </span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'ether'">
                  <profile-chip
                    v-if="displayFullTransaction && item.isReceiver"
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <profile-chip
                    v-else-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />

                  <span v-if="item.isReceiver">Received from</span>
                  <span v-else>Sent to</span>

                  <profile-chip
                    v-if="item.isReceiver"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <profile-chip
                    v-else
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'transferFrom'">
                  <span>Sent to</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span>by</span>
                  <profile-chip
                    :address="item.byAddress"
                    :profile-id="item.byUsername"
                    :profile-technical-id="item.byTechnicalId"
                    :profile-type="item.byType"
                    :display-name="item.byDisplayName"
                    :avatar="item.byAvatar" />
                  <span>on behalf of</span>
                  <profile-chip
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && (item.contractMethodName === 'transfer' || item.contractMethodName === 'approve')">
                  <profile-chip
                    v-if="displayFullTransaction && item.isReceiver"
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <profile-chip
                    v-else-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />

                  <span v-if="item.contractMethodName === 'transfer' && item.isReceiver">Received from</span>
                  <span v-else-if="item.contractMethodName === 'transfer' && !item.isReceiver">Sent to</span>
                  <span v-else-if="item.contractMethodName === 'approve' && item.isReceiver">Delegated from</span>
                  <span v-else>Delegated to</span>

                  <profile-chip
                    v-if="item.isReceiver"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <profile-chip
                    v-else
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'addAdmin'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Added</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span>as administrator on contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'removeAdmin'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Removed</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span>as administrator from contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>
                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'transferOwnership'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Transfered ownership to</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span> on contract </span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'approveAccount'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Approved</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span>account on contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'disapproveAccount'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Disapproved</span>
                  <profile-chip
                    :address="item.toAddress"
                    :profile-id="item.toUsername"
                    :profile-technical-id="item.toTechnicalId"
                    :profile-type="item.toType"
                    :display-name="item.toDisplayName"
                    :avatar="item.toAvatar" />
                  <span>account on contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'pause'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Paused contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'unPause'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Un-paused contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.contractMethodName === 'setSellPrice'">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Modified sell price to contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else-if="item.type === 'contract' && item.amount && Number(item.amount) && item.amountFiat">
                  <profile-chip
                    v-if="displayFullTransaction"
                    :address="item.fromAddress"
                    :profile-id="item.fromUsername"
                    :profile-technical-id="item.fromTechnicalId"
                    :profile-type="item.fromType"
                    :display-name="item.fromDisplayName"
                    :avatar="item.fromAvatar" />
                  <span>Sent ether to contract</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-title v-else>
                  <span>Contract transaction</span>
                  <wallet-address v-if="item.contractName" :value="item.contractName" :allow-copy="false" />
                  <wallet-address v-else :value="item.contractAddress" allow-copy />
                </v-list-tile-title>

                <v-list-tile-sub-title>
                  <v-icon v-if="!item.status" color="orange" title="Transaction failed">warning</v-icon>
                  <v-list-tile-action-text v-if="item.date && !item.pending">
                    {{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}
                  </v-list-tile-action-text>
                </v-list-tile-sub-title>

              </v-list-tile-content>

              <v-list-tile-content v-if="item.type === 'ether' && item.amount && Number(item.amount)" class="transactionDetailActions">
                <v-list-tile-title :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'">
                  <span>{{ toFixed(item.amount) }} ETH</span>
                </v-list-tile-title>
                <v-list-tile-sub-title v-if="item.amountFiat">
                  <v-list-tile-action-text>{{ toFixed(item.amountFiat) }} {{ fiatSymbol }}</v-list-tile-action-text>
                </v-list-tile-sub-title>
              </v-list-tile-content>

              <v-list-tile-content v-else class="transactionDetailActions">
                <v-list-tile-title v-if="item.contractAmount" :class="item.adminIcon ? '' : item.isReceiver ? 'green--text' : 'red--text'"><span>{{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}</span></v-list-tile-title>
                <v-list-tile-title v-else-if="item.amount && Number(item.amount)" :class="item.isReceiver ? 'green--text' : 'red--text'"><span>{{ toFixed(item.amount) }} ether</span></v-list-tile-title>
                <v-list-tile-sub-title v-if="item.amountFiat"><v-list-tile-action-text>{{ toFixed(item.amountFiat) }} {{ fiatSymbol }}</v-list-tile-action-text></v-list-tile-sub-title>
                <v-list-tile-sub-title v-else />
              </v-list-tile-content>
            </v-list-tile>
          </v-list>

          <v-list class="pl-5 ml-2 pr-4" dense>
            <v-list-tile v-if="!item.pending">
              <v-list-tile-content>Status</v-list-tile-content>
              <v-list-tile-content class="align-end">
                <v-icon
                  :color="item.status ? 'success' : 'error'"
                  v-text="item.status ? 'fa-check-circle' : 'fa-exclamation-circle'" />
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.label" class="dynamic-height">
              <v-list-tile-content>Label</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.label }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="item.message" class="dynamic-height">
              <v-list-tile-content>Message</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ item.message }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="Number(item.contractAmount)">
              <v-list-tile-content>{{ item.contractAmountLabel }}</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ toFixed(item.contractAmount) }} {{ item.contractSymbol }}
              </v-list-tile-content>
            </v-list-tile>
            <v-list-tile v-else-if="Number(item.amountFiat)">
              <v-list-tile-content>Amount</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ toFixed(item.amountFiat) }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>
            <v-list-tile v-else-if="Number(item.amount)">
              <v-list-tile-content>Amount</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ toFixed(item.amount) }} ether
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
                {{ toFixed(contractDetails.balanceFiat) }} {{ fiatSymbol }}
              </v-list-tile-content>
            </v-list-tile>

            <v-list-tile v-if="!contractDetails.isContract && item.balanceAtDateFiat">
              <v-list-tile-content>Balance at date</v-list-tile-content>
              <v-list-tile-content class="align-end">
                {{ toFixed(item.balanceAtDateFiat) }} {{ fiatSymbol }}
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
              <v-list-tile-content v-if="item.feeToken" class="align-end">
                {{ toFixed(item.feeToken) }} {{ item.contractSymbol }}
              </v-list-tile-content>
              <v-list-tile-content v-else class="align-end">
                {{ toFixed(item.feeFiat) }} {{ fiatSymbol }}
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
        <div v-if="!limitReached">
          <v-btn :loading="loading" color="primary" flat @click="transactionsLimit += transactionsPerPage">
            Load More
          </v-btn>
        </div>
      </v-expansion-panel>
      <v-flex v-else-if="!loading" class="text-xs-center">
        <span>No recent transactions</span>
      </v-flex>
    </v-card>
  </v-flex>

</template>

<script>
import TransactionsList from './TransactionsList.vue';
import WalletAddress from './WalletAddress.vue';
import ProfileChip from './ProfileChip.vue';

import {getTransactionEtherscanlink, getAddressEtherscanlink, getTokenEtherscanlink} from '../WalletUtils.js';
import {loadTransactions, loadContractTransactions, addTransaction} from '../WalletTransactions.js';

export default {
  components: {
    ProfileChip,
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
    },
    displayFullTransaction: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    selectedTransactionHash: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data () {
    return {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      refreshIndex: 1,
      loading: false,
      transactionsLimit: 10,
      transactionsPerPage: 10,
      limitReached: false,
      transactions: {}
    };
  },
  computed: {
    sortedTransactions() {
      // A trick to force update computed list
      // since the attribute this.transactions is modified outside the component
      if (!this.refreshIndex) {
        return {};
      }
      const transactions = this.transactions;
      const sortedTransactions = {};
      Object.values(transactions)
        .filter(transaction => transaction && transaction.date && !transaction.notLoaded && !transaction.ignore)
        .sort((transaction1, transaction2) => transaction2.date - transaction1.date)
        .forEach(transaction => {
          sortedTransactions[transaction.hash] = transaction;
        });
      return sortedTransactions;
    }
  },
  watch: {
    transactionsLimit() {
      if (this.transactionsLimit !== this.transactionsPerPage && this.account && !this.loading) {
        this.init()
          .catch(error => {
            console.debug("account field change event - error", error);
            this.loading = false;
            this.$emit("error", `Account loading error: ${error}`);
          });
      } 
    },
    contractDetails() {
      if (this.contractDetails && this.account) {
        this.limitReached = false;
        this.transactions = {};
        this.transactionsLimit = this.transactionsPerPage;
        this.init()
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
      this.init()
        .catch(error => {
          console.debug("init method - error", error);
          this.loading = false;
          this.$emit("error", `Account initialization error: ${error}`);
        });
    }
  },
  methods: {
    init() {
      this.loading = true;
      this.error = null;

      const thiss = this;
      // Get transactions to latest block with maxBlocks to load
      return this.loadRecentTransaction(this.transactionsLimit)
        .then(() => {
          if (this.selectedTransactionHash) {
            const selectedTransaction = this.transactions[this.selectedTransactionHash];
            if (selectedTransaction) {
              this.$set(selectedTransaction, "selected", true);
              this.$nextTick(() => {
                setTimeout(() => {
                  const selectedTransactionElement = document.getElementById(`transaction-${selectedTransaction.hash}`);
                  if (selectedTransactionElement) {
                    selectedTransactionElement.scrollIntoView();
                  }
                } ,200);
              });
            }
          }
          this.loading = false;
          this.forceUpdateList();
        })
        .catch(e => {
          console.debug("loadTransactions - method error", e);

          this.loading = false;
          this.$emit("error", `${e}`);
        })
        .finally(() => {
          this.$emit("loaded", this.sortedTransactions, this.transactions ? Object.keys(this.transactions).length : 0);
        });
    },
    loadRecentTransaction(limit) {
      const thiss = this;
      return loadTransactions(this.networkId, this.account, this.contractDetails, this.transactions, false, limit, this.selectedTransactionHash, () => {
        thiss.$emit("refresh-balance");
        thiss.forceUpdateList();
      })
        .then(() => {
          const totalTransactionsCount = Object.keys(this.transactions).length;
          const notLoadedTransactionsCount = Object.values(this.transactions).filter(transaction => transaction.notLoaded).length;
          const ignoredTransactionsCount = Object.values(this.transactions).filter(transaction => transaction.ignore).length;
          const loadedTransactionsCount = totalTransactionsCount - notLoadedTransactionsCount - ignoredTransactionsCount;
          this.limitReached = notLoadedTransactionsCount === 0;
          if(loadedTransactionsCount > this.transactionsLimit) {
            this.transactionsLimit = loadedTransactionsCount;
          }
          if(loadedTransactionsCount < this.transactionsLimit && notLoadedTransactionsCount > 0) {
            return this.loadRecentTransaction(limit + this.transactionsPerPage);
          }
        })
        .catch(e => {
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
          this.$emit("loaded", this.sortedTransactions);
          this.$emit("refresh-balance");
          this.forceUpdateList();
        },
        error => {
          transaction.icon = 'warning';
          transaction.error = `Error loading transaction ${error}`;
          this.forceUpdateList();
        }).then(() => this.forceUpdateList());
    },
    forceUpdateList() {
      try {
        // A trick to force update computed list
        // since the attribute this.transactions is modified outside the component
        this.refreshIndex ++;
        this.$forceUpdate();
      } catch (e) {
        console.debug("forceUpdateList - method error", e);
      }
    }
  }
};
</script>