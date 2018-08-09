<template>
  <v-app id="WalletApp" :class="isMaximized ? 'maximized': 'minimized'" color="transaprent" flat>
    <!-- This is a workaround to avoid having a script error caused by User Profile Menu

    <div v-if="isMaximized" class="FakeMenu uiProfileMenu hidden">
      <ul class="nav nav-tabs userNavigation" style="visibility: hidden;">
        <li id="myWalletTad" class="item active">
          <a href="/portal/intranet/wallet">
            <div class="uiIconDefaultApp uiIconWallet"></div>
            <span class="tabName">My Wallet</span>
          </a>
        </li>
      </ul>
    </div>
    End workaround -->
    <main v-if="isWalletEnabled">
      <v-layout>
        <v-flex>
          <v-toolbar v-if="isMaximized" color="grey lighten-2" flat dense>
            <v-toolbar-title v-if="isSpace">Space Wallet</v-toolbar-title>
            <v-toolbar-title v-else>My Wallet</v-toolbar-title>
            <v-spacer />
            <add-contract-modal v-if="!isSpace"
                                :net-id="networkId"
                                :account="account"
                                :open="showAddContractModal"
                                @added="reloadContracts()"
                                @close="showAddContractModal = false" />
            <qr-code-modal :to="account"
                           :open="showQRCodeModal"
                           title="Address QR Code"
                           @close="showQRCodeModal = false" />
            <user-settings-modal :account="account"
                                 :open="showSettingsModal"
                                 @close="showSettingsModal = false"
                                 @settings-changed="reload" />
            <v-menu v-if="isMaximized" offset-y left>
              <v-btn slot="activator" icon>
                <v-icon>more_vert</v-icon>
              </v-btn>
              <v-list>
                <v-list-tile v-if="!isSpace && !selectedAccount" @click="showAddContractModal = true">
                  <v-list-tile-avatar>
                    <v-icon>add</v-icon>
                  </v-list-tile-avatar>
                  <v-list-tile-title>Add Token address</v-list-tile-title>
                </v-list-tile>
                <v-list-tile @click="showQRCodeModal = true">
                  <v-list-tile-avatar>
                    <v-icon>fa-qrcode</v-icon>
                  </v-list-tile-avatar>
                  <v-list-tile-title>Wallet QR Code</v-list-tile-title>
                </v-list-tile>
                <v-list-tile v-if="!isSpace" @click="showSettingsModal = true">
                  <v-list-tile-avatar>
                    <v-icon>fa-cog</v-icon>
                  </v-list-tile-avatar>
                  <v-list-tile-title>Settings</v-list-tile-title>
                </v-list-tile>
              </v-list>
            </v-menu>
            <v-btn v-else icon title="Maximize" @click="maximize">
              <v-icon color="blue-grey">fa-window-maximize</v-icon>
            </v-btn>
          </v-toolbar>
          <h4 v-else class="head-container">Wallet</h4>
          <v-card v-if="!selectedAccount" class="text-xs-center" flat>
            <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
            <v-alert :value="displaySpaceAccountCreationHelp" type="info" dismissible>
              <div>
                Current space doesn't have an account yet ? If you are manager of the space, you can create a new account using Metamask.
              </div>
              <div v-if="currentAccountAlreadyInUse">
                Currently selected account in Metamask is already in use, you can't set it for this space.
              </div>
            </v-alert>
            <v-alert :value="oldAccountAddress && newAccountAddress && oldAccountAddress !== newAccountAddress" type="info" dismissible>
              <div>
                Would you like to replace your wallet address <code>{{ oldAccountAddress }}</code> by the current address <code>{{ newAccountAddress }}</code> ?
              </div>
              <v-btn @click="saveNewAddressInWallet">
                <v-icon color="success">fa-check</v-icon>
              </v-btn>
            </v-alert>
            <v-alert :value="!oldAccountAddress && newAccountAddress" type="info" dismissible>
              <div v-if="isSpace">
                Would you like to use the current address <code>{{ newAccountAddress }}</code> in Space Wallet ?
              </div>
              <div v-else>
                Would you like to use the current address <code>{{ newAccountAddress }}</code> in your Wallet ?
              </div>
              <v-btn @click="saveNewAddressInWallet">
                <v-icon color="success">fa-check</v-icon>
              </v-btn>
            </v-alert>
            <v-alert :value="error && !displaySpaceAccountCreationHelp" type="error">
              {{ error }}
            </v-alert>
            <v-list class="pb-0" two-line subheader>
              <v-list-tile v-for="(item, index) in accountsDetails" :key="index" :color="item.error ? 'red': ''" avatar ripple @click="openAccountDetail(item)">
                <v-list-tile-avatar>
                  <v-icon :class="item.error ? 'red':'purple'" dark>{{ item.icon }}</v-icon>
                </v-list-tile-avatar>
                <v-list-tile-content>
                  <v-list-tile-title v-if="item.error"><strike>{{ item.title }}</strike></v-list-tile-title>
                  <v-list-tile-title v-else v-html="item.title"></v-list-tile-title>

                  <v-list-tile-sub-title v-if="item.error">{{ item.error }}</v-list-tile-sub-title>
                  <v-list-tile-sub-title v-else>{{ item.balance }} {{ item.symbol }} {{ item.balanceUSD ? `(${item.balanceUSD} \$)`:'' }}</v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action v-if="isMaximized && !isSpace && item.isContract && !item.isDefault">
                  <v-btn icon ripple @click="deleteContract(item, $event)">
                    <v-icon color="primary">delete</v-icon>
                  </v-btn>
                </v-list-tile-action>
              </v-list-tile>
              <v-divider v-if="index + 1 < accountsDetails.length" :key="`divider-${index}`"></v-divider>
            </v-list>
          </v-card>
          <account-detail v-else :is-space="isSpace" :account="account" :contract-detail="selectedAccount" @back="reload" />
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import AccountDetail from './AccountDetail.vue';
import QrCodeModal from './QRCodeModal.vue';
import UserSettingsModal from './UserSettingsModal.vue';

import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {searchAddress, searchUserOrSpaceObject, searchFullName, saveNewAddress} from '../WalletAddressRegistry.js';
import {retrieveUSDExchangeRate, etherToUSD, initWeb3, initSettings, computeNetwork, computeBalance} from '../WalletUtils.js';

export default {
  components: {
    UserSettingsModal,
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
      currentAccountAlreadyInUse: false,
      displaySpaceAccountCreationHelp: false,
      oldAccountAddressNotFound: null,
      oldAccountAddress: null,
      newAccountAddress: null,
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
    error() {
      if(this.loading) {
        return null;
      } else if (this.errorMessage) {
        return this.errorMessage;
      } else if (!this.isSpace && !this.metamaskEnabled) {
        return 'Please install or Enable Metamask';
      } else if (!this.isSpace && !this.metamaskConnected) {
        return 'Please connect Metamask to the network';
      } else if (!this.isSpace && !this.account) {
        return 'Please select a valid account using Metamask';
      }
      return null;
    }
  },
  watch: {
    account(value, oldValue) {
      if (!value) {
        return;
      }
      this.refreshBalance()
        .catch(e => {
          this.errorMessage = `Error while retrieving user balance ${e}`;
        });
    }
  },
  created() {
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
            window.addEventListener('load', function() {
              if (window && window.localWeb3 && window.localWeb3.eth && window.localWeb3.eth.defaultAccount !== this.account) {
                thiss.init();
              }
            });
          })
          .catch(e => {
            this.errorMessage = `${e}`;
            this.loading = false;
          });
      }
    } catch (e) {
      this.errorMessage = `${e}`;
      this.loading = false;
    }
  },
  methods: {
    init() {
      this.errorMessage = null;
      return initSettings()
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
          if (!this.isSpace) {
            const thiss = this;
            // In case account switched in Metamars
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
          this.errorMessage = `${e}`;
          this.loading = false;
        });
    },
    refreshList(forceRefresh) {
      this.loading = true;
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
            return this.oldAccountAddress = this.newAccountAddress = this.account = window.web3.eth.defaultAccount;
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
          this.errorMessage = null;
          this.accountsDetails = {};
          return computeNetwork()
            .then((networkDetails, error) => {
              if (error) {
                throw error;
              }
              if (networkDetails && networkDetails.netId && networkDetails.netType) {
                this.networkId = networkDetails.netId;
                this.networkName = `${networkDetails.netType.toUpperCase()} Network`;
              }
            })
            .then((result, error) => {
              if (error) {
                throw error;
              }
              const accountDetails = this.accountsDetails[account] = {};
              accountDetails.title = 'Account in ETH';
              accountDetails.icon = 'fab fa-ethereum';
              accountDetails.balance = 0;
              accountDetails.symbol = 'ETH';
              accountDetails.isContract = false;
              accountDetails.address = account;

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
      return computeBalance()
        .then((balanceDetails, error) => {
          if (error) {
            throw error;
          }
          if (balanceDetails && balanceDetails.balance) {
            this.accountsDetails[this.account].balance = balanceDetails.balance;
            this.accountsDetails[this.account].balanceUSD = balanceDetails.balanceUSD;
          }
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
                this.accountsDetails[contractDetails.address] = contractDetails;
              }
            });
            this.$forceUpdate();
          }
        });
    },
    openAccountDetail(accountDetails) {
      if (!this.isMaximized) {
        this.maximize();
      } else if (!accountDetails.error) {
        this.selectedAccount = accountDetails;
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
    reload() {
      this.refreshList(this.account, true)
        .then(() => this.loading = false)
        .catch(e => {
          this.errorMessage = `Error reloading Wallet application: ${e}`;
          this.loading = false;
        });
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
        })
        .catch(e => {
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
      this.loading = true;
      saveNewAddress(
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