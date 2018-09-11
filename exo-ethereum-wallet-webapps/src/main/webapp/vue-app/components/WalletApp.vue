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
                             :is-account-details="selectedAccount !== null"
                             @refresh="init()"
                             @modify-settings="showSettingsModal = true" />

            <v-btn v-if="!isMaximized" icon class="maximizeIcon" @click="maximize">
              <v-icon size="15px">fa-external-link-alt</v-icon>
            </v-btn>

            <user-settings-modal :account="walletAddress"
                                 :open="showSettingsModal"
                                 :fiat-symbol="fiatSymbol"
                                 @close="showSettingsModal = false"
                                 @settings-changed="init()" />
          </v-toolbar>

          <v-toolbar v-if="!loading && walletAddress && isReadOnly && !useMetamask" id="readOnlyToolbar" color="transparent" flat dense>
            <div class="alert alert-info">
              <i class="uiIconInfo"></i>
              No private key was found on current browser. Your wallet is displayed in readonly mode.
              <a v-if="!displayWalletSetup" href="#" @click="openWalletSetup">More options</a>
            </div>
          </v-toolbar>

          <wallet-app-setup
            v-if="displayWalletSetup"
            v-show="!loading"
            ref="walletAppSetup"
            :error-code="errorCode"
            :is-space="isSpace"
            :is-read-only="isReadOnly"
            :use-metamask="useMetamask"
            @configured="init" />

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
              :is-space="isSpace"
              :is-read-only="isReadOnly"
              :wallet-address="walletAddress"
              @loading="loading = true"
              @end-loading="loading = false"
              @refresh="init()"
              @error="loading = false; errorMessage = $event" />

            <wallet-accounts-list
              v-if="walletAddressConfigured"
              ref="WalletAccountsList"
              :accounts-details="accountsDetails"
              :account="walletAddress"
              :network-id="networkId"
              :is-read-only="isReadOnly"
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
import WalletAccountsList from './WalletAccountsList.vue';
import AddContractModal from './AddContractModal.vue';
import AccountDetail from './AccountDetail.vue';
import QrCodeModal from './QRCodeModal.vue';
import UserSettingsModal from './UserSettingsModal.vue';

import * as constants from '../WalletConstants.js';
import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {searchAddress, searchUserOrSpaceObject, searchFullName} from '../WalletAddressRegistry.js';
import {etherToFiat, initWeb3, initSettings, computeBalance} from '../WalletUtils.js';

export default {
  components: {
    UserSettingsModal,
    WalletAppMenu,
    WalletAppSetup,
    WalletMetamaskSetup,
    WalletAccountsList,
    QrCodeModal,
    AccountDetail,
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
      walletAddressConfigured: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      showSettingsModal: false,
      showAddContractModal: false,
      displayWalletSetup: false,
      networkId: null,
      walletAddress: null,
      selectedAccount: null,
      fiatSymbol: '$',
      contracts: [],
      accountsDetails: {},
      refreshIndex: 1,
      errorCode: null,
      errorMessage: null
    };
  },
  computed: {
    displayAccountsList() {
      return !this.isMaximized || !this.selectedAccount;
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
            this.walletAddress = window.web3.eth.defaultAccount;
          } else {
            this.walletAddress = window.localWeb3.eth.defaultAccount;
          }

          this.isReadOnly = window.walletSettings.isReadOnly;

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
            this.openWalletSetup();
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
    openWalletSetup() {
      this.displayWalletSetup = true;
    }
  }
};
</script>