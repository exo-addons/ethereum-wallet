<template>
  <v-flex>
    <v-card class="card--flex-toolbar">
      <v-subheader>Transactions list retrieved from {{ loadedBlocks }} / {{ maxBlocksToLoad }} latest blocks. {{ transactions.length }} tranactions are loaded (maximum {{ transactionsToLoad }})</v-subheader>
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
      <v-list v-if="transactions.length" two-line class="pt-0 pb-0">
        <template v-for="(item, index) in sortedTransaction">
          <v-list-tile :key="item.hash" avatar>
            <v-list-tile-avatar>
              <v-icon :class="item.color" dark>{{ item.icon }}</v-icon>
            </v-list-tile-avatar>
            <v-list-tile-content>
              <v-list-tile-title>
                <span>{{ item.titlePrefix }}</span>
                <v-chip v-if="item.avatar" :alt="item.name" class="mt-0 mb-0" small>
                  <v-avatar size="23px !important">
                    <img :src="item.avatar">
                  </v-avatar>
                  <span v-html="item.displayName"></span>
                </v-chip>
                <code v-else>{{ item.displayName }}</code>
              </v-list-tile-title>
              <v-list-tile-sub-title>
                <v-icon v-if="!item.status" color="orange" title="Transaction failed">warning</v-icon>
                <span>{{ item.amount }} ETH</span>
                <span v-if="item.amountUSD"> / {{ item.amountUSD }} $</span>
                <span v-if="item.fee">
                  ( Fee: {{ item.fee }} ETH
                  <span v-if="item.feeUSD">/ {{ item.feeUSD }} $</span>
                  )
                </span>
              </v-list-tile-sub-title>
            </v-list-tile-content>
            <v-list-tile-action>
              <v-list-tile-action-text>{{ item.date.toLocaleDateString() }} - {{ item.date.toLocaleTimeString() }}</v-list-tile-action-text>
            </v-list-tile-action>
          </v-list-tile>
          <v-divider v-if="index + 1 < sortedTransaction.length" :key="index"></v-divider>
        </template>
      </v-list>
      <v-flex v-else-if="loadedBlocks == maxBlocksToLoad" class="text-xs-center">
        <v-chip color="white">
          <span>No transactions</span>
        </v-chip>
      </v-flex>
    </v-card>
  </v-flex>
</template>

<script>
import {searchFullName, getContractFromStorage, getContactFromStorage} from '../WalletAddressRegistry.js';
import {etherToUSD} from '../WalletUtils.js';

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
      lastBlockNumber: 0,
      finishedLoading: false,
      transactionsPerPage: 10,
      transactionsToLoad: 10,
      maxBlocksToLoad: 1000,
      loadedBlocks: 0,
      transactions: []
    };
  },
  computed: {
    sortedTransaction() {
      return this.transactions.slice(0).sort((t1, t2) => t2.date - t1.date);
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
            this.$emit("error", error);
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
          this.$emit("error", error);
        });
    }
  },
  methods: {
    init() {
      this.transactions = [];
      this.loadedBlocks = 0;

      return this.refreshTransactions(0);
    },
    refreshNewwestTransactions() {
      this.loading = true;
      return this.refreshTransactions(this.lastBlockNumber)
        .then(() => this.$emit("end-loading"))
        .finally(() => this.loading = false);
    },
    refreshTransactions(untilPreviousBlock) {
      let lastBlockNumberTmp = 0;

      // Retrive transactions from 1000 previous blocks (at maximum)
      // and display transactions sent/received by the current account
      return window.localWeb3.eth.getBlockNumber()
        .then(lastBlockNumber => lastBlockNumberTmp = lastBlockNumber)
        .then(lastBlockNumber => window.localWeb3.eth.getBlock(lastBlockNumber, true))
        .then((lastBlock) => this.addBlockTransactions(lastBlock, untilPreviousBlock))
        .then(() => this.lastBlockNumber = lastBlockNumberTmp);
    },
    addBlockTransactions(block, untilBlock) {
      if (!block) {
        throw new Error("Block not found");
      }

      // Don't display additional loaded blocks when refreshing the list
      if (!untilBlock) {
        this.loadedBlocks++;
      }

      // If we :
      //  * already searched inside 1000 block
      //  * or we reached the genesis block
      //  * or we already displayed 10 transactions
      // then stop searching
      if (block.number === 0
          || block.number <= untilBlock
          || (!untilBlock && this.transactionsToLoad <= this.transactions.length)
          || (!untilBlock && this.loadedBlocks >= this.maxBlocksToLoad)) {
        return false;
      }
      if (block.transactions && block.transactions.length) {

        const thiss = this;
        // Iterate over transactions from retrieved from block
        block.transactions.forEach(transaction => {
          // Make sure to not display transaction that hasn't a 'to' or 'from' address
          if (transaction.to && transaction.to.toLowerCase() === thiss.account.toLowerCase()
              || transaction.from && transaction.from.toLowerCase() === thiss.account.toLowerCase()) {
            window.localWeb3.eth.getTransactionReceipt(transaction.hash)
              .then(receipt => {
                // Calculate Transaction fees
                const transactionFeeInWei = receipt.gasUsed * transaction.gasPrice;
                const transactionFeeInEth = window.localWeb3.utils.fromWei(transactionFeeInWei.toString(), 'ether');
                const transactionFeeInUSD = etherToUSD(transactionFeeInEth);

                const isReceiver = transaction.to && transaction.to.toLowerCase() === thiss.account.toLowerCase();

                // Calculate sent/received amount
                const amount = window.localWeb3.utils.fromWei(transaction.value, 'ether');
                const amountUSD = etherToUSD(amount);
    
                const isFeeTransaction = parseFloat(amount) === 0;

                let displayedAddress = isReceiver ? transaction.from : transaction.to;

                let isContractCreationTransaction = false;

                if (!displayedAddress) {
                  displayedAddress = receipt.contractAddress;
                  isContractCreationTransaction = true;
                }

                // Retrieve user or space display name, avatar and id from sessionStorage
                const contactDetails = getContactFromStorage(displayedAddress, 'user', 'space');

                const transactionDetails = {
                  hash: transaction.hash,
                  titlePrefix: isReceiver ? 'Received from': isContractCreationTransaction ? 'Transaction spent on Contract creation ' : isFeeTransaction ? 'Transaction spent on' : 'Sent to',
                  displayName: contactDetails.name ? contactDetails.name : displayedAddress,
                  avatar: contactDetails.avatar,
                  name: null,
                  status: receipt.status,
                  color: isReceiver ? 'green' : 'red',
                  icon: isFeeTransaction ? 'fa-undo' : 'fa-exchange-alt',
                  amount: amount,
                  amountUSD: amountUSD,
                  gas: transaction.gas,
                  gasUsed: receipt.gasUsed,
                  gasPrice: transaction.gasPrice,
                  fee: transactionFeeInEth,
                  feeUSD: transactionFeeInUSD,
                  isContractCreation: receipt.contractAddress,
                  date: new Date(block.timestamp * 1000)
                };

                // If user/space details wasn't found on sessionStorage,
                // then display the transaction details and in // load name and avatar with a promise
                // From eXo Platform Server
                thiss.transactions.push(transactionDetails);

                if (!contactDetails || !contactDetails.name) {
                  // Test if address corresponds to a contract
                  getContractFromStorage(this.account, displayedAddress)
                    .then(contractDetails => {
                      if (contractDetails) {
                        transactionDetails.displayName = `Contract ${contractDetails.symbol}`;
                        transactionDetails.name = contractDetails.address;
                        this.$forceUpdate();
                        return false;
                      } else {
                        return true;
                      }
                    })
                    .then(continueSearch => {
                      if(continueSearch) {
                        // The address is not of type contract, so search correspondin user/space display name
                        return searchFullName(displayedAddress);
                      }
                    })
                    .then(item => {
                      if (item && item.name && item.name.length) {
                        transactionDetails.displayName = item.name;
                        transactionDetails.avatar = item.avatar;
                        transactionDetails.name = item.id;
                        // Vue didn't refresh automatically the list of transactions
                        // So this is used to re-read from transactions list the newly loaded details
                        this.$forceUpdate();
                        return true;
                      }
                      return false;
                    });
                }
              });
          }
        });
      }
      // Continue searching in previous block
      return window.localWeb3.eth.getBlock(block.parentHash, true)
        .then(this.addBlockTransactions);
    }
  }
};
</script>