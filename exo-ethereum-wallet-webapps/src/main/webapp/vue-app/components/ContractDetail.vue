<template>
  <v-flex v-if="contractDetails && contractDetails.title" id="accountDetail" class="text-xs-center white layout column">
    <v-card-title class="align-start accountDetailSummary">
      <v-layout column>
        <v-flex id="accountDetailTitle" class="mt-3">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">{{ contractDetails.icon }}</v-icon>
            Contract Details: {{ contractDetails.title }}
          </div>
          <h3 v-if="contractDetails.contractBalanceFiat" class="font-weight-light">Balance: {{ contractDetails.contractBalanceFiat }} {{ fiatSymbol }} / {{ contractDetails.contractBalance }} ether</h3>
          <h4 v-if="contractDetails.sellPrice" class="grey--text font-weight-light">Sell price: {{ contractDetails.sellPrice }} ether</h4>
          <h4 v-if="contractDetails.totalSupply" class="grey--text font-weight-light">Total supply: {{ totalSupply }} {{ contractDetails.symbol }}</h4>
          <h4 v-if="transferTransactionsCount" class="grey--text font-weight-light">Transfer transactions: {{ transferTransactionsCount }}</h4>
        </v-flex>

        <v-flex v-if="!isDisplayOnly" id="accountDetailActions">
          <!-- Send ether -->
          <send-ether-modal
            v-if="contractDetails.isOwner"
            :account="walletAddress"
            :balance="contractDetails.balance"
            :recipient="contractDetails.address"
            use-navigation
            @success="refreshBalance"
            @sent="newTransactionPending"
            @error="transactionError" />

          <!-- add/remove admin -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="addAdmin"
            title="Add administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to add"
            input-label="Habilitation level"
            input-placeholder="Choose a value between 1 and 5"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="removeAdmin"
            title="Remove administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to remove"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />

          <!-- approve/disapprove account -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 1"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="approveAccount"
            title="Approve account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to approve"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 1"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="disapproveAccount"
            title="Disapprove account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to disapprove"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />

          <!-- pause/unpause contract -->
          <contract-admin-modal
            v-if="!contractDetails.isPaused && contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="pause"
            title="Pause contract"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />
          <contract-admin-modal
            v-if="contractDetails.isPaused && contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="unPause"
            title="Unpause contract"
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />

          <!-- set sell price -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="setSellPrice"
            title="Set sell price"
            input-label="Token sell price"
            input-placeholder="Token sell price in ether"
            convert-wei
            @success="successTransaction"
            @sent="newTransactionPending"
            @error="transactionError" />
        </v-flex>
        <v-btn icon class="rightIcon" @click="$emit('back')">
          <v-icon>close</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <v-tabs v-if="contractDetails.contractType > 0" v-model="selectedTab" grow>
      <v-tabs-slider color="primary" />
      <v-tab key="transactions">Transactions{{ totalTransactionsCount ? ` (${totalTransactionsCount})` : '' }}</v-tab>
      <v-tab key="approvedAccounts">Approved accounts</v-tab>
      <v-tab key="adminAccounts">Admin accounts</v-tab>
    </v-tabs>
    <v-tabs-items v-if="contractDetails.contractType > 0" v-model="selectedTab">
      <v-tab-item key="transactions">
        <transactions-list
          id="transactionsList"
          ref="transactionsList"
          :network-id="networkId"
          :account="contractDetails.address"
          :contract-details="contractDetails"
          :fiat-symbol="fiatSymbol"
          :error="error"
          display-full-transaction
          @loaded="computeTransactionsCount"
          @error="error = $event" />
      </v-tab-item>
      <v-tab-item key="approvedAccountsTable">
        <v-progress-linear v-if="loadingWallets" indeterminate color="primary" class="mb-0 mt-0" />
        <v-data-table :items="approvedAccounts" hide-actions hide-headers>
          <template slot="items" slot-scope="props">
            <td>
              <v-avatar size="36px">
                <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
              </v-avatar>
            </td>
            <td>
              {{ props.item.name }}
            </td>
          </template>
        </v-data-table>
      </v-tab-item>
      <v-tab-item key="adminAccountsTable">
        <v-progress-linear v-if="loadingWallets" indeterminate color="primary" class="mb-0 mt-0" />
        <v-data-table :items="adminAccounts" hide-actions hide-headers>
          <template slot="items" slot-scope="props">
            <td>
              <v-avatar size="36px">
                <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
              </v-avatar>
            </td>
            <td>
              {{ props.item.name }}
            </td>
            <td>
              {{ props.item.accountAdminLevel[contractDetails.address] }} level
            </td>
          </template>
        </v-data-table>
      </v-tab-item>
    </v-tabs-items>
    <transactions-list
      v-if="contractDetails.contractType === 0"
      id="transactionsList"
      ref="transactionsList"
      :network-id="networkId"
      :account="contractDetails.address"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :error="error"
      display-full-transaction
      @loaded="computeTransactionsCount"
      @error="error = $event" />
  </v-flex>
</template>

<script>
import SendEtherModal from './SendEtherModal.vue';
import ContractAdminModal from './ContractAdminModal.vue';
import TransactionsList from './TransactionsList.vue';

import {retrieveContractDetails} from '../WalletToken.js';
import {addTransaction} from '../WalletTransactions.js';
import {etherToFiat, computeBalance, convertTokenAmountReceived} from '../WalletUtils.js';

export default {
  components: {
    SendEtherModal,
    ContractAdminModal,
    TransactionsList
  },
  props: {
    isDisplayOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    loadingWallets: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    wallets: {
      type: Object,
      default: function() {
        return {};
      }
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      selectedTab: 0,
      transferTransactionsCount: 0,
      totalTransactionsCount: 0,
      approvedAccounts: [],
      adminAccounts: [],
      error: null
    };
  },
  computed: {
    totalSupply() {
      return this.contractDetails && this.contractDetails.totalSupply ? convertTokenAmountReceived(this.contractDetails.totalSupply, this.contractDetails.decimals) : 0;
    }
  },
  watch: {
    wallets() {
      this.retrieveAccountsDetails();
    },
    contractDetails() {
      this.error = null;
      this.totalTransactionsCount = 0;
      this.transferTransactionsCount = 0;
      this.retrieveAccountsDetails();
    }
  },
  methods: {
    successSendingEther() {
      this.refreshB
        .then(() => {
          this.$emit('success', this.contractDetails);
          this.$forceUpdate();
        });
    },
    retrieveAccountsDetails() {
      this.approvedAccounts = [];
      this.adminAccounts = [];
      if (this.wallets && this.contractDetails && this.contractDetails.contractType > 0) {
        this.wallets.filter(wallet => wallet.address).forEach(wallet => {
          if (!wallet.approved) {
            wallet.approved = {};
          }
          if (!wallet.accountAdminLevel) {
            wallet.accountAdminLevel = {};
          }
          if (wallet.approved[this.contractDetails.address]) {
            if (wallet.approved[this.contractDetails.address] === 'approved') {
              this.approvedAccounts.push(wallet);
            }
          } else {
            this.contractDetails.contract.methods.isApprovedAccount(wallet.address).call()
              .then(approved => {
                wallet.approved[this.contractDetails.address] = approved ? 'approved' : 'disapproved';
                if (approved) {
                  this.approvedAccounts.push(wallet);
                }
              });
          }
          if (wallet.accountAdminLevel[this.contractDetails.address]) {
            if (wallet.accountAdminLevel[this.contractDetails.address] > 0 && wallet.accountAdminLevel[this.contractDetails.address] !== 'not admin') {
              this.adminAccounts.push(wallet);
            }
          } else {
            this.contractDetails.contract.methods.getAdminLevel(wallet.address).call()
              .then(level => {
                level = Number(level);
                wallet.accountAdminLevel[this.contractDetails.address] = level ? level : 'not admin';
                if (level) {
                  this.adminAccounts.push(wallet);
                }
              });
          }
        });
      }
    },
    newTransactionPending(transaction, contractDetails) {
      transaction.isSender = true;
      if (!contractDetails) {
        contractDetails = this.contractDetails;
      }
      if (this.$refs.transactionsList) {
        this.$refs.transactionsList.addTransaction(transaction, contractDetails);
      } else {
        addTransaction(this.networkId,
          contractDetails.address,
          contractDetails,
          [],
          transaction);
      }
      if (this.contractDetails && transaction.value > 0) {
        this.$set(this.contractDetails, "loadingBalance", true);
      }
      this.$forceUpdate();
    },
    transactionError(error) {
      this.error = String(error);
      this.$forceUpdate();
    },
    successTransaction(txHash, contractDetails, methodName, autoCompleteValue, inputValue) {
      if (methodName === 'approveAccount') {
        const wallet = this.wallets.find(wallet => wallet.address === autoCompleteValue);
        if (wallet && wallet.address) {
          if (!wallet.approved) {
            wallet.approved = {};
          }
          wallet.approved[contractDetails.address] = 'approved';
          this.approvedAccounts.push(wallet);
        }
      } else if (methodName === 'disapproveAccount') {
        const walletIndex = this.approvedAccounts.findIndex(wallet => wallet.address === autoCompleteValue);
        if (walletIndex >= 0) {
          this.approvedAccounts[walletIndex].approved[contractDetails.address] = 'disapproved';
          this.approvedAccounts.splice(walletIndex, 1);
        }
      } else if (methodName === 'addAdmin') {
        const wallet = this.wallets.find(wallet => wallet.address === autoCompleteValue);
        if (wallet && wallet.address) {
          if (!wallet.accountAdminLevel) {
            wallet.accountAdminLevel = {};
          }
          wallet.accountAdminLevel[contractDetails.address] = inputValue;
          if (inputValue) {
            const adminAccount = this.adminAccounts.find(wallet => wallet.address === autoCompleteValue);
            if (!adminAccount) {
              this.adminAccounts.push(wallet);
            }
          }
        }
      } else if (methodName === 'removeAdmin') {
        const walletIndex = this.adminAccounts.findIndex(wallet => wallet.address === autoCompleteValue);
        if (walletIndex >= 0) {
          this.adminAccounts[walletIndex].approved[contractDetails.address] = 'not admin';
          this.adminAccounts.splice(walletIndex, 1);
        }
      } else if (methodName === 'pause' || methodName === 'unPause') {
        this.contractDetails.contract.methods.isPaused().call()
          .then(isPaused =>  {
            this.$set(this.contractDetails, "isPaused", isPaused);
          });
      } else if (methodName === 'setSellPrice') {
        this.contractDetails.contract.methods.getSellPrice().call()
          .then(sellPrice => {
            if (sellPrice) {
              this.$set(this.contractDetails, "sellPrice", window.localWeb3.utils.fromWei(sellPrice, "ether"));
            } else {
              this.$set(this.contractDetails, "sellPrice", 0);
            }
          });
      }
      this.$forceUpdate();
    },
    computeTransactionsCount(transactions) {
      this.totalTransactionsCount = Object.values(transactions).length;
      this.transferTransactionsCount = Object.values(transactions).filter(transaction => transaction.contractMethodName && transaction.contractMethodName === 'transfer').length;
    },
    refreshBalance() {
      this.$set(this.contractDetails, "loadingBalance", false);
      return computeBalance(this.contractDetails.address)
        .then(contractBalance => {
          if (contractBalance) {
            this.$set(this.contractDetails, "contractBalance", contractBalance.balance);
            this.$set(this.contractDetails, "contractBalanceFiat", contractBalance.balanceFiat);
          }
        });
    }
  }
};
</script>