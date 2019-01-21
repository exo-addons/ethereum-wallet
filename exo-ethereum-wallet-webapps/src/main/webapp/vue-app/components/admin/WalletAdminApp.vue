<template>
  <v-app
    id="WalletAdminApp"
    color="transaprent"
    flat>
    <main>
      <v-layout>
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
            is-maximized
            hide-actions
            @refresh-balance="refreshBalance"
            @error="error = $event" />

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
            <v-tab key="general">
              Settings
            </v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="funds">
              Initial accounts funds
            </v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="overview">
              Advanced settings
            </v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="contracts">
              Contracts
            </v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="wallets">
              Wallets
            </v-tab>
            <v-tab
              v-if="sameConfiguredNetwork"
              key="kudosList"
              :class="kudosListRetrieved || 'hidden'">
              Kudos
            </v-tab>
            <v-tab
              v-if="sameConfiguredNetwork"
              key="gamificationTab"
              :class="gamificationRetrieved || 'hidden'">
              Gamification
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab">
            <v-tab-item id="general">
              <general-tab
                ref="generalTab"
                :network-id="networkId"
                :default-network-id="defaultNetworkId"
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
            <v-tab-item v-if="sameConfiguredNetwork" id="funds">
              <initial-funds-tab
                ref="fundsTab"
                :default-network-id="defaultNetworkId"
                :loading="loading"
                :contracts="contracts"
                :same-configured-network="sameConfiguredNetwork"
                @initial-funds-loaded="initialFunds = $event"
                @save="saveGlobalSettings" />
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" id="overview">
              <advanced-settings-tab
                ref="advancedTab"
                :default-network-id="defaultNetworkId"
                :loading="loading"
                :contracts="contracts"
                :principal-contract="principalContract"
                :fiat-symbol="fiatSymbol"
                :same-configured-network="sameConfiguredNetwork"
                @save="saveGlobalSettings" />
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" id="contracts">
              <contracts-tab
                ref="contractsTab"
                :network-id="networkId"
                :wallet-address="walletAddress"
                :loading="loading"
                :loading-wallets="loadingWallets"
                :fiat-symbol="fiatSymbol"
                :token-etherscan-link="tokenEtherscanLink"
                :wallets="wallets"
                @contract-list-modified="$refs.walletsTab && $refs.walletsTab.init()"
                @pending-transaction="watchPendingTransaction"
                @contracts-loaded="contracts = $event" />
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" id="wallets">
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
                @pending="pendingTransaction"
                @wallets-loaded="wallets = $event"
                @loading-wallets-changed="loadingWallets = $event"
                @refresh-balance="refreshBalance" />
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" id="kudosList">
              <kudos-tab
                v-if="!loading"
                ref="kudosList"
                :wallets="wallets"
                :wallet-address="walletAddress"
                :contracts="contracts"
                :principal-account="principalAccountAddress"
                @pending="pendingTransaction"
                @open-wallet-transaction="openWalletTransaction"
                @list-retrieved="kudosListRetrieved = true" />
            </v-tab-item>
            <v-tab-item v-if="sameConfiguredNetwork" id="gamificationTab">
              <gamification-tab
                v-if="!loading"
                ref="kudosList"
                :wallets="wallets"
                :wallet-address="walletAddress"
                :contracts="contracts"
                :principal-account="principalAccountAddress"
                @pending="pendingTransaction"
                @open-wallet-transaction="openWalletTransaction"
                @list-retrieved="gamificationRetrieved = true" />
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
import AdvancedSettingsTab from './WalletAdminSettingsAdvancedTab.vue';
import ContractsTab from './WalletAdminContractsTab.vue';
import WalletsTab from './WalletAdminWalletsTab.vue';
import KudosTab from './WalletAdminKudosTab.vue';
import GamificationTab from './WalletAdminGamificationTab.vue';

import WalletSetup from '../WalletSetup.vue';
import WalletSummary from '../WalletSummary.vue';

import * as constants from '../../WalletConstants.js';
import {saveContractAddressOnServer} from '../../WalletToken.js';
import {initWeb3, initSettings, watchTransactionStatus, computeBalance, convertTokenAmountReceived, getTokenEtherscanlink, getAddressEtherscanlink} from '../../WalletUtils.js';

export default {
  components: {
    GeneralTab,
    InitialFundsTab,
    AdvancedSettingsTab,
    ContractsTab,
    WalletsTab,
    KudosTab,
    GamificationTab,
    WalletSummary,
    WalletSetup,
  },
  data() {
    return {
      loading: false,
      loadingSettings: false,
      loadingContracts: false,
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
      kudosListRetrieved: false,
      gamificationRetrieved: false,
      initialFunds: [],
      contracts: [],
      wallets: [],
    };
  },
  watch: {
    kudosListRetrieved() {
      if (this.kudosListRetrieved) {
        window.dispatchEvent(new Event('resize'));
      }
    },
    gamificationRetrieved() {
      if (this.gamificationRetrieved) {
        window.dispatchEvent(new Event('resize'));
      }
    },
    selectedTab() {
      this.$nextTick(() => {
        window.dispatchEvent(new Event('resize'));
      });
    },
    loading() {
      if (!this.loading) {
        this.$nextTick(() => {
          window.dispatchEvent(new Event('resize'));
        });
      }
    },
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
        .then(() => this.$refs.advancedTab && this.$refs.advancedTab.init())
        .then(() => {
          this.sameConfiguredNetwork = String(this.networkId) === String(window.walletSettings.defaultNetworkId);
          this.fiatSymbol = (window.walletSettings && window.walletSettings.fiatSymbol) || '$';
          this.loadingSettings = false;
        })
        .then(() => this.$refs.contractsTab && this.$refs.contractsTab.init())
        .then(() => this.$refs && this.$refs.generalTab && this.$refs.generalTab.setSelectedValues())
        .then(() => this.$refs && this.$refs.walletsTab && this.$refs.walletsTab.init())
        .catch((error) => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error retrieving contracts', error);
            if (!this.error) {
              this.error = 'Error retrieving contracts';
            }
          } else {
            this.error = 'Please configure your wallet';
          }
        })
        .catch((e) => {
          console.debug('init method - error', e);
          this.error = `Error encountered: ${e}`;
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
          this.$set(wallet, 'loadingBalancePrincipal', true);
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
            transaction.addLoadingToRecipient = true;
            this.$refs.contractDetail.newTransactionPending(transaction, contract);
          }
        }
      }
      if (this.$refs.walletSummary) {
        this.$refs.walletSummary.loadPendingTransactions();
      }
    },
    refreshBalance(accountDetails, address, error) {
      if (this.walletAddress) {
        computeBalance(this.walletAddress).then((balanceDetails) => {
          if (balanceDetails) {
            this.walletAddressEtherBalance = balanceDetails.balance;
            this.walletAddressFiatBalance = balanceDetails.balanceFiat;
          }
        });
      }
      if (error) {
        console.debug('Error while proceeding transaction', error);
        this.contracts.forEach((contract) => {
          if (contract.loadingBalance) {
            this.$set(contract, 'loadingBalance', false);
          }
        });
        this.wallets.forEach((wallet) => {
          if (wallet.loadingBalance || wallet.loadingBalancePrincipal) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', `Error proceeding transaction: ${error}`);
            // Update wallet stateus: admin, approved ...
            if (this.$refs.contractDetail) {
              this.$refs.contractDetail.retrieveAccountDetails(wallet);
            }
          }
        });
        return;
      }
      const contract = this.contracts.find((contract) => contract && address && contract.address && contract.address.toLowerCase() === address.toLowerCase());
      if (contract) {
        computeBalance(address)
          .then((balanceDetails, error) => {
            if (!error) {
              this.$set(contract, 'contractBalance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            }
            this.forceUpdate();
          })
          .catch((error) => {
            this.error = String(error);
          })
          .finally(() => {
            this.$set(contract, 'loadingBalance', false);
          });
      } else if (this.$refs.walletsTab) {
        const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === address);
        const currentUserWallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address === this.walletAddress);
        if (currentUserWallet) {
          this.$refs.walletsTab.computeBalance(accountDetails, currentUserWallet);
        }
        if (wallet) {
          this.$refs.walletsTab.computeBalance(accountDetails, wallet);
        }
      }
    },
    openWalletTransaction(transactionDetails) {
      if (!this.$refs.walletsTab) {
        return;
      }
      const wallet = this.wallets.find((wallet) => wallet && wallet.address && wallet.address.toLowerCase() === transactionDetails.address.toLowerCase());
      if (wallet && this.$refs.walletsTab) {
        this.selectedTab = 4;
        this.$refs.walletsTab.openAccountDetail(wallet, transactionDetails.hash);
      }
    },
    watchPendingTransaction(transaction, contractDetails) {
      if (this.$refs.walletSummary) {
        this.$refs.walletSummary.loadPendingTransactions();
      }
      const thiss = this;
      watchTransactionStatus(transaction.hash, (receipt) => {
        if (receipt.status) {
          const wallet = thiss.wallets && thiss.wallets.find((wallet) => wallet && wallet.address && transaction.to && wallet.address.toLowerCase() === transaction.to.toLowerCase());
          if (transaction.contractMethodName === 'approveAccount' || transaction.contractMethodName === 'disapproveAccount') {
            if (wallet) {
              contractDetails.contract.methods
                .isApprovedAccount(wallet.address)
                .call()
                .then((approved) => {
                  if (!wallet.approved) {
                    wallet.approved = {};
                  }
                  thiss.$set(wallet.approved, contractDetails.address, approved ? 'approved' : 'disapproved');
                  thiss.$nextTick(() => thiss.forceUpdate());
                });
            }
          } else if (transaction.contractMethodName === 'transferOwnership') {
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
          } else if ((!transaction.contractMethodName || transaction.contractMethodName === 'transfer' || transaction.contractMethodName === 'transferFrom' || transaction.contractMethodName === 'approve') && transaction.to) {
            if (wallet) {
              const thiss = this;
              // Wait for Block synchronization with Metamask
              setTimeout(() => {
                thiss.refreshWalletBalance(wallet, transaction.contractMethodName ? thiss.principalContract : null, true);
              }, 2000);
            }
          } else if (contractDetails && transaction.to && transaction.to === contractDetails.address) {
            this.$set(contractDetails, 'loadingBalance', false);
          }
        }
      });
    },
    refreshWalletBalance(wallet, contractDetails, disableRefreshInProgress) {
      if (contractDetails && contractDetails.contract) {
        return contractDetails.contract.methods
          .balanceOf(wallet.address)
          .call()
          .then((balance, error) => {
            if (error) {
              throw new Error('Invalid contract address');
            }
            balance = String(balance);
            balance = convertTokenAmountReceived(balance, contractDetails.decimals);
            this.$set(wallet, 'balancePrincipal', balance);
            this.forceUpdate();
            return this.refreshCurrentWalletBalances(contractDetails);
          })
          .catch((error) => {
            this.error = String(error);
          })
          .finally(() => {
            this.$set(wallet, 'loadingBalancePrincipal', false);
          });
      } else {
        return computeBalance(wallet.address)
          .then((balanceDetails, error) => {
            if (error) {
              this.$set(wallet, 'icon', 'warning');
              this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
            } else {
              this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
              this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
            }
            this.forceUpdate();
            return this.refreshCurrentWalletBalances();
          })
          .catch((error) => {
            this.error = String(error);
          })
          .finally(() => {
            this.$set(wallet, 'loadingBalance', false);
          });
      }
    },
    refreshCurrentWalletBalances(contractDetails) {
      if (!this.walletAddress) {
        return Promise.resolve(null);
      }
      const wallet = this.wallets.find((wallet) => wallet.address && wallet.address.toLowerCase() === this.walletAddress.toLowerCase());
      return computeBalance(this.walletAddress).then((balanceDetails) => {
        if (balanceDetails) {
          this.walletAddressEtherBalance = balanceDetails.balance;
          this.walletAddressFiatBalance = balanceDetails.balanceFiat;
          if (wallet) {
            this.$set(wallet, 'balance', balanceDetails.balance);
            this.$set(wallet, 'balanceFiat', balanceDetails.balanceFiat);
          }
          this.forceUpdate();
          if (this.$refs.walletSummary) {
            this.$refs.walletSummary.$forceUpdate();
          }
        }
        if (contractDetails) {
          contractDetails.contract.methods
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
            .catch((error) => {
              this.error = String(error);
            });
        }
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
