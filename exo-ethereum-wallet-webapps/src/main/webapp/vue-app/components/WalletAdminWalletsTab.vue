<template>
  <v-card v-if="!loading" flat>
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-data-table :headers="walletHeaders" :items="wallets" :loading="loadingWallets" hide-actions>
      <template slot="items" slot-scope="props">
        <td class="clickable" @click="openAccountDetail(props.item)">
          <v-avatar size="36px">
            <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
          </v-avatar>
        </td>
        <td class="clickable" @click="openAccountDetail(props.item)">
          {{ props.item.name }}
        </td>
        <td>
          <a
            v-if="addressEtherscanLink"
            :href="`${addressEtherscanLink}${props.item.address}`"
            target="_blank"
            title="Open on etherscan">{{ props.item.address }}</a>
          <span v-else>{{ props.item.address }}</span>
        </td>
        <td class="clickable text-xs-right" @click="openAccountDetail(props.item)">
          <v-badge v-if="props.item.loadingBalancePrincipal" color="red" right title="A transaction is in progress">
            <v-progress-circular color="primary" indeterminate size="20"></v-progress-circular>
          </v-badge>
          <template v-if="props.item.balancePrincipal">
            {{ toFixed(props.item.balancePrincipal) }} {{ principalContract && principalContract.symbol ? principalContract.symbol : '' }}
            <v-btn 
              class="bottomNavigationItem transparent"
              title="Send funds"
              flat
              icon
              @click="openSendFundsModal($event, props.item, true)">
              <v-icon>send</v-icon>
            </v-btn>
          </template>
          <template v-else>-</template>
        </td>
        <td class="clickable text-xs-right" @click="openAccountDetail(props.item)">
          <v-badge v-if="props.item.loadingBalance" color="red" right title="A transaction is in progress">
            <v-progress-circular color="primary" indeterminate size="20"></v-progress-circular>
          </v-badge>
          <template>
            {{ toFixed(props.item.balance) }} eth
            <v-btn 
              class="bottomNavigationItem transparent"
              title="Send funds"
              flat
              icon
              @click="openSendFundsModal($event, props.item)">
              <v-icon>send</v-icon>
            </v-btn>
          </template>
        </td>
      </template>
    </v-data-table>

    <!-- The selected account detail -->
    <v-navigation-drawer
      id="accountDetailsDrawer"
      v-model="seeAccountDetails"
      fixed
      temporary
      right
      stateless
      width="700"
      max-width="100vw">

      <account-detail
        ref="accountDetail"
        :fiat-symbol="fiatSymbol"
        :network-id="networkId"
        :wallet-address="selectedWalletAddress"
        :contract-details="selectedWalletDetails"
        :selected-transaction-hash="selectedTransactionHash"
        :wallet="selectedWallet"
        is-read-only
        is-display-only
        @back="back()"/>
    </v-navigation-drawer>

    <send-funds-modal
      ref="sendFundsModal"
      :accounts-details="accountsDetails"
      :principal-account="principalAccountAddress"
      :refresh-index="refreshIndex"
      :network-id="networkId"
      :wallet-address="walletAddress"
      no-button
      display-all-accounts
      add-pending-to-receiver
      @success="refreshBalance"
      @pending="$emit('pending', $event)"
      @error="refreshBalance(null, null, $event)" />
  </v-card>
</template>
<script>
import SendFundsModal from './SendFundsModal.vue';
import AccountDetail from './AccountDetail.vue';

import {getWallets, computeBalance, convertTokenAmountReceived} from '../WalletUtils.js';

export default {
  components: {
    SendFundsModal,
    AccountDetail
  },
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    addressEtherscanLink: {
      type: String,
      default: function() {
        return null;
      }
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    principalAccount: {
      type: Object,
      default: function() {
        return null;
      }
    },
    principalAccountAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    principalContract: {
      type: Object,
      default: function() {
        return null;
      }
    },
    initialFunds: {
      type: Object,
      default: function() {
        return null;
      }
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return null;
      }
    }
  },
  data () {
    return {
      loadingWallets: false,
      sameConfiguredNetwork: true,
      selectedTransactionHash: null,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      selectedWalletAddress: null,
      selectedWallet: null,
      selectedWalletDetails: null,
      error: null,
      wallets: [],
      walletHeaders: [
        {
          text: '',
          align: 'left',
          sortable: false,
          value: 'avatar'
        },
        {
          text: 'Name',
          align: 'left',
          sortable: true,
          value: 'name'
        },
        {
          text: 'Address',
          align: 'center',
          sortable: false,
          value: 'address'
        },
        {
          text: 'Principal balance',
          align: 'center',
          value: 'balancePrincipal'
        },
        {
          text: 'Ether balance',
          align: 'center',
          value: 'balance'
        }
      ]
    };
  },
  watch: {
    loadingWallets(value) {
      this.$emit("loading-wallets-changed", value);
    },
    seeAccountDetails() {
      if (this.seeAccountDetails) {
        $("body").addClass("hide-scroll");
        const thiss = this;
        setTimeout(() => {
          thiss.seeAccountDetailsPermanent = true;
        }, 200);
      } else {
        $("body").removeClass("hide-scroll");
        this.seeAccountDetailsPermanent = false;
      }
    }
  },
  methods: {
    init() {
      this.loadingWallets = true;
      getWallets()
        .then(wallets => this.wallets = wallets)
        .then(() => {
          this.$emit("wallets-loaded", this.wallets);
          this.wallets.forEach(wallet => {
            if(wallet && wallet.address) {
              this.$set(wallet, "loadingBalance", true);
              this.$set(wallet, "loadingBalancePrincipal", true);
              this.computeBalance(this.principalContract, wallet);
            }
          });
        })
        .finally(() => {
          this.loadingWallets = false;
        });
    },
    openSendFundsModal(event, wallet, principal) {
      event.preventDefault();
      event.stopPropagation();

      if (wallet && wallet.address && (wallet.type || wallet.contractType)) {
        if (principal) {
          if(this.principalAccount && this.principalAccount.value) {
            const principalInitialFund = this.initialFunds.find(account => account.address === this.principalAccount.value);
            this.$refs.sendFundsModal.prepareSendForm(wallet.type ? wallet.id : wallet.address, wallet.type, wallet.type ? principalInitialFund && principalInitialFund.amount : null, this.principalAccount.value);
          } else {
            console.error("No selected principal account found");
          }
        } else {
          const etherInitialFund = this.initialFunds.find(account => account.address === 'ether');
          this.$refs.sendFundsModal.prepareSendForm(wallet.type ? wallet.id : wallet.address, wallet.type, wallet.type ? etherInitialFund && etherInitialFund.amount : null);
        }
      } else {
        console.debug("Wallet object doesn't have a type or an address", wallet);
      }
    },
    refreshBalance(accountDetails, address, error) {
      this.$emit("refresh-balance", accountDetails, address, error);
    },
    computeBalance(accountDetails, wallet, ignoreUpdateLoadingBalanceParam) {
      computeBalance(wallet.address)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          } else {
            this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
          }
          if(!ignoreUpdateLoadingBalanceParam) {
            this.$set(wallet, "loadingBalance", false);
          }
          this.$forceUpdate();
        })
        .catch(error => {
          this.error = String(error);
        });
      if (accountDetails && accountDetails.contract && accountDetails.isContract) {
        accountDetails.contract.methods.balanceOf(wallet.address).call()
          .then((balance, error) => {
            if (error) {
              if(!ignoreUpdateLoadingBalanceParam) {
                this.$set(wallet, "loadingBalancePrincipal", false);
              }
              throw new Error('Invalid contract address');
            }
            balance = String(balance);
            balance = convertTokenAmountReceived(balance, accountDetails.decimals);
            this.$set(wallet, 'balancePrincipal', balance);
            if(!ignoreUpdateLoadingBalanceParam) {
              this.$set(wallet, "loadingBalancePrincipal", false);
            }
          });
      }
    },
    openAccountDetail(wallet, hash) {
      this.selectedTransactionHash = hash;
      this.selectedWalletAddress = wallet.address;
      this.selectedWallet = wallet;
      this.computeWalletDetails(wallet);
      this.seeAccountDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay').off('click').on('click', event => {
          thiss.back();
        });
      });
    },
    computeWalletDetails(wallet) {
      if (!this.selectedWalletAddress) {
        this.selectedWalletDetails = null;
        return;
      }
      this.selectedWalletDetails = {
        title : 'ether',
        icon : 'fab fa-ethereum',
        symbol : 'ether',
        isContract : false,
        address : this.selectedWalletAddress,
        balance : wallet.balance,
        balanceFiat : wallet.balanceFiat,
        details: wallet
      };
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedWalletAddress = null;
      this.selectedWalletDetails = null;
    }
  }
};
</script>