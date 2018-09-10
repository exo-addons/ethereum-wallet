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

            <wallet-app-menu v-if="!hasError && !loading && account"
                             :is-space="isSpace"
                             :is-account-details="selectedAccount !== null"
                             @refresh="refreshList(true)"
                             @modify-settings="showSettingsModal = true" />

            <v-btn v-if="!isMaximized" icon class="maximizeIcon" @click="maximize">
              <v-icon size="15px">fa-external-link-alt</v-icon>
            </v-btn>

            <user-settings-modal :account="account"
                                 :open="showSettingsModal"
                                 :fiat-symbol="fiatSymbol"
                                 @close="showSettingsModal = false"
                                 @settings-changed="refreshList(true)" />
          </v-toolbar>

          <!-- Body -->
          <v-card v-if="displayAccountsList" class="text-xs-center" flat>
            <v-progress-circular v-show="loading" color="primary" indeterminate></v-progress-circular>

            <wallet-app-alerts v-if="!loading && walletAddressConfigured"
                               :display-not-same-network-warning="displayNotSameNetworkWarning"
                               :network-label="networkLabel"
                               :display-space-metamask-enable-help="displaySpaceMetamaskEnableHelp"
                               :new-address-detected="newAddressDetected"
                               :display-space-account-creation-help="displaySpaceAccountCreationHelp"
                               :current-account-already-in-use="currentAccountAlreadyInUse"
                               :display-space-account-association-help="displaySpaceAccountAssociationHelp"
                               :display-user-account-association-help="displayUserAccountAssociationHelp"
                               :display-user-account-change-help="displayUserAccountChangeHelp"
                               :display-account-help-actions="displayAccountHelpActions"
                               :display-errors="displayErrors"
                               :metamask-enabled="metamaskEnabled"
                               :metamask-connected="metamaskConnected"
                               :account="account"
                               :old-account-address="oldAccountAddress"
                               :new-account-address="newAccountAddress"
                               :error-message="errorMessage"
                               @save-address-to-account="saveNewAddressInWallet" />

            <wallet-app-setup
              v-else-if="!loading"
              :error-code="errorCode"
              :is-space="isSpace"
              @configured="init" />

            <wallet-accounts-list
              v-if="walletAddressConfigured"
              ref="WalletAccountsList"
              :accounts-details="accountsDetails"
              :account="account"
              :network-id="networkId"
              :refresh-index="refreshIndex"
              :fiat-symbol="fiatSymbol"
              @account-details-selected="openAccountDetail"
              @error="errorMessage = $event" />

          </v-card>

          <!-- The selected account detail -->
          <v-navigation-drawer v-if="!isMaximized" id="accountDetailsDrawer" v-model="seeAccountDetails" :permanent="seeAccountDetailsPermanent" fixed temporary right width="700" max-width="100vw">
            <account-detail ref="accountDetail" :fiat-symbol="fiatSymbol" :is-space="isSpace" :network-id="networkId" :account="account" :contract-detail="selectedAccount" @back="back"/>
          </v-navigation-drawer>
          <account-detail v-else-if="selectedAccount" ref="accountDetail" :fiat-symbol="fiatSymbol" :network-id="networkId" :is-space="isSpace" :account="account" :contract-detail="selectedAccount" @back="back"/>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import WalletAppMenu from './WalletAppMenu.vue';
import WalletAppAlerts from './WalletAppAlerts.vue';
import WalletAccountsList from './WalletAccountsList.vue';
import AddContractModal from './AddContractModal.vue';
import AccountDetail from './AccountDetail.vue';
import QrCodeModal from './QRCodeModal.vue';
import UserSettingsModal from './UserSettingsModal.vue';
import WalletAppSetup from './WalletAppSetup.vue';

import * as constants from '../WalletConstants.js';
import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {searchAddress, searchUserOrSpaceObject, searchFullName, saveNewAddress} from '../WalletAddressRegistry.js';
import {etherToFiat, initWeb3, initSettings, computeNetwork, computeBalance} from '../WalletUtils.js';

export default {
  components: {
    UserSettingsModal,
    WalletAppMenu,
    WalletAppAlerts,
    WalletAppSetup,
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
      walletConfigurationMenu: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      addressAssociationDialog: false,
      installInstructionDialog: false,
      metamaskWatchInterval: false,
      isWalletEnabled: false,
      networkLabel: '',
      loading: true,
      walletAddressConfigured: false,
      sameConfiguredNetwork: true,
      currentAccountAlreadyInUse: false,
      displaySpaceAccountCreationHelp: false,
      oldAccountAddressNotFound: null,
      oldAccountAddress: null,
      newAccountAddress: null,
      showSettingsModal: false,
      showAddContractModal: false,
      networkId: null,
      networkName: null,
      account: null,
      lastCheckedAccount: null,
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
    metamaskEnabled () {
      return window.web3 && window.web3.currentProvider && window.web3.currentProvider.isMetaMask;
    },
    metamaskConnected () {
      return this.metamaskEnabled && window.web3.currentProvider.isConnected();
    },
    displayAccountsList() {
      return !this.isMaximized || !this.selectedAccount;
    },
    displayErrors() {
      return this.hasErrors && !this.loading;
    },
    displayNotSameNetworkWarning() {
      return !this.loading && !this.isSpace && !this.sameConfiguredNetwork && this.networkLabel && this.networkLabel.length;
    },
    displayAccountHelpActions() {
      return !this.currentAccountAlreadyInUse && this.displayUserAccountChangeHelp || this.displayUserAccountAssociationHelp || this.displaySpaceAccountAssociationHelp;
    },
    displayUserAccountChangeHelp() {
      return !this.isSpace && this.oldAccountAddress && this.newAccountAddress;
    },
    displayUserAccountAssociationHelp() {
      return !this.isSpace && !this.oldAccountAddress && this.newAccountAddress;
    },
    displaySpaceAccountAssociationHelp() {
      return this.isSpace && !this.oldAccountAddress && this.newAccountAddress && this.newAccountAddress.length;
    },
    displaySpaceMetamaskEnableHelp() {
      return this.isSpace && !this.loading && !this.oldAccountAddress && !this.newAccountAddress && !this.account;
    },
    hasErrors() {
      return !this.loading && this.walletAddressConfigured && (this.errorMessage || (!this.isSpace && !this.account));
    },
    newAddressDetected() {
      return this.sameConfiguredNetwork
        && !this.loading
        && (this.displaySpaceAccountCreationHelp
            || (this.newAccountAddress && (!this.oldAccountAddress || this.oldAccountAddress !== this.newAccountAddress)));
    }
  },
  watch: {
    account(value, oldValue) {
      if (!this.account || !this.account.length) {
        return;
      }
      this.refreshBalance()
        .catch(e => {
          console.debug("refreshBalance method - error", e);
          this.errorMessage = `Error while retrieving user balance ${e}`;
        });
    },
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
    if(eXo.env.portal.profileOwner && eXo.env.portal.profileOwner !== eXo.env.portal.userName) {
      this.isWalletEnabled = false;
      return;
    }
    if(this.isSpace && !(eXo && eXo.env && eXo.env.portal && eXo.env.portal.spaceGroup && eXo.env.portal.spaceGroup.length)) {
      this.isWalletEnabled = false;
      return;
    }

    // Close wallet menu on click
    $(document).on("click", () => {
      this.walletConfigurationMenu = false;
    });

    // Init application
    this.loading = true;
    this.init()
      .then((result, error) => {
        if (error) {
          throw error;
        }
        const thiss = this;
        // Refresh application when Metamask address changes
        window.addEventListener('load', function() {
          if (window && window.localWeb3 && window.localWeb3.eth && window.localWeb3.eth.defaultAccount !== this.account) {
            thiss.init();
          }
        });
      });
  },
  methods: {
    init() {
      this.errorMessage = null;
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
            if (window.walletSettings.userPreferences.walletAddress) {
              this.initMenuApp();
              this.forceUpdate();
            } else {
              throw new Error(constants.ERROR_WALLET_ADDRESS_NOT_CONFIGURED);
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
          this.walletAddressConfigured = true;
          return this.refreshList(false);
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          return this.retrieveDefaultUserAccount();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }

          this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$';
          return this.watchMetamaskAccount();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }

        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          this.loading = false;
        })
        .catch(e => {
          console.debug("init method - error", e);
          const error = `${e}`;
          if (error.indexOf(constants.ERROR_METAMASK_NOT_CONNECTED) >= 0) {
            this.errorCode = constants.ERROR_METAMASK_NOT_CONNECTED;
            // TODO
          } else if (error.indexOf(constants.ERROR_WRONG_WALLET_SETTINGS) >= 0) {
            this.errorCode = constants.ERROR_WRONG_WALLET_SETTINGS;
            // TODO
          } else if (error.indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) >= 0) {
            this.errorCode = constants.ERROR_WALLET_NOT_CONFIGURED;
            // TODO
          } else if (error.indexOf(constants.ERROR_WALLET_SETTINGS_NOT_LOADED) >= 0) {
            this.errorCode = constants.ERROR_WALLET_SETTINGS_NOT_LOADED;
            // TODO
          } else if (error.indexOf(constants.ERROR_WALLET_DISCONNECTED) >= 0) {
            this.errorCode = constants.ERROR_WALLET_DISCONNECTED;
            // TODO
          } else if (error.indexOf(constants.ERROR_WALLET_CONFIGURED_ADDRESS_NOT_FOUND) >= 0) {
            this.errorCode = constants.ERROR_WALLET_CONFIGURED_ADDRESS_NOT_FOUND;
            // TODO
          } else if (error.indexOf(constants.ERROR_WALLET_ADDRESS_NOT_CONFIGURED) >= 0) {
            this.errorCode = constants.ERROR_WALLET_ADDRESS_NOT_CONFIGURED;
            // TODO
          } else {
            this.walletAddressConfigured = true;
          }
          this.errorMessage = `${e}`;
          this.loading = false;
        })
        .finally(() => {
          if (window.walletSettings && window.walletSettings.defaultNetworkId) {
            this.networkLabel = constants.NETWORK_NAMES[window.walletSettings.defaultNetworkId];
            if (!this.networkLabel) {
              this.networkLabel = `Custom network with id ${window.walletSettings.defaultNetworkId}`;
            }
          }
        });
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    },
    refreshList(forceRefresh) {
      this.seeAccountDetails = false;
      this.errorMessage = null;
      this.selectedAccount = null;

      forceRefresh = forceRefresh ? true : false;

      this.loading = true;
      this.accountsDetails = {};

      return Promise.resolve(forceRefresh)
        .then(forceRefresh => {
          if (forceRefresh) {
            return initSettings(this.isSpace);
          }
        })
        .then(() => this.getAccount(this.isSpace))
        .then((account, error) => {
          if (error) {
            throw error;
          }
          return this.initAccount(account, forceRefresh);
        })
        .then(() => {
          this.loading = false;
          this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$';
          $(window).trigger("resize");
        })
        .catch(e => {
          console.debug("refreshList method - error", e);
          this.errorMessage = `${e}`;
          this.loading = false;
        });
    },
    getAccount(isSpace) {
      if (window.walletSettings.userPreferences.useMetamask) {
        if(isSpace) {
          if (!eXo.env.portal.spaceGroup) {
            throw new Error("Space identifier is empty");
          }
          return searchAddress(eXo.env.portal.spaceGroup, 'space');
        } else {
          return window.localWeb3.eth.getCoinbase();
        }
      } else {
        return window.localWeb3.eth.defaultAccount;
      }
    },
    initAccount(account, forceRefresh) {
      if (this.isSpace) {
        if (!account || !account.length) {
          if (window.web3 && window.web3.eth.defaultAccount) {
            // Display information to allow administrator to associate
            // This new address with wallet
            return this.oldAccountAddress = this.newAccountAddress = window.web3.eth.defaultAccount;
          } else {
            this.displaySpaceAccountCreationHelp = true;
          }
        } else {
          account = account.toLowerCase();
          window.localWeb3.eth.defaultAccount = account;
        }
      } else {
        if (!account || !account.length) {
          throw new Error("Please make sure that you are connected using Metamask with a valid account");
        }
        account = account.toLowerCase();
        if (!forceRefresh && window.localWeb3.eth.defaultAccount === account) {
          return account;
        }
      }

      if ((window.localWeb3.currentProvider.connected
        || (window.localWeb3.currentProvider.isConnected && window.localWeb3.currentProvider.isConnected()))
        && account && account.length) {

        if (forceRefresh || (account !== this.account && this.lastCheckedAccount !== account)) {
          this.lastCheckedAccount = account;
          this.loading = true;
          this.displaySpaceAccountCreationHelp = false;
          this.currentAccountAlreadyInUse = false;
          this.errorMessage = null;
          this.accountsDetails = {};
          this.selectedAccount = null;
          return computeNetwork()
            .then((networkDetails, error) => {
              if (error) {
                throw error;
              }
              if (networkDetails && networkDetails.netId && networkDetails.netType) {
                this.networkId = networkDetails.netId;
                this.sameConfiguredNetwork = window.walletSettings.defaultNetworkId === this.networkId;
                this.networkName = `${networkDetails.netType.toUpperCase()} Network`;
              }
            })
            .then((result, error) => {
              if (error) {
                throw error;
              }
              this.account = window.localWeb3.eth.defaultAccount = account;
              return this.refreshBalance();
            })
            .then((result, error) => {
              if (error) {
                throw error;
              }
              return this.reloadContracts(this.account);
            })
            .catch(e => {
              console.debug("initAccount method - error", e);
              throw new Error(`Error encountered while loading contracts: ${e}`);
            });
        } else {
          return {ignoreRefresh: true};
        }
      } else {
        if (this.isSpace) {
          this.account = null;
          throw new Error('Can\'t connect space account to Network');
        }
      }
    },
    refreshBalance() {
      return computeBalance(this.account)
        .then((balanceDetails, error) => {
          if (error) {
            this.accountsDetails[this.account] = {
              title : 'ether',
              icon : 'warning',
              balance : 0,
              symbol : 'ether',
              isContract : false,
              address : this.account,
              error : `Error retrieving balance of account ${error}`
            };
            throw error;
          }
          const accountDetails = {
            title : 'ether',
            icon : 'fab fa-ethereum',
            symbol : 'ether',
            isContract : false,
            address : this.account,
            balance : balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0,
            balanceFiat : balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0
          };
          return this.accountsDetails[this.account] = accountDetails;
        })
        .catch(e => {
          console.debug("refreshBalance method - error", e);
          this.accountsDetails[this.account] = {
            title : 'ether',
            icon : 'warning',
            balance : 0,
            symbol : 'ether',
            isContract : false,
            address : this.account,
            error : `Error retrieving balance of account ${e}`,
          };
          throw e;
        });
    },
    reloadContracts(account) {
      this.showAddContractModal = false;
      if(!account) {
        account = this.account;
      }
      return getContractsDetails(account, this.networkId)
        .then((contractsDetails, error) => {
          if (error) {
            throw error;
          }
          if (contractsDetails && contractsDetails.length) {
            contractsDetails.forEach(contractDetails => {
              if (contractDetails && contractDetails.address) {
                if (this.accountsDetails[this.account]) {
                  contractDetails.etherBalance = this.accountsDetails[this.account].balance;
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
      if(deleteContractFromStorage(this.account, this.networkId, item.address)) {
        delete this.accountsDetails[item.address];
        this.forceUpdate();
      }
      event.preventDefault();
      event.stopPropagation();
    },
    back() {
      this.seeAccountDetails = false;
      this.selectedAccount = null;
      /*
      this.refreshList(this.account, true)
        .then(() => this.loading = false)
        .catch(e => {
          console.debug("refreshList method - error", e);
          this.errorMessage = `Error reloading Wallet application: ${e}`;
          this.loading = false;
        });
      */
    },
    initSpaceAccount() {
      return searchUserOrSpaceObject(eXo.env.portal.spaceGroup, 'space')
        .then((spaceObject, error) => {
          if (error) {
            throw error;
          }
          if(spaceObject
              && spaceObject.managers && spaceObject.managers.length && spaceObject.managers.indexOf(eXo.env.portal.userName) > -1
              && (!spaceObject.address || !spaceObject.address.length || this.newAccountAddress !== spaceObject.address)) {
            if (this.newAccountAddress) {
              return this.newAccountAddress;
            }
          }
          return null;
        })
        .then((account, error) => {
          if (error) {
            throw error;
          }
          if (account) {
            return searchFullName(this.newAccountAddress);
          } else {
            // The user is not administrator, so don't propose him
            // to associate the current address with space
            return {ignore: true};
          }
        }).then((item, error) => {
          if (error) {
            throw error;
          }
          if (!item || !item.ignore) {
            // If no association with currently selected account,
            // then, we can propose to associate it with current space
            if (!item || !item.id || !item.id.length) {
              // Display wallet association to space for creator only
              this.oldAccountAddress = null;
            } else {
              this.displaySpaceAccountCreationHelp = true;
              this.currentAccountAlreadyInUse = true;
            }
          }
          return item;
        });
    },
    initUserAccount() {
      return searchAddress(eXo.env.portal.userName, 'user')
        .then((address, error) => {
          if (error) {
            throw error;
          }
          this.newAccountAddress = this.account;
          if(address && address.length) {
            this.oldAccountAddress = address.toLowerCase();
          } else {
            this.oldAccountAddressNotFound = true;
          }
          if (this.newAddressDetected) {
            return searchFullName(this.newAccountAddress);
          } else {
            // The user is not administrator, so don't propose him
            // to associate the current address with space
            return {ignore: true};
          }
        }).then((item, error) => {
          if (error) {
            throw error;
          }
          if (item && item.id && item.id.length) {
            this.currentAccountAlreadyInUse = true;
          }
          return item;
        })
        .catch(e => {
          console.debug("searchAddress method - error", e);
          this.oldAccountAddressNotFound = true;
        });
    },
    retrieveDefaultUserAccount() {
      if (this.isSpace) {
        return this.initSpaceAccount();
      } else if(!this.oldAccountAddressNotFound) {
        return this.initUserAccount();
      }
    },
    saveNewAddressInWallet() {
      this.addressAssociationDialog = false;
      this.loading = true;
      return saveNewAddress(
        this.isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName,
        this.isSpace ? 'space' : 'user',
        this.newAccountAddress)
        .then((resp, error) => {
          if (error) {
            throw error;
          }
          if (resp && resp.ok) {
            this.oldAccountAddress = this.newAccountAddress;
            this.displaySpaceAccountCreationHelp = false;
            if (this.isSpace) {
              this.init();
            }
          } else {
            this.errorMessage = 'Error saving new Wallet address';
          }
          this.loading = false;
        })
        .catch(e => {
          console.debug("saveNewAddress method - error", e);
          this.errorMessage = 'Error saving new Wallet address';
          this.loading = false;
        });
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
    watchMetamaskAccount() {
      if (this.metamaskWatchInterval) {
        clearInterval(this.metamaskWatchInterval);
      }
      if (window.walletSettings.userPreferences.useMetamask && this.metamaskEnabled && this.metamaskConnected) {
        const thiss = this;
        // In case account switched in Metamask
        // See https://github.com/MetaMask/faq/blob/master/DEVELOPERS.md
        this.metamaskWatchInterval = setInterval(function() {
          thiss.getAccount()
            .then((account, error) => {
              if (error) {
                throw error;
              }
              return thiss.initAccount(account);
            })
            .then((result, error) => {
              if (error) {
                throw error;
              }
              if (!result || !result.ignoreRefresh) {
                thiss.retrieveDefaultUserAccount();
              }
            })
            .then((result, error) => {
              if (error) {
                throw error;
              }
              thiss.loading = false;
            })
            .catch(e => {
              console.debug("setInterval getAccount method - error", e);
              thiss.errorMessage = `${e}`;
              thiss.loading = false;
            });
        }, 1000);
      }
    }
  }
};
</script>