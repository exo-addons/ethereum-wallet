<template>
  <v-app
    id="WalletAdminApp"
    color="transaprent"
    flat>
    <main>
      <v-layout column>
        <v-flex>
          <v-card class="applicationToolbar mb-3" flat>
            <v-flex class="pl-3 pr-3 pt-2 pb-2">
              <strong>Wallet administration</strong>
            </v-flex>
          </v-card>
        </v-flex>
        <v-flex class="white text-xs-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <wallet-setup
            ref="walletSetup"
            :wallet-address="originalWalletAddress"
            :refresh-index="refreshIndex"
            :loading="loading"
            class="mb-3"
            is-administration
            @loading="loadingContracts = true"
            @end-loading="loadingContracts = false"
            @refresh="init()"
            @error="
              loadingContracts = false;
              error = $event;
            " />

          <v-dialog
            v-model="loading"
            persistent
            width="300">
            <v-card color="primary" dark>
              <v-card-text>
                Loading ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <wallet-summary
            v-if="walletAddress && accountsDetails"
            ref="walletSummary"
            :accounts-details="accountsDetails"
            :overview-accounts="overviewAccounts"
            :principal-account="principalAccountAddress"
            :refresh-index="refreshIndex"
            :network-id="networkId"
            :wallet-address="walletAddress"
            :ether-balance="walletAddressEtherBalance"
            :total-balance="walletAddressEtherBalance"
            :total-fiat-balance="walletAddressFiatBalance"
            :fiat-symbol="fiatSymbol"
            :loading="loading"
            is-maximized
            hide-actions
            @error="error = $event" />

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
            <v-tab
              v-if="sameConfiguredNetwork && isAdmin"
              key="general"
              href="#general">
              Settings
            </v-tab>
            <v-tab
              v-if="sameConfiguredNetwork && isAdmin"
              key="funds"
              href="#funds">
              Initial accounts funds
            </v-tab>
            <v-tab
              v-if="isAdmin"
              key="network"
              href="#network">
              Network
            </v-tab>
            <v-tab
              v-if="sameConfiguredNetwork"
              key="contracts"
              href="#contracts">
              Contracts
            </v-tab>
            <v-tab
              v-if="sameConfiguredNetwork && isAdmin"
              key="wallets"
              href="#wallets">
              Wallets
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item
              v-if="sameConfiguredNetwork && isAdmin"
              id="general"
              value="general">
              <general-tab
                ref="generalTab"
                :wallet-address="walletAddress"
                :loading="loading"
                :loading-settings="loadingSettings"
                :ether-balance="walletAddressEtherBalance"
                :contracts="contracts"
                :same-configured-network="sameConfiguredNetwork"
                @principal-contract-loaded="principalContract = $event"
                @principal-account-loaded="principalAccount = $event"
                @principal-account-address-loaded="principalAccountAddress = $event"
                @accounts-details-loaded="accountsDetails = $event"
                @overview-accounts-loaded="overviewAccounts = $event"
                @save="saveGlobalSettings" />
            </v-tab-item>
            <v-tab-item
              v-if="sameConfiguredNetwork && isAdmin"
              id="funds"
              value="funds">
              <initial-funds-tab
                ref="fundsTab"
                :default-network-id="defaultNetworkId"
                :loading="loading"
                :contracts="contracts"
                :same-configured-network="sameConfiguredNetwork"
                @initial-funds-loaded="initialFunds = $event"
                @save="saveGlobalSettings" />
            </v-tab-item>
            <v-tab-item id="network" value="network">
              <network-tab
                v-if="isAdmin"
                ref="networkTab"
                :network-id="networkId"
                :default-network-id="defaultNetworkId"
                :loading="loading"
                :principal-contract="principalContract"
                :fiat-symbol="fiatSymbol"
                :same-configured-network="sameConfiguredNetwork"
                @save="saveGlobalSettings" />
            </v-tab-item>

            <v-tab-item
              v-if="sameConfiguredNetwork"
              id="contracts"
              value="contracts">
              <contracts-tab
                ref="contractsTab"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :loading="loading"
                :loading-wallets="loadingWallets"
                :fiat-symbol="fiatSymbol"
                :address-etherscan-link="addressEtherscanLink"
                :token-etherscan-link="tokenEtherscanLink"
                :wallets="wallets"
                :same-configured-network="sameConfiguredNetwork"
                @contract-list-modified="$refs.walletsTab && $refs.walletsTab.init()"
                @pending-transaction="watchPendingTransaction"
                @contracts-loaded="contracts = $event" />
            </v-tab-item>

            <v-tab-item
              v-if="sameConfiguredNetwork && isAdmin"
              id="wallets"
              value="wallets">
              <wallets-tab
                ref="walletsTab"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :loading="loading"
                :fiat-symbol="fiatSymbol"
                :refresh-index="refreshIndex"
                :address-etherscan-link="addressEtherscanLink"
                :principal-account-address="principalAccountAddress"
                :principal-account="principalAccount"
                :principal-contract="principalContract"
                :initial-funds="initialFunds"
                :accounts-details="accountsDetails"
                :same-configured-network="sameConfiguredNetwork"
                @pending="pendingTransaction"
                @wallets-loaded="wallets = $event"
                @loading-wallets-changed="loadingWallets = $event"
                @refresh-balance="refreshCurrentWalletBalances" />
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import GeneralTab from './WalletAdminSettingsTab.vue';
import InitialFundsTab from './WalletAdminInitialFundsTab.vue';
import NetworkTab from './WalletAdminNetworkTab.vue';
import ContractsTab from './WalletAdminContractsTab.vue';
import WalletsTab from './WalletAdminWalletsTab.vue';

import WalletSetup from '../WalletSetup.vue';
import WalletSummary from '../WalletSummary.vue';

import * as constants from '../../WalletConstants.js';
import {saveContractAddressOnServer} from '../../WalletToken.js';
import {initWeb3, initSettings, watchTransactionStatus, computeBalance, getWallets, convertTokenAmountReceived, getTokenEtherscanlink, getAddressEtherscanlink} from '../../WalletUtils.js';

export default {
  components: {
    GeneralTab,
    InitialFundsTab,
    NetworkTab,
    ContractsTab,
    WalletsTab,
    WalletSummary,
    WalletSetup,
  },
  data() {
    return {
      loading: false,
      loadingSettings: false,
      loadingContracts: false,
      isAdmin: false,
      selectedTab: true,
      sameConfiguredNetwork: true,
      fiatSymbol: '$',
      refreshIndex: 1,
      originalWalletAddress: null,
      walletAddress: null,
      walletAddressEtherBalance: null,
      walletAddressFiatBalance: null,
      principalContract: null,
      principalAccount: null,
      principalAccountAddress: null,
      accountsDetails: null,
      overviewAccounts: null,
      loadingWallets: false,
      networkId: null,
      tokenEtherscanLink: null,
      addressEtherscanLink: null,
      selectedTransactionHash: null,
      initialFunds: [],
      contracts: [],
      wallets: [],
    };
  },
  created() {
    this.init()
      .then(() => (this.tokenEtherscanLink = getTokenEtherscanlink(this.networkId)))
      .then(() => (this.addressEtherscanLink = getAddressEtherscanlink(this.networkId)));
  },
  methods: {
    init(ignoreContracts) {
      this.loading = true;
      this.loadingSettings = true;
      this.loadingContracts = true;
      this.showAddContractModal = false;
      this.forceUpdate();
      this.selectedOverviewAccounts = [];
      this.error = null;

      return initSettings()
        .then(() => {
          if (!window.walletSettings) {
            this.forceUpdate();
            throw new Error('Wallet settings are empty for current user');
          }
          this.isAdmin = window.walletSettings.isAdmin;
        })
        .then(() => ignoreContracts || initWeb3(false, true))
        .catch((error) => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error connecting to network', error);
            this.error = 'Error connecting to network';
          } else {
            this.error = 'Please configure your wallet';
            throw error;
          }
        })
        .then(() => {
          this.walletAddress = window.localWeb3 && window.localWeb3.eth.defaultAccount && window.localWeb3.eth.defaultAccount.toLowerCase();
          this.originalWalletAddress = window.walletSettings.userPreferences.walletAddress;
          this.networkId = window.walletSettings.currentNetworkId;
          this.sameConfiguredNetwork = String(this.networkId) === String(window.walletSettings.defaultNetworkId);
          if(!this.sameConfiguredNetwork) {
            this.selectedTab = 'network';
          }
          if (this.walletAddress) {
            return computeBalance(this.walletAddress);
          }
        })
        .then((balanceDetails) => {
          if (balanceDetails) {
            this.walletAddressEtherBalance = balanceDetails.balance;
            this.walletAddressFiatBalance = balanceDetails.balanceFiat;
          }
        })
        .then(() => this.$refs.walletSetup && this.$refs.walletSetup.init())
        .then(() => this.$refs.generalTab && this.$refs.generalTab.init())
        .then(() => this.$refs.fundsTab && this.$refs.fundsTab.init())
        .then(() => this.$refs.networkTab && this.$refs.networkTab.init())
        .then(() => {
          this.fiatSymbol = (window.walletSettings && window.walletSettings.fiatSymbol) || '$';
          this.loadingSettings = false;
        })
        .then(() => this.$refs.contractsTab && this.$refs.contractsTab.init())
        .then(() => this.$refs && this.$refs.generalTab && this.$refs.generalTab.setSelectedValues())
        .then(() => this.$refs && this.$refs.walletsTab && this.$refs.walletsTab.init(true))
        .then(() => {
          if(!this.isAdmin) {
            // Load wallets for contract manager (rewarding group)
            return getWallets().then(wallets => this.wallets = wallets);
          }
        })
        .catch((error) => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug(error);
            if (!this.error) {
              this.error = String(error);
            }
          } else {
            this.error = 'Please configure your wallet';
          }
        })
        .catch((e) => {
          console.debug('init method - error', e);
          this.error = String(e);
        })
        .finally(() => {
          this.loadingContracts = this.loadingSettings = this.loadingWallets = this.loading = false;
          this.forceUpdate();
        });
    },
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === recipient);
      if (wallet) {
        if (transaction.contractAddress) {
          if(!transaction.contractMethodName || transaction.contractMethodName === 'transfer'  || transaction.contractMethodName === 'transferFrom' || transaction.contractMethodName === 'approve') {
            this.$set(wallet, 'loadingBalancePrincipal', true);
          }
          this.watchPendingTransaction(transaction, this.principalContract);
        } else {
          this.$set(wallet, 'loadingBalance', true);
          this.watchPendingTransaction(transaction);
        }
      } else {
        const contract = this.contracts.find((contract) => contract && contract.address && contract.address.toLowerCase() === recipient.toLowerCase());
        if (contract) {
          this.$set(contract, 'loadingBalance', true);
          if (this.$refs.contractDetail) {
            this.$refs.contractDetail.newTransactionPending(transaction, contract);
          }
        }
      }
      if (this.$refs.walletSummary) {
        this.$refs.walletSummary.loadPendingTransactions();
      }
    },
    watchPendingTransaction(transaction, contractDetails) {
      if (this.$refs.walletSummary) {
        this.$refs.walletSummary.loadPendingTransactions();
      }
      const thiss = this;
      watchTransactionStatus(transaction.hash, (receipt) => {
        if (receipt && receipt.status) {
          this.refreshCurrentWalletBalances(contractDetails);
          const wallet = thiss.wallets && thiss.wallets.find((wallet) => wallet && wallet.address && transaction.to && wallet.address.toLowerCase() === transaction.to.toLowerCase());
          if (transaction.contractMethodName === 'transferOwnership') {
            if (contractDetails && contractDetails.isContract && contractDetails.address && transaction.contractAddress && contractDetails.address.toLowerCase() === transaction.contractAddress.toLowerCase()) {
              this.$set(contractDetails, 'owner', transaction.to);
              contractDetails.networkId = this.networkId;
              return saveContractAddressOnServer(contractDetails).then(() => this.init());
            }
          } else if (transaction.contractMethodName === 'addAdmin' || transaction.contractMethodName === 'removeAdmin') {
            if (wallet) {
              contractDetails.contract.methods
                .getAdminLevel(wallet.address)
                .call()
                .then((level) => {
                  if (!wallet.accountAdminLevel) {
                    wallet.accountAdminLevel = {};
                  }
                  level = Number(level);
                  thiss.$set(wallet.accountAdminLevel, contractDetails.address, level ? level : 'not admin');
                  thiss.$nextTick(() => thiss.forceUpdate());
                });
            }
          } else if (transaction.contractMethodName === 'unPause' || transaction.contractMethodName === 'pause') {
            thiss.$set(contractDetails, 'isPaused', transaction.contractMethodName === 'pause' ? true : false);
            thiss.$nextTick(thiss.forceUpdate);
          }
          // Wait for Block synchronization with Metamask
          setTimeout(() => {
            if(wallet && thiss.$refs.walletsTab) {
              thiss.$refs.walletsTab.refreshWallet(wallet);
            }
          }, 2000);
        }
      });
    },
    refreshCurrentWalletBalances(contractDetails) {
      if (!this.walletAddress) {
        return Promise.resolve(null);
      }
      const wallet = this.wallets.find((wallet) => wallet.address && wallet.address.toLowerCase() === this.walletAddress.toLowerCase());
      return this.$refs.walletsTab.refreshWallet(wallet)
        .then(() => {
          this.walletAddressEtherBalance = wallet.balance;
          this.walletAddressFiatBalance = wallet.balanceFiat;
          this.forceUpdate();
          if (this.$refs.walletSummary) {
            this.$refs.walletSummary.$forceUpdate();
          }
          if (contractDetails && contractDetails.address && contractDetails.isContract) {
            if (this.principalContract && this.principalContract.address && this.principalContract.address.toLowerCase() === contractDetails.address.toLowerCase()) {
              this.$set(contractDetails, 'balance', wallet.balancePrincipal);
            } else {
              return contractDetails.contract.methods
                .balanceOf(this.walletAddress)
                .call()
                .then((balance, error) => {
                  if (error) {
                    throw new Error('Invalid contract address');
                  }
                  balance = String(balance);
                  balance = convertTokenAmountReceived(balance, contractDetails.decimals);
                  this.$set(contractDetails, 'balance', balance);
                  if (wallet) {
                    this.$set(wallet, 'balancePrincipal', balance);
                  }
                  this.forceUpdate();
                  if (this.$refs.walletSummary) {
                    this.$refs.walletSummary.$forceUpdate();
                  }
                })
            }
          }
        })
        .catch((error) => {
          this.error = String(error);
        });
    },
    saveGlobalSettings(globalSettings) {
      this.loading = true;
      this.loadingSettings = true;
      const defaultInitialFundsMap = {};
      if (!globalSettings.initialFunds) {
        if (window.walletSettings.initialFunds && window.walletSettings.initialFunds.length) {
          window.walletSettings.initialFunds.forEach((initialFund) => {
            defaultInitialFundsMap[initialFund.address] = initialFund.amount;
          });
        }
      }
      let reloadContract = false;
      if (globalSettings.defaultNetworkId) {
        reloadContract = String(window.walletSettings.defaultNetworkId) !== String(globalSettings.defaultNetworkId);
      }
      const currentGlobalSettings = Object.assign({}, window.walletSettings, {initialFunds: defaultInitialFundsMap});
      const globalSettingsToSave = Object.assign(currentGlobalSettings, globalSettings);
      if(globalSettingsToSave.contractAbi) {
        delete globalSettingsToSave.contractAbi;
      }
      if(globalSettingsToSave.contractBin) {
        delete globalSettingsToSave.contractBin;
      }
      if(globalSettingsToSave.contractBin) {
        delete globalSettingsToSave.contractBin;
      }
      if(globalSettingsToSave.userPreferences) {
        delete globalSettingsToSave.userPreferences;
      }
      delete globalSettingsToSave.walletEnabled;
      delete globalSettingsToSave.admin;
      return fetch('/portal/rest/wallet/api/global-settings/save', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(globalSettingsToSave),
      })
        .then((resp) => {
          if (resp && resp.ok) {
            return resp.text();
          } else {
            throw new Error('Error saving global settings');
          }
        })
        .then(() => {
          window.setTimeout(() => {
            this.init(!reloadContract);
          }, 200);
        })
        .catch((e) => {
          this.loading = false;
          console.debug('fetch global-settings - error', e);
          this.error = 'Error saving global settings';
        });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
  },
};
</script>
