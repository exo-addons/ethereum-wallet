<template>
  <v-flex flat>
    <div v-if="error" class="alert alert-error v-content">
      <i class="uiIconError"></i>{{ error }}
    </div>
    <v-container>
      <v-layout>
        <v-flex md4 xs12>
          <v-switch v-model="displayUsers" label="Display users" />
        </v-flex>
        <v-flex md4 xs12>
          <v-switch v-model="displayDisabledUsers" label="Display disabled users" />
        </v-flex>
        <v-flex md4 xs12>
          <v-switch v-model="displaySpaces" label="Display spaces" />
        </v-flex>
      </v-layout>
      <v-flex>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search in name, address"
          single-line
          hide-details />
      </v-flex>
    </v-container>
    <v-data-table
      :headers="walletHeaders"
      :items="filteredWallets"
      :loading="loadingWallets"
      hide-actions>
      <template slot="items" slot-scope="props">
        <td class="clickable" @click="openAccountDetail(props.item)">
          <v-avatar size="36px">
            <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
          </v-avatar>
        </td>
        <td class="clickable" @click="openAccountDetail(props.item)">
          <template v-if="props.item.enabled">
            {{ props.item.name }}
          </template>
          <span v-else>
            <del class="red--text">{{ props.item.name }}</del> (Disabled)
          </span>
        </td>
        <td>
          <a
            v-if="addressEtherscanLink"
            :href="`${addressEtherscanLink}${props.item.address}`"
            target="_blank"
            title="Open on etherscan">
            {{ props.item.address }}
          </a> <span v-else>
            {{ props.item.address }}
          </span>
        </td>
        <td class="clickable text-xs-right" @click="openAccountDetail(props.item)">
          <v-progress-circular
            v-if="props.item.loadingBalancePrincipal"
            :title="loadingWallets ? 'Loading balance' : 'A transaction is in progress'"
            color="primary"
            class="mr-4"
            indeterminate
            size="20" />
          <span
            v-else-if="loadingWallets && props.item.loadingBalancePrincipal !== false"
            title="Loading balance...">
            loading...
          </span>
          <template v-else-if="props.item.balancePrincipal">
            {{ toFixed(props.item.balancePrincipal) }} {{ principalContract && principalContract.symbol ? principalContract.symbol : '' }}
            <v-btn
              class="bottomNavigationItem transparent"
              title="Send funds"
              flat
              icon
              @click="openSendFundsModal($event, props.item, true)">
              <v-icon>
                send
              </v-icon>
            </v-btn>
          </template>
          <template v-else>
            -
          </template>
        </td>
        <td class="clickable text-xs-right" @click="openAccountDetail(props.item)">
          <v-progress-circular
            v-if="props.item.loadingBalance"
            :title="loadingWallets ? 'Loading balance' : 'A transaction is in progress'"
            color="primary"
            class="mr-4"
            indeterminate
            size="20" />
          <span
            v-else-if="loadingWallets && props.item.loadingBalance !== false"
            title="Loading balance...">
            loading...
          </span>
          <template v-else>
            {{ toFixed(props.item.balance) }} eth
            <v-btn
              class="bottomNavigationItem transparent"
              title="Send funds"
              flat
              icon
              @click="openSendFundsModal($event, props.item)">
              <v-icon>
                send
              </v-icon>
            </v-btn>
          </template>
        </td>
        <td>
          <v-menu offset-y>
            <v-btn
              slot="activator"
              icon
              small>
              <v-icon size="20px">fa-ellipsis-v</v-icon>
            </v-btn>
            <v-list flat class="pt-0 pb-0">
              <v-list-tile @click="removeWalletAssociation(props.item)">
                <v-list-tile-title>Remove wallet</v-list-tile-title>
              </v-list-tile>
            </v-list>
          </v-menu>
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
        is-administration
        @back="back()" />
    </v-navigation-drawer>

    <send-funds-modal
      ref="sendFundsModal"
      :accounts-details="accountsDetails"
      :principal-account="principalAccountAddress"
      :refresh-index="refreshIndex"
      :network-id="networkId"
      :wallet-address="walletAddress"
      :default-message="initialFundsMessage"
      no-button
      display-all-accounts
      add-pending-to-receiver
      @success="refreshBalance"
      @pending="$emit('pending', $event)"
      @error="refreshBalance(null, null, $event)" />
  </v-flex>
</template>
<script>
import SendFundsModal from '../SendFundsModal.vue';
import AccountDetail from '../AccountDetail.vue';

import {getWallets, removeWalletAssociation, computeBalance, convertTokenAmountReceived} from '../../WalletUtils.js';

export default {
  components: {
    SendFundsModal,
    AccountDetail,
  },
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      },
    },
    addressEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
    refreshIndex: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    principalAccount: {
      type: Object,
      default: function() {
        return null;
      },
    },
    principalAccountAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    principalContract: {
      type: Object,
      default: function() {
        return null;
      },
    },
    initialFunds: {
      type: Object,
      default: function() {
        return null;
      },
    },
    accountsDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      search: null,
      loadingWallets: false,
      appInitialized: false,
      displayUsers: true,
      displaySpaces: true,
      displayDisabledUsers: true,
      sameConfiguredNetwork: true,
      selectedTransactionHash: null,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      selectedWalletAddress: null,
      selectedWallet: null,
      selectedWalletDetails: null,
      initialFundsMessage: null,
      error: null,
      wallets: [],
      walletHeaders: [
        {
          text: '',
          align: 'left',
          sortable: false,
          value: 'avatar',
        },
        {
          text: 'Name',
          align: 'left',
          sortable: true,
          value: 'name',
        },
        {
          text: 'Address',
          align: 'center',
          sortable: false,
          value: 'address',
        },
        {
          text: 'Principal balance',
          align: 'center',
          value: 'balancePrincipal',
        },
        {
          text: 'Ether balance',
          align: 'center',
          value: 'balance',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: '',
        },
      ],
    };
  },
  computed: {
    filteredWallets() {
      if (this.displayUsers && this.displayDisabledUsers && this.displaySpaces && !this.search) {
        return this.wallets.filter(wallet => wallet && wallet.address);
      } else {
        return this.wallets.filter(wallet => wallet && wallet.address && (this.displayUsers || wallet.type !== 'user') && (this.displaySpaces || wallet.type !== 'space') && (this.displayDisabledUsers || wallet.enabled || wallet.type !== 'user') && (!this.search || wallet.name.toLowerCase().indexOf(this.search) >= 0 || wallet.address.toLowerCase().indexOf(this.search) >= 0));
      }
    }
  },
  watch: {
    loadingWallets(value) {
      this.$emit('loading-wallets-changed', value);
    },
    seeAccountDetails() {
      if (this.seeAccountDetails) {
        $('body').addClass('hide-scroll');
        const thiss = this;
        setTimeout(() => {
          thiss.seeAccountDetailsPermanent = true;
        }, 200);
      } else {
        $('body').removeClass('hide-scroll');
        this.seeAccountDetailsPermanent = false;
      }
    },
  },
  methods: {
    init(appInitialized) {
      this.initialFundsMessage = window.walletSettings.initialFundsRequestMessage;
      if(!appInitialized || this.appInitialized) {
        return;
      }
      this.appInitialized = this.appInitialized || appInitialized;
      if(this.loadingWallets) {
        return;
      }
      this.loadingWallets = true;
      getWallets()
        .then((wallets) => (this.wallets = wallets))
        .then(() => {
          this.$emit('wallets-loaded', this.wallets);
          return this.loadWalletsBalances(this.principalContract, this.wallets.sort(this.sortByName));
        })
        .finally(() => {
          this.loadingWallets = false;
        });
    },
    sortByName(a, b) {
      // To use same Vuetify datable sort algorithm
      const sortA = a.name.toLocaleLowerCase();
      const sortB = b.name.toLocaleLowerCase();
      return (sortA > sortB && 1) || (sortA < sortB && (-1)) || 0;
    },
    loadWalletsBalances(accountDetails, wallets, i) {
      if(!wallets || !wallets.length) {
        return;
      }
      if(!i) {
        i = 0;
      }
      if(i >= wallets.length) {
        return;
      }
      return this.computeBalance(accountDetails, wallets[i])
        .then(() => this.loadWalletsBalances(accountDetails, wallets, ++i));
    },
    openSendFundsModal(event, wallet, principal) {
      event.preventDefault();
      event.stopPropagation();

      if (wallet && wallet.address && (wallet.type || wallet.contractType)) {
        if (principal) {
          if (this.principalAccount && this.principalAccount.value) {
            const principalInitialFund = this.initialFunds.find((account) => account.address === this.principalAccount.value);
            this.$refs.sendFundsModal.prepareSendForm(wallet.type ? wallet.id : wallet.address, wallet.type, wallet.type ? principalInitialFund && principalInitialFund.amount : null, this.principalAccount.value);
          } else {
            console.error('No selected principal account found');
          }
        } else {
          const etherInitialFund = this.initialFunds.find((account) => account.address === 'ether');
          this.$refs.sendFundsModal.prepareSendForm(wallet.type ? wallet.id : wallet.address, wallet.type, wallet.type ? etherInitialFund && etherInitialFund.amount : null);
        }
      } else {
        console.debug("Wallet object doesn't have a type or an address", wallet);
      }
    },
    refreshBalance(accountDetails, address, error) {
      this.$emit('refresh-balance', accountDetails, address, error);
    },
    computeBalance(accountDetails, wallet, ignoreUpdateLoadingBalanceParam) {
      if(!wallet.address) {
        return Promise.resolve(null);
      }
      this.$set(wallet, 'loadingBalance', true);
      return computeBalance(wallet.address)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          } else {
            this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
          }
        })
        .then(() => {
          // check if we should reload contract balance too
          if (accountDetails && accountDetails.contract && accountDetails.isContract) {
            this.$set(wallet, 'loadingBalancePrincipal', true);
            return accountDetails.contract.methods
              .balanceOf(wallet.address)
              .call()
              .then((balance, error) => {
                if (error) {
                  throw new Error('Invalid contract address');
                }
                balance = String(balance);
                balance = convertTokenAmountReceived(balance, accountDetails.decimals);
                this.$set(wallet, 'balancePrincipal', balance);
              })
              .finally(() => {
                if (!ignoreUpdateLoadingBalanceParam) {
                  this.$set(wallet, 'loadingBalancePrincipal', false);
                }
              });
          }
        })
        .catch((error) => {
          this.$set(wallet, 'icon', 'warning');
          this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          this.error = String(error);
        })
        .finally(() => {
          if (!ignoreUpdateLoadingBalanceParam) {
            this.$set(wallet, 'loadingBalance', false);
            if (wallet.loadingBalancePrincipal && accountDetails && accountDetails.contract && accountDetails.isContract) {
              this.$set(wallet, 'loadingBalancePrincipal', false);
            }
          }
        });
    },
    openAccountDetail(wallet, hash) {
      this.selectedTransactionHash = hash;
      this.selectedWalletAddress = wallet.address;
      this.selectedWallet = wallet;
      this.computeWalletDetails(wallet);
      this.seeAccountDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay')
          .off('click')
          .on('click', (event) => {
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
        title: 'ether',
        icon: 'fab fa-ethereum',
        symbol: 'ether',
        isContract: false,
        address: this.selectedWalletAddress,
        balance: wallet.balance,
        balanceFiat: wallet.balanceFiat,
        details: wallet,
      };
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedWalletAddress = null;
      this.selectedWalletDetails = null;
    },
    removeWalletAssociation(wallet) {
      return removeWalletAssociation(wallet.address)
        .then((result) => {
          if(result) {
            const index = this.wallets.indexOf(wallet);
            if(index >= 0) {
              this.wallets.splice(index, 1);
            }
          } else {
            this.error = 'An error occurred while removing wallet';
          }
        })
        .catch(e => this.error = String(e));
    },
  },
};
</script>
