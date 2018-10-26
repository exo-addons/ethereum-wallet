<template>
  <v-app v-if="isWalletEnabled" id="WalletAdminApp" color="transaprent" flat>
    <main>
      <v-layout>
        <v-flex class="white text-xs-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <v-dialog v-model="loading" persistent width="300">
            <v-card color="primary" dark>
              <v-card-text>
                Loading ...
                <v-progress-linear indeterminate color="white" class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <v-tabs v-if="displaySettings" v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
            <v-tab key="general">Settings</v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="funds">Initial accounts funds</v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="overview">Advanced settings</v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="contracts">Contracts</v-tab>
            <v-tab v-if="sameConfiguredNetwork" key="wallets">Wallets</v-tab>
          </v-tabs>

          <v-tabs-items v-if="displaySettings" v-model="selectedTab">
            <v-tab-item key="general">
              <v-card v-if="!loadingSettings" class="text-xs-center pr-3 pl-3 pt-2" flat>
                <v-combobox
                  v-model="selectedNetwork"
                  :items="networks"
                  label="Select ethereum network" />

                <v-text-field
                  v-if="showSpecificNetworkFields"
                  v-model="selectedNetwork.value"
                  :rules="mandatoryRule"
                  :label="`Ethereum Network ID (current id: ${networkId})`"
                  type="number"
                  name="defaultNetworkId" />

                <v-text-field
                  v-if="showSpecificNetworkFields"
                  ref="providerURL"
                  v-model="selectedNetwork.httpLink"
                  :rules="mandatoryRule"
                  type="text"
                  name="providerURL"
                  label="Ethereum Network HTTP URL used for static displaying spaces wallets (without Metamask)"
                  autofocus />

                <v-text-field
                  v-if="showSpecificNetworkFields"
                  ref="websocketProviderURL"
                  v-model="selectedNetwork.wsLink"
                  :rules="mandatoryRule"
                  type="text"
                  name="websocketProviderURL"
                  label="Ethereum Network Websocket URL used for notifications" />

                <v-flex id="accessPermissionAutoComplete" class="contactAutoComplete mt-4">
                  <v-autocomplete
                    v-if="sameConfiguredNetwork"
                    ref="accessPermissionAutoComplete"
                    v-model="accessPermission"
                    :items="accessPermissionOptions"
                    :loading="isLoadingSuggestions"
                    :search-input.sync="accessPermissionSearchTerm"
                    attach="#accessPermissionAutoComplete"
                    label="Wallet access permission (Spaces only)"
                    class="contactAutoComplete"
                    placeholder="Start typing to Search a space"
                    content-class="contactAutoCompleteContent"
                    max-width="100%"
                    item-text="name"
                    item-value="id"
                    hide-details
                    hide-selected
                    chips
                    cache-items
                    dense
                    flat>
                    <template slot="no-data">
                      <v-list-tile>
                        <v-list-tile-title>
                          Search for a <strong>Space</strong>
                        </v-list-tile-title>
                      </v-list-tile>
                    </template>
      
                    <template slot="selection" slot-scope="{ item, selected }">
                      <v-chip v-if="item.error" :selected="selected" class="autocompleteSelectedItem">
                        <del><span>{{ item.name }}</span></del>
                      </v-chip>
                      <v-chip v-else :selected="selected" class="autocompleteSelectedItem">
                        <span>{{ item.name }}</span>
                      </v-chip>
                    </template>
  
                    <template slot="item" slot-scope="{ item, tile }">
                      <v-list-tile-avatar v-if="item.avatar" tile size="20">
                        <img :src="item.avatar">
                      </v-list-tile-avatar>
                      <v-list-tile-title v-text="item.name" />
                    </template>
                  </v-autocomplete>
                </v-flex>

                <v-combobox
                  v-if="sameConfiguredNetwork"
                  v-model="selectedPrincipalAccount"
                  :items="accountsList"
                  class="mt-4"
                  item-disabled="itemDisabled"
                  label="Select principal account displayed in wallet overview"
                  placeholder="Select principal account displayed in wallet overview"
                  chips />

                <v-combobox
                  v-if="sameConfiguredNetwork"
                  v-model="selectedOverviewAccounts"
                  :items="accountsList"
                  label="List of currencies to use (by order)"
                  placeholder="List of contracts, ether and fiat to use in wallet application (by order)"
                  multiple
                  deletable-chips
                  chips />
    
                <v-card-actions>
                  <v-spacer />
                  <button class="btn btn-primary mb-3" @click="saveGlobalSettings">
                    Save
                  </button>
                  <v-spacer />
                </v-card-actions>

              </v-card>
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" key="funds">
              <v-card v-if="!loadingSettings" class="text-xs-center pr-3 pl-3 pt-2" flat>
                <v-card-title>
                  The following settings manages the funds holder and the amount of initial funds to send for a user that has created a new wallet for the first time.
                  You can choose to set initial funds for a token to 0 so that no funds will be send.
                  The funds holder will receive a notification per user per currency (ether and/or token).
                </v-card-title>
                <v-card-text>
                  <v-flex id="fundsHolderAutoComplete" class="contactAutoComplete">
                    <v-autocomplete
                      v-if="sameConfiguredNetwork"
                      ref="fundsHolderAutoComplete"
                      v-model="fundsHolder"
                      :items="fundsHolderOptions"
                      :loading="isLoadingSuggestions"
                      :search-input.sync="fundsHolderSearchTerm"
                      attach="#fundsHolderAutoComplete"
                      label="Wallet funds holder"
                      class="contactAutoComplete"
                      placeholder="Start typing to Search a user"
                      content-class="contactAutoCompleteContent"
                      max-width="100%"
                      item-text="name"
                      item-value="id"
                      hide-details
                      hide-selected
                      chips
                      cache-items
                      dense
                      flat>
                      <template slot="no-data">
                        <v-list-tile>
                          <v-list-tile-title>
                            Search for a <strong>user</strong>
                          </v-list-tile-title>
                        </v-list-tile>
                      </template>
        
                      <template slot="selection" slot-scope="{ item, selected }">
                        <v-chip :selected="selected" class="autocompleteSelectedItem">
                          <span>{{ item.name }}</span>
                        </v-chip>
                      </template>
    
                      <template slot="item" slot-scope="{ item, tile }">
                        <v-list-tile-avatar v-if="item.avatar" tile size="20">
                          <img :src="item.avatar">
                        </v-list-tile-avatar>
                        <v-list-tile-title v-text="item.name" />
                      </template>
                    </v-autocomplete>
                  </v-flex>

                  <v-textarea
                    id="initialFundsRequestMessage"
                    v-model="initialFundsRequestMessage"
                    name="initialFundsRequestMessage"
                    label="Initial funds request message"
                    placeholder="You can enter a custom message to send with initial funds request"
                    class="mt-4"
                    rows="7"
                    flat
                    no-resize />
                </v-card-text>

                <v-card-text class="text-xs-left">
                  <v-label light>Default amount of automatic initial funds request</v-label>
                  <v-data-table :headers="initialFundsHeaders" :items="initialFunds" :sortable="false" hide-actions>
                    <template slot="items" slot-scope="props">
                      <td class="text-xs-left">
                        {{ props.item.name ? props.item.name : props.item.address }}
                      </td>
                      <td>
                        <v-text-field v-model="props.item.amount" single-line />
                      </td>
                    </template>
                  </v-data-table>
                </v-card-text>
                <v-card-actions>
                  <v-spacer />
                  <button class="btn btn-primary mb-3" @click="saveGlobalSettings">
                    Save
                  </button>
                  <v-spacer />
                </v-card-actions>
              </v-card>
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" key="overview">
              <v-card v-if="!loadingSettings && !error" class="text-xs-center pr-3 pl-3 pt-2" flat>
                <div>
                  <v-switch v-model="enableDelegation" label="Enable token delegation operations for wallets by default"></v-switch>
                </div>

                <v-slider
                  v-model="defaultGas"
                  :label="`Maximum transaction fee: ${defaultGas} ${defaultGasFiatPrice ? '(' + defaultGasFiatPrice + ' ' + fiatSymbol + ')' : ''}`"
                  :min="35000"
                  :max="100000"
                  :step="1000"
                  type="number"
                  class="mt-4"
                  required />

                <v-slider
                  v-model="defaultBlocksToRetrieve"
                  :label="`Default blocks to retrieve for ether transactions: ${defaultBlocksToRetrieve}`"
                  :min="100"
                  :max="10000"
                  :step="100"
                  type="number"
                  required />

                <v-card-actions>
                  <v-spacer />
                  <button class="btn btn-primary mb-3" @click="saveGlobalSettings">
                    Save
                  </button>
                  <v-spacer />
                </v-card-actions>

              </v-card>
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" key="contracts">
              <wallet-setup
                ref="walletSetup"
                :wallet-address="walletAddress"
                :refresh-index="refreshIndex"
                class="mb-3"
                @loading="loadingContracts = true"
                @end-loading="loadingContracts = false"
                @refresh="init()"
                @error="loadingContracts = false; error = $event" />
              <v-card-title v-if="loadingContracts">
                <v-spacer />
                <v-progress-circular indeterminate color="primary" />
                <v-spacer />
              </v-card-title>
              <v-card v-else flat>
                <v-subheader class="text-xs-center">Default contracts</v-subheader>
                <v-divider />

                <div v-if="newTokenAddress" class="alert alert-success v-content">
                  <i class="uiIconSuccess"></i>
                  Contract created under address: 
                  <wallet-address :value="newTokenAddress" />
                </div>
                <v-data-table :headers="headers" :items="contracts" :sortable="false" class="elevation-1 mr-3 ml-3" hide-actions>
                  <template slot="items" slot-scope="props">
                    <td :class="props.item.error ? 'red--text' : ''">{{ props.item.error ? props.item.error : props.item.name }}</td>
                    <td v-if="props.item.error" class="text-xs-right"><del>{{ props.item.address }}</del></td>
                    <td v-else class="text-xs-right">{{ props.item.address }}</td>
                    <td class="text-xs-right">
                      <v-progress-circular v-if="props.item.isPending" :width="3" indeterminate color="primary" />
                      <v-btn v-else icon ripple @click="deleteContract(props.item, $event)">
                        <i class="uiIconTrash uiIconBlue"></i>
                      </v-btn>
                    </td>
                  </template>
                </v-data-table>
                <v-divider />
                <div class="text-xs-center pt-2 pb-2">
                  <deploy-new-contract 
                    :account="walletAddress"
                    :network-id="networkId"
                    :fiat-symbol="fiatSymbol"
                    @list-updated="updateList($event)"/>

                  <button class="btn mt-3" @click="showAddContractModal = true">
                    Add Existing contract Address
                  </button>
                  <add-contract-modal
                    :net-id="networkId"
                    :account="walletAddress"
                    :open="showAddContractModal"
                    :is-default-contract="true"
                    @added="contractsModified"
                    @close="showAddContractModal = false" />
                </div>
              </v-card>
            </v-tab-item>

            <v-tab-item v-if="sameConfiguredNetwork" key="wallets">
              <v-card-title v-if="loadingWallets">
                <v-spacer />
                <v-progress-circular indeterminate color="primary" />
                <v-spacer />
              </v-card-title>
              <v-card v-else flat>
                <v-data-table :headers="walletHeaders" :items="wallets">
                  <template slot="items" slot-scope="props">
                    <tr class="clickable" @click="openAccountDetail(props.item)">
                      <td>
                        <v-avatar size="36px">
                          <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
                        </v-avatar>
                      </td>
                      <td>
                        {{ props.item.name }}
                      </td>
                      <td>
                        {{ props.item.address }}
                      </td>
                      <td class="text-xs-right">
                        <v-card-title v-if="props.item.loadingBalancePrincipal" primary-title class="pb-0">
                          <v-spacer />
                          <v-badge color="red" right title="A transaction is in progress">
                            <v-progress-circular color="primary" indeterminate size="20"></v-progress-circular>
                          </v-badge>
                        </v-card-title>
                        <template v-else-if="props.item.balancePrincipal">
                          {{ props.item.balancePrincipal }}
                          <v-btn 
                            class="bottomNavigationItem transparent"
                            title="Send funds"
                            flat
                            @click="openSendFundsModal($event, props.item, true)">
                            <v-icon>send</v-icon>
                          </v-btn>
                        </template>
                        <template v-else>-</template>
                      </td>
                      <td class="text-xs-right">
                        <v-card-title v-if="props.item.loadingBalance" primary-title class="pb-0">
                          <v-spacer />
                          <v-badge color="red" right title="A transaction is in progress">
                            <v-progress-circular color="primary" indeterminate size="20"></v-progress-circular>
                          </v-badge>
                        </v-card-title>
                        <template v-else>
                          {{ props.item.balance }}
                          <v-btn 
                            class="bottomNavigationItem transparent"
                            title="Send funds"
                            flat
                            @click="openSendFundsModal($event, props.item)">
                            <v-icon>send</v-icon>
                          </v-btn>
                        </template>
                      </td>
                    </tr>
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
                    is-read-only
                    is-display-only
                    @back="back()"/>
                </v-navigation-drawer>

                <send-funds-modal
                  ref="sendFundsModal"
                  :accounts-details="accountsDetails"
                  :overview-accounts="overviewAccounts"
                  :principal-account="principalAccount"
                  :refresh-index="refreshIndex"
                  :network-id="networkId"
                  :wallet-address="walletAddress"
                  no-button
                  @success="refreshBalance"
                  @pending="pendingTransaction"
                  @error="refreshBalance(null, null, $event)" />
              </v-card>
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import DeployNewContract from './DeployNewContract.vue';
import AddContractModal from './AddContractModal.vue';
import WalletAddress from './WalletAddress.vue';
import WalletSetup from './WalletSetup.vue';
import AccountDetail from './AccountDetail.vue';
import SendFundsModal from './SendFundsModal.vue';

import * as constants from '../WalletConstants.js';
import {searchSpaces, searchUsers} from '../WalletAddressRegistry.js';
import {getContractsDetails, removeContractAddressFromDefault, getContractDeploymentTransactionsInProgress, removeContractDeploymentTransactionsInProgress, saveContractAddress} from '../WalletToken.js';
import {initWeb3,initSettings, retrieveFiatExchangeRate, computeNetwork, getTransactionReceipt, watchTransactionStatus, gasToFiat, getWallets, computeBalance} from '../WalletUtils.js';

export default {
  components: {
    DeployNewContract,
    AddContractModal,
    WalletAddress,
    AccountDetail,
    SendFundsModal,
    WalletSetup
  },
  data () {
    return {
      isWalletEnabled: false,
      loading: false,
      loadingSettings: false,
      loadingContracts: false,
      loadingWallets: false,
      selectedTab: true,
      sameConfiguredNetwork: true,
      fundsHolder: '',
      fundsHolderOptions: [],
      fundsHolderSearchTerm: null,
      accessPermission: '',
      accessPermissionOptions: [],
      accessPermissionSearchTerm: null,
      isLoadingSuggestions: false,
      defaultBlocksToRetrieve: 1000,
      defaultGas: 50000,
      defaultGasFiatPrice: 0,
      fiatSymbol: '$',
      refreshIndex: 1,
      walletAddress: null,
      networkId: null,
      newTokenAddress: null,
      showAddContractModal: false,
      selectedOverviewAccounts: [],
      selectedPrincipalAccount: null,
      enableDelegation: true,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      selectedWalletAddress: null,
      selectedWalletDetails: null,
      mandatoryRule: [
        (v) => !!v || 'Field is required'
      ],
      headers: [
        {
          text: 'Token name',
          align: 'left',
          sortable: false,
          value: 'name'
        },
        {
          text: 'Contract address',
          align: 'center',
          sortable: false,
          value: 'address'
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'action'
        }
      ],
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
      ],
      initialFundsHeaders: [
        {
          text: 'Name',
          align: 'left',
          sortable: false,
          value: 'name'
        },
        {
          text: 'Amount',
          align: 'center',
          sortable: false,
          value: 'amount'
        }
      ],
      selectedNetwork: {
        text: '',
        value: ''
      },
      networks: [
        {
          text: 'Ethereum Main Network',
          value: 1,
          wsLink: 'wss://mainnet.infura.io/ws',
          httpLink: 'https://mainnet.infura.io'
        },
        {
          text: 'Ropsten',
          value: 3,
          wsLink: 'wss://ropsten.infura.io/ws',
          httpLink: 'https://ropsten.infura.io'
        },
        {
          text: 'Other',
          value: 0,
          wsLink: 'ws://127.0.0.1:8546',
          httpLink: 'http://127.0.0.1:8545'
        }
      ],
      etherAccount: {text: 'Ether', value: 'ether', disabled: false},
      fiatAccount: {text: 'Fiat', value: 'fiat', disabled: false},
      contracts: [],
      wallets: []
    };
  },
  computed: {
    displaySettings() {
      return !this.loading && !this.loadingSettings;
    },
    principalAccount() {
      if (this.selectedPrincipalAccount && this.selectedPrincipalAccount.value) {
        return this.selectedPrincipalAccount.value === 'ether' ? this.walletAddress : this.selectedPrincipalAccount.value;
      } else {
        return this.walletAddress;
      }
    },
    overviewAccounts() {
      const overviewAccounts = [];
      if (this.selectedOverviewAccounts && this.selectedOverviewAccounts.length) {
        // Add contracts addresses
        this.selectedOverviewAccounts.forEach(account => {
          if (account.value && account.value !== 'ether' && account.value !== this.walletAddress) {
            overviewAccounts.push(account.value);
          }
        });
      }
      // Add ether to the comobobox list options
      overviewAccounts.push(this.walletAddress);
      return overviewAccounts;
    },
    accountsDetails() {
      const accountsDetails = {};
      if (this.contracts && this.contracts.length) {
        this.contracts.forEach(contract => {
          accountsDetails[contract.address] = contract;
        });
      }
      const currentUserWallet = this.wallets.find(wallet => wallet.address === this.walletAddress);
      if (currentUserWallet) {
        accountsDetails[this.walletAddress] = {
          title : 'ether',
          icon : 'warning',
          balance : currentUserWallet.balance,
          symbol : 'ether',
          isContract : false,
          address : this.walletAddress
        };
      }
      return accountsDetails;
    },
    showSpecificNetworkFields() {
      return this.selectedNetwork && this.selectedNetwork.value !== 1 && this.selectedNetwork.value !== 3;
    },
    initialFunds() {
      let initialFunds = [];
      if (!window.walletSettings.initialFunds) {
        initialFunds = [{name : 'ether', address : 'ether', amount : 0}];
        if(this.contracts) {
          this.contracts.forEach(contract => {
            initialFunds.push({name : contract.name, address : contract.address,  amount : 0});
          });
        }
      } else {
        // Add newly added contracts
        this.contracts.forEach(contract => {
          const contractDetails = window.walletSettings.initialFunds.find(tmpContract => tmpContract.address === contract.address);
          if(!contractDetails) {
            window.walletSettings.initialFunds.push({name : contract.name, address : contract.address,  amount : 0});
          } else if(contractDetails.address === 'ether') {
            contractDetails.name = 'ether';
          } else  {
            contractDetails.name = contract.name;
          }
        });

        window.walletSettings.initialFunds.forEach(contract => {
          const contractDetails = this.contracts.find(tmpContract => tmpContract.address === contract.address);
          if (contractDetails || contract.address === 'ether') {
            initialFunds.push(contract);
          }
        });
      }
      return initialFunds;
    },
    accountsList() {
      const accountsList = [];
      accountsList.push(Object.assign({}, this.etherAccount), Object.assign({}, this.fiatAccount));
      if (this.contracts) {
        this.contracts.forEach(contract => {
          accountsList.push({text: contract.name, value: contract.address, disabled: false});
        });
      }
      return accountsList;
    }
  },
  watch: {
    accessPermissionSearchTerm() {
      this.isLoadingSuggestions = true;
      searchSpaces(this.accessPermissionSearchTerm)
        .then(items => {
          if (items) {
            this.accessPermissionOptions = items;
          } else {
            this.accessPermissionOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug("searchSpaces method - error", e);
          this.isLoadingSuggestions = false;
        });
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
    },
    fundsHolderSearchTerm() {
      if (!this.fundsHolderSearchTerm || !this.fundsHolderSearchTerm.length) {
        return;
      }
      this.isLoadingSuggestions = true;
      searchUsers(this.fundsHolderSearchTerm, true)
        .then(items => {
          if (items) {
            this.fundsHolderOptions = items;
          } else {
            this.fundsHolderOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug("searchUsers method - error", e);
          this.isLoadingSuggestions = false;
        });
    },
    selectedPrincipalAccount() {
      if (this.selectedPrincipalAccount) {
        this.selectedOverviewAccounts.forEach(account => {
          account.disabled = false;
        });

        this.accountsList.forEach((account, index) => {
          if (this.selectedPrincipalAccount.value === account.value) {
            account.disabled = true;
            const accountIndex = this.selectedOverviewAccounts.findIndex(foundSelectedAccount => foundSelectedAccount.value === account.value);
            if (accountIndex >= 0) {
              this.selectedOverviewAccounts.splice(accountIndex, 1);
            }
            this.selectedOverviewAccounts.unshift(account);
          } else {
            account.disabled = false;
          }
        });
        this.forceUpdate();
      }
    },
    accessPermission(newValue, oldValue) {
      if (oldValue) {
        this.accessPermissionSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.accessPermissionAutoComplete.isFocused = false;
      }
    },
    fundsHolder(newValue, oldValue) {
      if (oldValue) {
        this.fundsHolderSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.fundsHolderAutoComplete.isFocused = false;
      }
    },
    defaultGas() {
      this.defaultGasFiatPrice = gasToFiat(this.defaultGas);
    }
  },
  created() {
    this.init()
      .then(() => this.loadWallets());
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
          if (!window.walletSettings || !window.walletSettings.isWalletEnabled) {
            this.isWalletEnabled = false;
            this.forceUpdate();
            throw new Error("Wallet disabled for current user");
          } else {
            this.isWalletEnabled = true;
          }
        })
        .then(() => ignoreContracts || initWeb3())
        .catch(error => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug("Error connecting to network", error);
            this.error = "Error connecting to network";
          } else {
            throw error;
          }
        })
        .then(account => {
          this.walletAddress = window.localWeb3.eth.defaultAccount && window.localWeb3.eth.defaultAccount.toLowerCase();
          this.networkId = window.walletSettings.currentNetworkId;
        })
        .then(this.setDefaultValues)
        .then(() => this.loadingSettings = false)
        .then(() => ignoreContracts || this.refreshContractsList())
        .then(this.setSelectedValues)
        .catch(error => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug("Error retrieving contracts", error);
            if (!this.error) {
              this.error = "Error retrieving contracts";
            }
          }
        })
        .then(() => {
          this.loadingContracts = this.loading = false;
          this.forceUpdate();
        })
        .catch(e => {
          console.debug("init method - error", e);
          this.loadingContracts = this.loadingSettings = this.loadingWallets = this.loading = false;
          this.loadingSettings = false;
          this.error = `Error encountered: ${e}`;
        });
    },
    openSendFundsModal(event, wallet, principal) {
      event.preventDefault();
      event.stopPropagation();

      if (wallet && wallet.address && wallet.type) {
        if (principal) {
          if(this.selectedPrincipalAccount && this.selectedPrincipalAccount.value) {
            const principalInitialFund = this.initialFunds.find(account => account.address === this.selectedPrincipalAccount.value);
            this.$refs.sendFundsModal.prepareSendForm(wallet.id, wallet.type, principalInitialFund && principalInitialFund.amount, this.selectedPrincipalAccount.value);
          } else {
            console.error("No selected principal account found");
          }
        } else {
          const etherInitialFund = this.initialFunds.find(account => account.address === 'ether');
          this.$refs.sendFundsModal.prepareSendForm(wallet.id, wallet.type, etherInitialFund && etherInitialFund.amount);
        }
      }
    },
    pendingTransaction(transaction) {
      const recipient = transaction.to.toLowerCase();
      const wallet = this.wallets.find(wallet => wallet && wallet.address && wallet.address === recipient);
      if (transaction.contractAddress) {
        this.$set(wallet, "loadingBalancePrincipal", true);
      } else {
        this.$set(wallet, "loadingBalance", true);
      }
    },
    refreshBalance(accountDetails, address, error) {
      if (error) {
        console.debug("Error while proceeding transaction", error);
        return;
      }
      const wallet = this.wallets.find(wallet => wallet && wallet.address && wallet.address === address);
      if (wallet) {
        this.computeBalance(accountDetails, wallet);
      }
    },
    loadWallets() {
      this.loadingWallets = true;
      getWallets()
        .then(wallets => this.wallets = wallets)
        .then(() => {
          let principalContract = null;
          if (this.selectedPrincipalAccount && this.selectedPrincipalAccount.value && this.selectedPrincipalAccount.value !== 'ether') {
            principalContract = this.contracts.find(contract => contract.isContract && contract.address === this.selectedPrincipalAccount.value);
          }
          this.wallets.forEach(wallet => {
            if(wallet && wallet.address) {
              this.$set(wallet, "loadingBalance", true);
              this.$set(wallet, "loadingBalancePrincipal", true);
              this.computeBalance(principalContract, wallet);
            }
          });
        })
        .finally(() => {
          this.loadingWallets = false;
        });
    },
    computeBalance(accountDetails, wallet) {
      computeBalance(wallet.address)
        .then((balanceDetails, error) => {
          if (error) {
            this.$set(wallet, 'icon', 'warning');
            this.$set(wallet, 'error', `Error retrieving balance of wallet: ${error}`);
          } else {
            this.$set(wallet, 'balance', balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0);
            this.$set(wallet, 'balanceFiat', balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0);
          }
          this.$set(wallet, "loadingBalance", false);
          this.$forceUpdate();
        });
      if (accountDetails && accountDetails.contract && accountDetails.isContract) {
        accountDetails.contract.methods.balanceOf(wallet.address).call()
          .then((balance, error) => {
            if (error) {
              this.$set(wallet, "loadingBalancePrincipal", false);
              throw new Error('Invalid contract address');
            }
            balance = String(balance);
            this.$set(wallet, 'balancePrincipal', balance);
            this.$set(wallet, "loadingBalancePrincipal", false);
          });
      } else {
        this.$set(wallet, "loadingBalancePrincipal", false);
      }
    },
    setSelectedValues() {
      const selectedOverviewAccountsValues = window.walletSettings.defaultOverviewAccounts;
      selectedOverviewAccountsValues.forEach(selectedValue => {
        const selectedObject = this.getOverviewAccountObject(selectedValue);
        if (selectedObject) {
          this.selectedOverviewAccounts.push(selectedObject);
        }
      });

      this.selectedPrincipalAccount = this.getOverviewAccountObject(window.walletSettings.defaultPrincipalAccount);
    },
    openAccountDetail(accountDetails) {
      this.selectedWalletAddress = accountDetails.address;
      this.computeWalletDetails(accountDetails);
      this.seeAccountDetails = true;

      this.$nextTick(() => {
        const thiss = this;
        $('.v-overlay').on('click', event => {
          thiss.back();
        });
      });
    },
    computeWalletDetails(accountDetails) {
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
        balance : accountDetails.balance,
        balanceFiat : accountDetails.balanceFiat
      };
    },
    back() {
      this.seeAccountDetails = false;
      this.seeAccountDetailsPermanent = false;
      this.selectedWalletAddress = null;
      this.selectedWalletDetails = null;
    },
    getOverviewAccountObject(selectedValue) {
      if (selectedValue === 'fiat') {
        return Object.assign({}, this.fiatAccount);
      } else if (selectedValue === 'ether') {
        return Object.assign({}, this.etherAccount);
      } else if(this.contracts && this.contracts.length) {
        const selectedContractAddress = this.contracts.findIndex(contract => contract.address === selectedValue);
        if (selectedContractAddress >= 0) {
          const contract = this.contracts[selectedContractAddress];
          if (!contract.error) {
            return {text: contract.name, value: contract.address, disabled: false};
          }
        }
      }
    },
    setDefaultValues() {
      if (window.walletSettings.fundsHolder) {
        this.fundsHolder = window.walletSettings.fundsHolder;
        searchUsers(this.fundsHolder, true)
          .then(items => {
            if (items) {
              this.fundsHolderOptions = items;
            } else {
              this.fundsHolderOptions = [];
            }
          });
      }
      if (window.walletSettings.accessPermission) {
        this.accessPermission = window.walletSettings.accessPermission;
        searchSpaces(this.accessPermission)
          .then(items => {
            if (items) {
              this.accessPermissionOptions = items;
            } else {
              this.accessPermissionOptions = [];
            }
            if (!this.accessPermissionOptions.find(item => item.id === this.accessPermission)) {
              this.accessPermissionOptions.push({id : this.accessPermission, name : this.accessPermission, error : true});
            }
          });
      }
      if (window.walletSettings.defaultNetworkId === 1) {
        this.selectedNetwork = this.networks[0];
      } else if (window.walletSettings.defaultNetworkId === 3) {
        this.selectedNetwork = this.networks[1];
      } else {
        this.networks[2].value = window.walletSettings.defaultNetworkId;
        this.networks[2].wsLink = window.walletSettings.websocketProviderURL;
        this.networks[2].httpLink = window.walletSettings.providerURL;
        this.selectedNetwork = this.networks[2];
      }
      if (window.walletSettings.defaultBlocksToRetrieve) {
        this.defaultBlocksToRetrieve = window.walletSettings.defaultBlocksToRetrieve;
      }
      if (window.walletSettings.defaultGas) {
        this.defaultGas = window.walletSettings.defaultGas;
      }
      this.fiatSymbol = (window.walletSettings && window.walletSettings.fiatSymbol) || '$';
      this.sameConfiguredNetwork = String(this.networkId) === String(this.selectedNetwork.value);
      this.enableDelegation = window.walletSettings.enableDelegation;
      this.initialFundsRequestMessage = window.walletSettings.initialFundsRequestMessage;
    },
    updateList(address) {
      this.loading = true;
      if (address) {
        this.newTokenAddress = address;
      }
      this.refreshContractsList()
        .then(() => this.loading = false)
        .catch(e => {
          console.debug("refreshContractsList method - error", e);
          this.loading = false;
          this.error = `Error encountered: ${e}`;
        });
    },
    refreshContractsList() {
      return getContractsDetails(this.walletAddress, this.networkId, true)
        .then(contracts => this.contracts = contracts ? contracts.filter(contract => contract.isDefault) : [])
        .then(() => getContractDeploymentTransactionsInProgress(this.networkId))
        .then(contractsInProgress => {
          Object.keys(contractsInProgress).forEach(hash => {
            const contractInProgress = contractsInProgress[hash];
            getTransactionReceipt(contractInProgress.hash)
              .then(receipt => {
                if (!receipt) {
                  // pending transaction
                  this.contracts.push({
                    name: contractInProgress.name,
                    hash: contractInProgress.hash,
                    address: 'Transaction in progress...',
                    isPending: true
                  });
                  watchTransactionStatus(contractInProgress.hash, () => {
                    this.refreshContractsList();
                  });
                } else if(receipt.status && receipt.contractAddress) {
                  const contractAddress = receipt.contractAddress.toLowerCase();
                  // success transaction
                  // Add contract as default if not yet present
                  if (contractInProgress.isDefault && !this.contracts.find(contract => contract.address === contractAddress)) {
                    // This may happen when the contract is already added in //
                    if (window.walletSettings.defaultContractsToDisplay.indexOf(contractAddress)) {
                      this.newTokenAddress = contractAddress;
                      removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                      this.contractsModified();
                    } else {
                      // Save newly created contract as default
                      return saveContractAddress(this.walletAddress, contractAddress, this.networkId, contractInProgress.isDefault)
                        .then((added, error) => {
                          if (error) {
                            throw error;
                          }
                          if (added) {
                            this.newTokenAddress = contractAddress;
                            removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                            this.contractsModified();
                          } else {
                            this.error = `Address ${contractAddress} is not recognized as ERC20 Token contract's address`;
                          }
                          this.loading = false;
                        })
                        .catch(err => {
                          console.debug("saveContractAddress method - error", err);
                          this.loading = false;
                          this.error = `${err}`;
                        });
                    }
                  } else {
                    // The contract was already saved
                    removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                  }
                } else {
                  // failed transaction
                  this.contracts.push({
                    name: contractInProgress.name,
                    hash: contractInProgress.hash,
                    address: '',
                    error: `Transaction failed on contract ${contractInProgress.name}`
                  });
                }
              });
          });
        });
    },
    saveGlobalSettings() {
      this.loading = true;
      this.loadingSettings = true;
      const initialFundsMap = {};
      if (this.initialFunds && this.initialFunds.length) {
        this.initialFunds.forEach(initialFund => {
          initialFundsMap[initialFund.address] = initialFund.amount;
        });
      }
      const reloadContract = window.walletSettings.defaultNetworkId !== this.selectedNetwork.value;

      fetch('/portal/rest/wallet/api/global-settings/save', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          accessPermission: this.accessPermission,
          fundsHolder: this.fundsHolder,
          fundsHolderType: 'user',
          initialFundsRequestMessage: this.initialFundsRequestMessage,
          providerURL: this.selectedNetwork.httpLink,
          websocketProviderURL: this.selectedNetwork.wsLink,
          defaultBlocksToRetrieve: this.defaultBlocksToRetrieve,
          defaultNetworkId: this.selectedNetwork.value,
          defaultPrincipalAccount: this.selectedPrincipalAccount && this.selectedPrincipalAccount.value,
          defaultOverviewAccounts: this.selectedOverviewAccounts && this.selectedOverviewAccounts.map(item => item.value),
          defaultGas: this.defaultGas,
          enableDelegation: this.enableDelegation,
          initialFunds: initialFundsMap
        })
      })
        .then(resp => {
          if (resp && resp.ok) {
            window.walletSettings.defaultNetworkId = this.selectedNetwork.value;
            window.walletSettings.providerURL = this.selectedNetwork.httpLink;
            window.walletSettings.websocketProviderURL = this.selectedNetwork.wsLink;
            window.walletSettings.accessPermission = this.accessPermission;
            window.walletSettings.fundsHolder = this.fundsHolder;
            window.walletSettings.defaultBlocksToRetrieve = this.defaultBlocksToRetrieve;
            window.walletSettings.defaultGas = this.defaultGas;
            this.sameConfiguredNetwork = String(this.networkId) === String(this.selectedNetwork.value);
  
            this.loadingSettings = this.loading = false;
          } else {
            this.error = 'Error saving global settings';
          }
        })
        .then(this.init(!reloadContract))
        .catch(e => {
          this.loading = false;
          console.debug("fetch global-settings - error", e);
          this.error = 'Error saving global settings';
        });
    },
    contractsModified() {
      this.refreshContractsList()
        .then(() => this.loading = false)
        .catch(e => {
          console.debug("refreshContractsList method - error", e);
          this.loading = false;
          this.error = `Error adding new contract address: ${e}`;
        });
    },
    deleteContract(item, event) {
      if (!item || !item.address) {
        this.error = 'Contract doesn\'t have an address';
      }
      this.loading = true;
      if (item.hash) {
        removeContractDeploymentTransactionsInProgress(this.networkId, item.hash);
        this.contractsModified();
      } else {
        removeContractAddressFromDefault(item.address)
          .then((resp, error) => {
            if (error) {
              this.error = 'Error deleting contract as default';
            } else {
              return this.refreshContractsList();
            }
          })
          .then(() => this.loading = false)
          .catch(e => {
            console.debug("removeContractAddressFromDefault method - error", e);
            this.loading = false;
            this.error = 'Error deleting contract as default';
          });
      }
      event.preventDefault();
      event.stopPropagation();
    },
    forceUpdate() {
      this.refreshIndex++;
      this.$forceUpdate();
    }
  }
};
</script>