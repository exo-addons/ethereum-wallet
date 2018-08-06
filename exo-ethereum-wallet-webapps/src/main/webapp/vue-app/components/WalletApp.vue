<template>
  <v-app id="WalletApp">
    <main>
      <v-layout row>
        <v-flex v-if="!selectedAccount" xs12 sm6 offset-sm3>
          <v-card class="text-xs-center">
            <v-toolbar >
              <v-toolbar-title>Wallet ({{ networkName }})</v-toolbar-title>
              <v-spacer />
              <add-contract-modal v-if="!isSpace"
                                  :net-id="networkId"
                                  :account="account"
                                  :open="showAddContractModal"
                                  @added="reloadContracts()"
                                  @close="showAddContractModal = false" />
              <qr-code-modal :to="account"
                             :open="showQRCodeModal"
                             title="Wallet Address QR Code"
                             @close="showQRCodeModal = false" />
              <user-settings-modal :account="account"
                                   :open="showSettingsModal"
                                   @close="showSettingsModal = false"
                                   @settings-changed="applySettings" />
              <v-menu offset-y left>
                <v-btn slot="activator" icon>
                  <v-icon>more_vert</v-icon>
                </v-btn>
                <v-list>
                  <v-list-tile v-if="!isSpace" @click="showAddContractModal = true">
                    <v-list-tile-avatar>
                      <v-icon>add</v-icon>
                    </v-list-tile-avatar>
                    <v-list-tile-title>Add Token</v-list-tile-title>
                  </v-list-tile>
                  <v-list-tile @click="showQRCodeModal = true">
                    <v-list-tile-avatar>
                      <v-icon>fa-qrcode</v-icon>
                    </v-list-tile-avatar>
                    <v-list-tile-title>QR Code</v-list-tile-title>
                  </v-list-tile>
                  <v-list-tile v-if="!isSpace" @click="showSettingsModal = true">
                    <v-list-tile-avatar>
                      <v-icon>fa-cog</v-icon>
                    </v-list-tile-avatar>
                    <v-list-tile-title>Settings</v-list-tile-title>
                  </v-list-tile>
                </v-list>
              </v-menu>
            </v-toolbar>
            <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
            <v-alert :value="oldAccountAddress && newAccountAddress && oldAccountAddress !== newAccountAddress" type="info" dismissible>
              <div>
                Would you like to replace your wallet address <code>{{ oldAccountAddress }}</code> by the current address <code>{{ newAccountAddress }}</code> ?
              </div>
              <v-btn>
                <v-icon color="success" @click="saveNewAddressInWallet">fa-check</v-icon>
              </v-btn>
            </v-alert>
            <v-alert :value="!oldAccountAddress && newAccountAddress" type="info" dismissible>
              <div v-if="isSpace">
                Would you like to use the current address <code>{{ newAccountAddress }}</code> in Space Wallet ?
              </div>
              <div v-else>
                Would you like to use the current address <code>{{ newAccountAddress }}</code> in your Wallet ?
              </div>
              <v-btn>
                <v-icon color="success" @click="saveNewAddressInWallet">fa-check</v-icon>
              </v-btn>
            </v-alert>
            <v-alert :value="error" type="error">
              {{ error }}
            </v-alert>
            <v-list two-line subheader>
              <template v-for="(item, index) in accountsDetails">
                <v-list-tile :key="index" :color="item.error ? 'red': ''" avatar ripple @click="openAccountDetail(item)">
                  <v-list-tile-avatar>
                    <v-icon :class="item.error ? 'red':'purple'" dark>{{ item.icon }}</v-icon>
                  </v-list-tile-avatar>
                  <v-list-tile-content>
                    <v-list-tile-title v-if="item.error"><strike>{{ item.title }}</strike></v-list-tile-title>
                    <v-list-tile-title v-else v-html="item.title"></v-list-tile-title>

                    <v-list-tile-sub-title v-if="item.error">{{ item.error }}</v-list-tile-sub-title>
                    <v-list-tile-sub-title v-else>{{ item.balance }} {{ item.symbol }} {{ item.balanceUSD ? `(${item.balanceUSD} \$)`:'' }}</v-list-tile-sub-title>
                  </v-list-tile-content>
                  <v-list-tile-action v-if="!isSpace && item.isContract && !item.isDefault">
                    <v-btn icon ripple @click="deleteContract(item, $event)">
                      <v-icon color="primary">delete</v-icon>
                    </v-btn>
                  </v-list-tile-action>
                </v-list-tile>
                <v-divider v-if="index + 1 < accountsDetails.length" :key="`divider-${index}`"></v-divider>
              </template>
            </v-list>
          </v-card>
        </v-flex>
        <account-detail v-else :is-space="isSpace" :account="account" :contract-detail="selectedAccount" @back="refreshList(account, true)"></account-detail>
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
import {searchAddress, searchUserOrSpaceObject} from '../WalletAddressRegistry.js';
import {retrieveUSDExchangeRate, etherToUSD, initWeb3, initSettings} from '../WalletUtils.js';

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
      loading: true,
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
      return web3 && web3.currentProvider && web3.currentProvider.isMetaMask;
    },
    metamaskConnected () {
      return this.metamaskEnabled && web3.currentProvider.isConnected();
    },
    error() {
      if(this.loading) {
        return null;
      } else if (this.errorMessage) {
        return this.errorMessage;
      } else if (!this.metamaskEnabled) {
        return 'Please install or Enable Metamask';
      } else if (!this.metamaskConnected) {
        return 'Please connect Metamask to the network';
      } else if (!this.account && !this.isSpace) {
        return 'Please select a valid account using Metamask';
      }
      return null;
    }
  },
  watch: {
    account(value) {
      if (!value) {
        return;
      }
      this.computeBalance();
    },
    errorMessage() {
      if (this.errorMessage && this.errorMessage.length) {
        this.loading = false;
      }
    }
  },
  created() {
    if (this.isSpace || this.metamaskEnabled && this.metamaskConnected) {
      this.init();
    } else {
      const thiss = this;
      window.addEventListener('load', function() {
        thiss.init();
      });
    }
  },
  methods: {
    init() {
      this.errorMessage = null;
      initSettings()
        .then(() => initWeb3(this.isSpace))
        .then(retrieveUSDExchangeRate)
        .then(this.refreshList)
        .then(this.retrieveDefaultUserAccount);

      if (!this.isSpace) {
        const thiss = this;
        // In case account switched in Metamars
        // See https://github.com/MetaMask/faq/blob/master/DEVELOPERS.md
        setInterval(function() {
          thiss.getAccount()
            .then(thiss.initAccount)
            .then(thiss.retrieveDefaultUserAccount);
        }, 1000);
      }
    },
    refreshList(forceRefresh) {
      this.loading = true;
      this.errorMessage = null;
      this.selectedAccount = null;

      return this.getAccount()
        .then(account => this.initAccount(account, forceRefresh))
        .then(() => this.loading = false)
        .catch((e) => {
          this.errorMessage = `${e}`;
          this.loading = false;
        });
    },
    getAccount() {
      if(this.isSpace) {
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
          // Display information to allo administrator to associate
          // This new address with wallet
          return window.localWeb3.eth.getCoinbase()
            .then((account) => this.oldAccountAddress = this.newAccountAddress = account);
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
        || window.localWeb3.currentProvider.isConnected && window.localWeb3.currentProvider.isConnected())
        && account && account.length) {

        if (forceRefresh || account !== this.account && this.lastCheckedAccount !== account) {
          this.lastCheckedAccount = account;
          this.errorMessage = '';
          this.account = null;
          this.accountsDetails = {};
          return this.computeNetwork()
            .then(() => {
              const accountDetails = this.accountsDetails[account] = {};
              accountDetails.title = 'Account in ETH';
              accountDetails.icon = 'fab fa-ethereum';
              accountDetails.balance = 0;
              accountDetails.symbol = 'ETH';
              accountDetails.isContract = false;
              accountDetails.address = account;

              this.account = window.localWeb3.eth.defaultAccount = account;
            })
            .then(() => this.reloadContracts(account))
            .catch(err => {
              this.errorMessage = `Error encountered while loading contracts: ${err}`;
              console.error('Error encountered while loading contracts', err);
            });
        }
      } else {
        if (this.isSpace) {
          this.account = null;
          this.errorMessage = 'Please make sure that you are connected using Metamask with a valid account';
        }
      }
    },
    computeNetwork() {
      return window.localWeb3.eth.net.getId()
        .then(netId => {
          this.networkId = netId;
          return window.localWeb3.eth.net.getNetworkType();
        })
        .then(netType => this.networkName = `${netType.toUpperCase()} Network`);
    },
    computeBalance() {
      window.localWeb3.eth.getBalance(window.localWeb3.eth.defaultAccount)
        .then(balance => this.accountsDetails[this.account].balance = window.localWeb3.utils.fromWei(balance, "ether"))
        .then(balance => this.accountsDetails[this.account].balanceUSD = etherToUSD(balance))
        .then(() => this.$forceUpdate());
    },
    reloadContracts(account) {
      this.showAddContractModal = false;
      if(!account) {
        account = this.account;
      }
      return getContractsDetails(account, this.networkId)
        .then(contractsDetails => {
          if (contractsDetails && contractsDetails.length) {
            contractsDetails.forEach(contractDetails => {
              this.accountsDetails[contractDetails.address] = contractDetails;
            });
            this.$forceUpdate();
          }
        });
    },
    openAccountDetail(accountDetails) {
      if (!accountDetails.error) {
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
    applySettings(newSettings) {
      this.refreshList(this.account, true);
    },
    retrieveDefaultUserAccount() {
      if (this.isSpace) {
        searchUserOrSpaceObject(eXo.env.portal.spaceGroup, 'space')
          .then(spaceObject => {
            if(spaceObject
                && spaceObject.creator && spaceObject.creator === eXo.env.portal.userName
                && (!spaceObject.address || !spaceObject.address.length)) {
              // Display wallet association to space for creator only
              this.oldAccountAddress = null;
            }
          });
      } else if(!this.oldAccountAddressNotFound) {
        searchAddress(eXo.env.portal.userName, 'user')
          .then(address => {
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
      }
    },
    saveNewAddressInWallet() {
      this.loading = true;
      fetch('/portal/rest/wallet/api/account/saveAddress', {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          type: this.isSpace ? 'space' : 'user',
          id: this.isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName,
          address: this.newAccountAddress
        })
      }).then(resp => {
        if (resp && resp.ok) {
          if (this.isSpace) {
            this.init();
          } else {
            this.oldAccountAddress = this.newAccountAddress;
          }
        } else {
          this.errorMessage = 'Error saving new Wallet address';
        }
        this.loading = false;
      });
    }
  }
};
</script>