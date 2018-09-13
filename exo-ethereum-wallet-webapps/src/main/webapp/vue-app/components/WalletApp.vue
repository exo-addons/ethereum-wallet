<template>
  <v-app id="WalletApp" :class="isMaximized ? 'maximized': 'minimized'" color="transaprent" flat>
    <main v-if="isWalletEnabled">
      <v-layout>
        <v-flex>
          <v-toolbar :color="isMaximized ? 'grey lighten-2':'transparent'" :class="isMaximized ? '':'no-padding'" flat dense>
            <v-toolbar-title v-if="isSpace && isMaximized">Space Wallet</v-toolbar-title>
            <v-toolbar-title v-else-if="isMaximized">My Wallet</v-toolbar-title>
            <h4 v-else class="head-container">Wallet</h4>
            <v-spacer />

            <wallet-app-menu v-if="!hasError && !loading && walletAddress"
                             :is-space="isSpace"
                             :is-maximized="isMaximized"
                             @refresh="init()"
                             @maximize="maximize()"
                             @modify-settings="showSettingsModal = true" />

            <user-settings-modal :account="walletAddress"
                                 :open="showSettingsModal"
                                 :fiat-symbol="fiatSymbol"
                                 :display-reset-option="displayWalletResetOption"
                                 @copied="browserWalletBackedUp = true"
                                 @close="showSettingsModal = false"
                                 @settings-changed="init()" />

          </v-toolbar>

          <v-toolbar v-if="displayWalletBackup" class="additionalToolbar" color="transparent" flat dense>
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i>
              Your wallet is not yet backed up yet.
              <wallet-backup-modal :display-complete-message="true" @copied="browserWalletBackedUp = true" />
              <a href="javascript:void(0);" @click="hideBackupMessage">Don't ask me again</a>
            </div>
          </v-toolbar>

          <v-toolbar v-if="displayWalletCreationToolbar" class="additionalToolbar" color="transparent" flat dense>
            <div class="alert alert-info">
              <i class="uiIconInfo"></i>
              No private key was found in current browser. Your wallet is displayed in readonly mode.
              <a v-if="!displayWalletSetup" href="javascript:void(0);" @click="openWalletSetup">More options</a>
            </div>
          </v-toolbar>
          <v-toolbar v-else-if="displayWalletUnlockToolbar" class="additionalToolbar" color="transparent" flat dense>
            <div class="alert alert-info">
              <i class="uiIconInfo"></i>
              Your wallet is locked in current browser, thus you can't send transactions.
              <wallet-unlock-modal @refresh="init()"/>
            </div>
          </v-toolbar>

          <wallet-app-setup v-if="displayWalletSetup"
                            v-show="!loading"
                            ref="walletAppSetup"
                            :error-code="errorCode"
                            :is-space="isSpace"
                            :use-metamask="useMetamask"
                            @configured="init" />

          <div v-else-if="displayWalletNotExistingYet" class="alert alert-info">
            <i class="uiIconInfo"></i>
            Space administrator hasn't set a Wallet for this space yet
          </div>

          <!-- Body -->
          <v-card v-if="displayAccountsList" class="text-xs-center" flat>
            <div v-if="errorMessage && !loading" class="alert alert-error">
              <i class="uiIconError"></i>
              {{ errorMessage }}
            </div>

            <v-progress-circular v-if="loading" color="primary" indeterminate></v-progress-circular>

            <wallet-metamask-setup
              v-if="useMetamask"
              v-show="!loading"
              ref="walletMetamaskSetup"
              :wallet-address="walletAddress"
              :is-space="isSpace"
              @loading="loading = true"
              @end-loading="loading = false"
              @refresh="init()"
              @error="loading = false; errorMessage = $event" />

            <wallet-summary
              v-if="!loading && walletAddress"
              :wallet-address="walletAddress"
              :ether-balance="etherBalance"
              :total-balance="totalBalance"
              :total-fiat-balance="totalFiatBalance"
              :is-read-only="isReadOnly"
              :fiat-symbol="fiatSymbol" />

            <wallet-accounts-list
              v-if="isMaximized && walletAddressConfigured"
              ref="WalletAccountsList"
              :is-read-only="isReadOnly"
              :accounts-details="accountsDetails"
              :account="walletAddress"
              :network-id="networkId"
              :refresh-index="refreshIndex"
              :fiat-symbol="fiatSymbol"
              @account-details-selected="openAccountDetail"
              @error="errorMessage = $event" />

          </v-card>

          <!-- The selected account detail -->
          <v-navigation-drawer v-if="!isMaximized" id="accountDetailsDrawer" v-model="seeAccountDetails" :permanent="seeAccountDetailsPermanent" fixed temporary right width="700" max-width="100vw">
            <account-detail ref="accountDetail" :fiat-symbol="fiatSymbol" :is-read-only="isReadOnly" :network-id="networkId" :account="walletAddress" :contract-detail="selectedAccount" @back="back"/>
          </v-navigation-drawer>
          <account-detail v-else-if="selectedAccount" ref="accountDetail" :fiat-symbol="fiatSymbol" :network-id="networkId" :is-read-only="isReadOnly" :account="walletAddress" :contract-detail="selectedAccount" @back="back"/>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import WalletAppMenu from './WalletAppMenu.vue';
import WalletAppSetup from './WalletAppSetup.vue';
import WalletMetamaskSetup from './WalletMetamaskSetup.vue';
import WalletSummary from './WalletSummary.vue';
import WalletAccountsList from './WalletAccountsList.vue';
import AccountDetail from './AccountDetail.vue';
import UserSettingsModal from './UserSettingsModal.vue';
import WalletUnlockModal from './WalletUnlockModal.vue';
import WalletBackupModal from './WalletBackupModal.vue';
import AddContractModal from './AddContractModal.vue';

import * as constants from '../WalletConstants.js';
import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {initWeb3, initSettings, computeBalance, setWalletBackedUp, etherToFiat} from '../WalletUtils.js';

export default {
  components: {
    WalletAppMenu,
    WalletAppSetup,
    WalletMetamaskSetup,
    WalletSummary,
    WalletAccountsList,
    AccountDetail,
    UserSettingsModal,
    WalletUnlockModal,
    WalletBackupModal,
    AddContractModal
  },
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      isWalletEnabled: false,
      loading: true,
      useMetamask: false,
      isReadOnly: true,
      isSpaceAdministrator: false,
      walletAddressConfigured: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      showSettingsModal: false,
      showAddContractModal: false,
      displayWalletSetup: false,
      displayWalletNotExistingYet: false,
      networkId: null,
      browserWalletExists: false,
      browserWalletDecrypted: false,
      browserWalletBackedUp: true,
      walletAddress: null,
      selectedAccount: null,
      fiatSymbol: '$',
      contracts: [],
      accountsDetails: {},
      refreshIndex: 1,
      watchMetamaskAccountInterval: null,
      errorCode: null,
      errorMessage: null
    };
  },
  computed: {
    displayAccountsList() {
      return !this.isMaximized || !this.selectedAccount;
    },
    displayWalletCreationToolbar() {
      return !this.loading && this.walletAddress && this.isReadOnly && !this.useMetamask && (!this.isSpace || this.isSpaceAdministrator) && !this.browserWalletExists;
    },
    displayWalletUnlockToolbar() {
      return !this.loading && this.walletAddress && !this.useMetamask && !this.browserWalletDecrypted && this.browserWalletExists;
    },
    displayWalletResetOption() {
      return !this.loading && this.walletAddress && !this.useMetamask && this.browserWalletExists;
    },
    displayWalletBackup() {
      return !this.loading && this.walletAddress && !this.useMetamask && !this.browserWalletBackedUp && this.browserWalletExists;
    },
    etherBalance() {
      if (this.refreshIndex > 0 && this.walletAddress && this.accountsDetails && this.accountsDetails[this.walletAddress]) {
        return this.accountsDetails[this.walletAddress].balance;
      }
      return 0;
    },
    totalFiatBalance() {
      return etherToFiat(this.totalBalance);
    },
    totalBalance() {
      let balance = 0;
      if (this.refreshIndex > 0 && this.walletAddress && this.accountsDetails) {
        Object.keys(this.accountsDetails).forEach(key => {
          const accountDetail = this.accountsDetails[key];
          balance += Number((accountDetail.isContract ? accountDetail.balanceInEther : accountDetail.balance) || 0);
        });
      }
      return balance;
    }
  },
  watch: {
    seeAccountDetails() {
      if (this.seeAccountDetails) {
        const thiss = this;
        setTimeout(() => {
          thiss.seeAccountDetailsPermanent = true;
        }, 200);
      } else {
        this.seeAccountDetailsPermanent = false;
      }
    }
  },
  created() {
    if (!eXo && eXo.env || !eXo.env.portal || !eXo.env.portal.userName || !eXo.env.portal.userName.length) {
      this.isWalletEnabled = false;
      return;
    }

    if (eXo.env.portal.profileOwner && eXo.env.portal.profileOwner !== eXo.env.portal.userName) {
      this.isWalletEnabled = false;
      return;
    }

    if (this.isSpace && !(eXo && eXo.env && eXo.env.portal && eXo.env.portal.spaceGroup && eXo.env.portal.spaceGroup.length)) {
      this.isWalletEnabled = false;
      return;
    }

    // Init application
    this.init()
      .then((result, error) => {
        const thiss = this;
        // Refresh application when Metamask address changes
        this.watchMetamaskAccount();
        window.addEventListener('load', function() {
          if (!thiss.loading && thiss.useMetamask && window && window.web3 && window.web3.eth && window.web3.eth.defaultAccount
              && window.web3.eth.defaultAccount.toLowerCase() !== this.walletAddress) {
            thiss.init();
          }
        });
      });
  },
  methods: {
    init() {
      this.loading = true;
      this.errorMessage = null;
      this.seeAccountDetails = false;
      this.errorMessage = null;
      this.selectedAccount = null;
      this.accountsDetails = {};
      this.walletAddress = null;
      this.displayWalletNotExistingYet = false;

      return initSettings(this.isSpace)
        .then((result, error) => {
          if (error) {
            throw error;
          }

          if (!window.walletSettings || !window.walletSettings.isWalletEnabled) {
            this.isWalletEnabled = false;
            this.forceUpdate();
            throw new Error("Wallet disabled for current user");
          } else {
            this.isWalletEnabled = true;
            this.useMetamask = window.walletSettings.userPreferences.useMetamask;
            this.isSpaceAdministrator = window.walletSettings.isSpaceAdministrator;
            if (window.walletSettings.userPreferences.walletAddress) {
              this.initMenuApp();
              this.forceUpdate();
            } else {
              throw new Error(constants.ERROR_WALLET_NOT_CONFIGURED);
            }
          }
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          return initWeb3(this.isSpace);
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }

          this.networkId = window.walletSettings.currentNetworkId;

          if (this.useMetamask && window && window.web3 && window.web3.eth && window.web3.eth.defaultAccount) {
            this.walletAddress = window.web3.eth.defaultAccount.toLowerCase();
          } else if (window.localWeb3.eth.defaultAccount) {
            this.walletAddress = window.localWeb3.eth.defaultAccount.toLowerCase();
          }

          this.isReadOnly = window.walletSettings.isReadOnly;
          this.browserWalletExists = window.walletSettings.browserWalletExists;
          this.browserWalletDecrypted = window.walletSettings.browserWallet;
          this.browserWalletBackedUp = window.walletSettings.userPreferences.backedUp;

          this.walletAddressConfigured = true;
          this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$';

          return this.refreshBalance();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          return this.reloadContracts();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          if (this.useMetamask) {
            return this.$refs.walletMetamaskSetup.init();
          }
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          this.forceUpdate();
          this.loading = false;
        })
        .catch(e => {
          console.debug("init method - error", e);
          const error = `${e}`;

          if (error.indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            this.errorCode = constants.ERROR_WALLET_NOT_CONFIGURED;
            if (!this.isSpace || this.isSpaceAdministrator) {
              this.openWalletSetup();
            } else {
              this.displayWalletNotExistingYet = true;
            }
          } else if (error.indexOf(constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.errorMessage = 'Failed to load user settings';
          } else if (error.indexOf(constants.ERROR_WALLET_DISCONNECTED) >= 0) {
            this.errorMessage = 'Failed to connect to network';
          } else {
            this.walletAddressConfigured = true;
            this.errorMessage = error;
          }
          this.loading = false;
        });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshBalance() {
      return computeBalance(this.walletAddress)
        .then((balanceDetails, error) => {
          if (error) {
            this.accountsDetails[this.walletAddress] = {
              title : 'ether',
              icon : 'warning',
              balance : 0,
              symbol : 'ether',
              isContract : false,
              address : this.walletAddress,
              error : `Error retrieving balance of wallet: ${error}`
            };
            throw error;
          }
          const accountDetails = {
            title : 'ether',
            icon : 'fab fa-ethereum',
            symbol : 'ether',
            isContract : false,
            address : this.walletAddress,
            balance : balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0,
            balanceFiat : balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0
          };
          return this.accountsDetails[this.walletAddress] = accountDetails;
        })
        .catch(e => {
          console.debug("refreshBalance method - error", e);
          this.accountsDetails[this.walletAddress] = {
            title : 'ether',
            icon : 'warning',
            balance : 0,
            symbol : 'ether',
            isContract : false,
            address : this.walletAddress,
            error : `Error retrieving balance of wallet ${e}`,
          };
          throw e;
        });
    },
    reloadContracts() {
      this.showAddContractModal = false;
      return getContractsDetails(this.walletAddress, this.networkId)
        .then((contractsDetails, error) => {
          if (error) {
            throw error;
          }
          if (contractsDetails && contractsDetails.length) {
            contractsDetails.forEach(contractDetails => {
              if (contractDetails && contractDetails.address) {
                if (this.accountsDetails[this.walletAddress]) {
                  contractDetails.etherBalance = this.accountsDetails[this.walletAddress].balance;
                }
                this.accountsDetails[contractDetails.address] = contractDetails;
              }
            });
            this.forceUpdate();
          }
        });
    },
    openAccountDetail(accountDetails) {
      if (!accountDetails.error) {
        this.selectedAccount = accountDetails;
      }
      if (!this.isMaximized) {
        this.seeAccountDetails = true;
      }
    },
    deleteContract(item, event) {
      if(deleteContractFromStorage(this.walletAddress, this.networkId, item.address)) {
        delete this.accountsDetails[item.address];
        this.forceUpdate();
      }
      event.preventDefault();
      event.stopPropagation();
    },
    back() {
      this.seeAccountDetails = false;
      this.selectedAccount = null;
    },
    maximize() {
      window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/wallet`; 
    },
    initMenuApp() {
      if (!this.isWalletEnabled || this.isSpace) {
        return;
      }
      this.$nextTick(() => {
        if ($("#myWalletTad").length) {
          return;
        }
        $(".userNavigation").append(` \
          <li id='myWalletTad' class='item${this.isMaximized ? " active" : ""}'> \
            <a href='/portal/intranet/wallet'> \
              <div class='uiIconAppWallet uiIconDefaultApp' /> \
              <span class='tabName'>My Wallet</span> \
            </a> \
          </li>`);
        $(window).trigger("resize");
      });
    },
    hideBackupMessage() {
      setWalletBackedUp(null, true);
      this.browserWalletBackedUp = true;
    },
    openWalletSetup() {
      this.displayWalletSetup = true;
    },
    watchMetamaskAccount() {
      const thiss = this;

      if (this.watchMetamaskAccountInterval) {
        clearInterval(this.watchMetamaskAccountInterval);
      }
      // In case account switched in Metamask
      // See https://github.com/MetaMask/faq/blob/master/DEVELOPERS.md
      this.watchMetamaskAccountInterval = setInterval(function() {
        if (!thiss.useMetamask || !window || !window.web3 || !window.web3.eth.defaultAccount || !thiss.walletAddress) {
          return;
        }

        if (window.web3.eth.defaultAccount.toLowerCase() !== thiss.walletAddress.toLowerCase()) {
          thiss.init();
          return;
        }
      }, 1000);
    }
  }
};
</script>