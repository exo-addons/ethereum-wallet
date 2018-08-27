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
                             @add-contract="showAddContractModal = true"
                             @show-wallet-address="showWalletAddress = true"
                             @show-qr-code="showQRCodeModal = true"
                             @modify-settings="showSettingsModal = true" />

            <add-contract-modal v-if="!isSpace"
                                :net-id="networkId"
                                :account="account"
                                :open="showAddContractModal"
                                @added="reloadContracts()"
                                @close="showAddContractModal = false" />

            <wallet-address-modal :address="account"
                                  :open="showWalletAddress"
                                  @close="showWalletAddress = false" />

            <qr-code-modal :to="account"
                           :open="showQRCodeModal"
                           title="Address QR Code"
                           @close="showQRCodeModal = false" />

            <user-settings-modal :account="account"
                                 :open="showSettingsModal"
                                 @close="showSettingsModal = false"
                                 @settings-changed="refreshList(true)" />
          </v-toolbar>

          <!-- Body -->
          <v-card v-if="displayAccountsList" class="text-xs-center" flat>
            <v-progress-circular v-show="loading" color="primary" indeterminate></v-progress-circular>

            <wallet-app-alerts :display-not-same-network-warning="displayNotSameNetworkWarning"
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

            <!-- list of contracts and ETH account -->
            <v-list class="pb-0" two-line subheader>
              <v-list-tile v-for="(item, index) in accountsDetails" :key="index" :color="item.error ? 'red': ''" :title="item.title" avatar ripple @click="openAccountDetail(item)">
                <v-list-tile-avatar>
                  <v-icon :class="item.error ? 'red--text':'uiIconBlue'" dark>{{ item.icon }}</v-icon>
                </v-list-tile-avatar>
                <v-list-tile-content>
                  <v-list-tile-title v-if="item.error"><strike>{{ item.title }}</strike></v-list-tile-title>
                  <v-list-tile-title v-else>{{ item.title }}</v-list-tile-title>

                  <v-list-tile-sub-title v-if="item.error">{{ item.error }}</v-list-tile-sub-title>
                  <v-list-tile-sub-title v-else>{{ item.balance }} {{ item.symbol }} {{ item.balanceUSD ? `(${item.balanceUSD} \$)`:'' }}</v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action v-if="!isSpace && item.isContract && !item.isDefault">
                  <v-btn icon ripple @click="deleteContract(item, $event)">
                    <i class="uiIconTrash uiIconBlue"></i>
                  </v-btn>
                </v-list-tile-action>
              </v-list-tile>
              <v-divider v-if="index + 1 < accountsDetails.length" :key="`divider-${index}`"></v-divider>
            </v-list>
          </v-card>

          <!-- The selected account detail -->
          <v-navigation-drawer v-if="!isMaximized" id="accountDetailsDrawer" v-model="seeAccountDetails" :permanent="seeAccountDetailsPermanent" fixed temporary right width="700" max-width="100vw">
            <account-detail :is-space="isSpace" :account="account" :contract-detail="selectedAccount" @back="back"/>
          </v-navigation-drawer>
          <account-detail v-else-if="selectedAccount" :is-space="isSpace" :account="account" :contract-detail="selectedAccount" @back="back"/>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import WalletAppMenu from './WalletAppMenu.vue';
import WalletAppAlerts from './WalletAppAlerts.vue';
import AddContractModal from './AddContractModal.vue';
import AccountDetail from './AccountDetail.vue';
import QrCodeModal from './QRCodeModal.vue';
import WalletAddressModal from './WalletAddressModal.vue';
import UserSettingsModal from './UserSettingsModal.vue';

import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {searchAddress, searchUserOrSpaceObject, searchFullName, saveNewAddress} from '../WalletAddressRegistry.js';
import {retrieveUSDExchangeRate, etherToUSD, initWeb3, initSettings, computeNetwork, computeBalance} from '../WalletUtils.js';

export default {
  components: {
    UserSettingsModal,
    WalletAddressModal,
    WalletAppMenu,
    WalletAppAlerts,
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
      isWalletEnabled: false,
      sameConfiguredNetwork: true,
      networkLabel: '',
      loading: true,
      currentAccountAlreadyInUse: false,
      displaySpaceAccountCreationHelp: false,
      oldAccountAddressNotFound: null,
      oldAccountAddress: null,
      newAccountAddress: null,
      showWalletAddress: false,
      showQRCodeModal: false,
      showSettingsModal: false,
      showAddContractModal: false,
      networkId: null,
      networkName: null,
      account: null,
      lastCheckedAccount: null,
      selectedAccount: null,
      contracts: [],
      accountsDetails: {},
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
      return this.hasErrors && !this.loading && !this.displaySpaceAccountCreationHelp && !this.displaySpaceMetamaskEnableHelp;
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
      return !this.loading && (this.errorMessage || (!this.isSpace && (!this.metamaskEnabled || !this.metamaskConnected || !this.account)));
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
    $(document).on("click", () => {
      this.walletConfigurationMenu = false;
    });
    // Init application
    try {
      if (this.isSpace || (this.metamaskEnabled && this.metamaskConnected)) {
        this.loading = true;
        this.init();
      } else {
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
          })
          .catch(e => {
            console.debug("init method - error", e);
            this.errorMessage = `${e}`;
            this.loading = false;
          });
      }
    } catch (e) {
      console.debug("init method - error", e);
      this.errorMessage = `${e}`;
      this.loading = false;
    }
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
            this.$forceUpdate();
            throw new Error("Wallet disabled for current user");
          } else {
            this.isWalletEnabled = true;
            this.$forceUpdate();
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
          return retrieveUSDExchangeRate();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          return this.refreshList(result);
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          this.retrieveDefaultUserAccount();
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          if (!this.isSpace && this.metamaskEnabled && this.metamaskConnected) {
            const thiss = this;
            // In case account switched in Metamask
            // See https://github.com/MetaMask/faq/blob/master/DEVELOPERS.md
            setInterval(function() {
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
                  thiss.retrieveDefaultUserAccount();
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
        })
        .then((result, error) => {
          if (error) {
            throw error;
          }
          this.loading = false;
        })
        .catch(e => {
          console.debug("init method - error", e);
          this.errorMessage = `${e}`;
          this.loading = false;
        })
        .finally(() => {
          if (window.walletSettings && window.walletSettings.defaultNetworkId) {
            switch (window.walletSettings.defaultNetworkId) {
            case 0:
              this.networkLabel = '';
              break;
            case 1:
              this.networkLabel = 'Main network';
              break;
            case 2:
              this.networkLabel = 'Ethereum Classic main network';
              break;
            case 3:
              this.networkLabel = 'Ropsten network';
              break;
            case 4:
              this.networkLabel = 'Rinkeby network';
              break;
            case 42:
              this.networkLabel = 'Kovan network';
              break;
            default:
              this.networkLabel = 'custom network';
            }
          }
        });
    },
    refreshList(forceRefresh) {
      this.loading = true;
      this.seeAccountDetails = false;
      this.errorMessage = null;
      this.selectedAccount = null;

      return this.getAccount(this.isSpace)
        .then((account, error) => {
          if (error) {
            throw error;
          }
          return this.initAccount(account, forceRefresh);
        })
        .catch(e => {
          console.debug("refreshList method - error", e);
          this.errorMessage = `${e}`;
          this.loading = false;
        });
    },
    getAccount(isSpace) {
      if(isSpace) {
        if (!eXo.env.portal.spaceGroup) {
          throw new Error("Space identifier is empty");
        }
        return searchAddress(eXo.env.portal.spaceGroup, 'space');
      } else {
        return window.localWeb3.eth.getCoinbase();
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
              title : 'ETH',
              icon : 'warning',
              balance : 0,
              symbol : 'ETH',
              isContract : false,
              address : this.account,
              error : `Error retrieving balance of account ${error}`
            };
            throw error;
          }
          const accountDetails = {
            title : 'ETH',
            icon : 'fab fa-ethereum',
            symbol : 'ETH',
            isContract : false,
            address : this.account,
            balance : balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0,
            balanceUSD : balanceDetails && balanceDetails.balanceUSD ? balanceDetails.balanceUSD : 0
          };
          return this.accountsDetails[this.account] = accountDetails;
        })
        .catch(e => {
          console.debug("refreshBalance method - error", e);
          this.accountsDetails[this.account] = {
            title : 'ETH',
            icon : 'warning',
            balance : 0,
            symbol : 'ETH',
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
            this.$forceUpdate();
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
        this.$forceUpdate();
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
    }
  }
};
</script>