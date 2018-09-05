<template>
  <v-app v-if="isWalletEnabled" id="WalletAdminApp" color="transaprent">
    <main>
      <v-layout>
        <v-flex class="white">
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>
          <div v-if="!sameConfiguredNetwork" class="alert alert-warning v-content">
            <i class="uiIconWarning"></i>
            Current selected network on Metamask is different from configured network to use with the platform.
            (The deployed contracts on default network aren't displayed)
          </div>
          <v-card class="text-xs-center pr-3 pl-3">
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

            <v-autocomplete
              ref="accessPermissionAutoComplete"
              v-model="accessPermission"
              :items="accessPermissionOptions"
              :loading="isLoadingSuggestions"
              :search-input.sync="accessPermissionSearchTerm"
              :open-on-click="false"
              :open-on-hover="false"
              :open-on-clear="false"
              :no-filter="true"
              counter="1"
              max-width="100%"
              item-text="name"
              item-value="id"
              label="Wallet access permission (Spaces only)"
              placeholder="Start typing to Search a space"
              hide-no-data hide-details hide-selected chips>

              <template slot="no-data">
                <v-list-tile>
                  <v-list-tile-title>
                    Search for a <strong>Space</strong>
                  </v-list-tile-title>
                </v-list-tile>
              </template>

              <template slot="selection" slot-scope="{ item, selected }">
                <v-chip :selected="selected" color="blue-grey" class="white--text">
                  <v-avatar dark>
                    <img :src="item.avatar">
                  </v-avatar>
                  <span>{{ item.name }}</span>
                </v-chip>
              </template>
          
              <template slot="item" slot-scope="{ item, tile }">
                <v-list-tile-avatar>
                  <img :src="item.avatar">
                </v-list-tile-avatar>
                <v-list-tile-content>
                  <v-list-tile-title v-text="item.name"></v-list-tile-title>
                </v-list-tile-content>
              </template>
            </v-autocomplete>

            <v-slider
              v-model="defaultGas"
              :label="`Default Gas for transactions: ${defaultGas} ${defaultGasFiatPrice ? '(' + defaultGasFiatPrice + ' ' + fiatSymbol + ')' : ''}`"
              :min="21000"
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
            <button class="btn btn-primary mb-3" @click="saveGlobalSettings">
              Save
            </button>
          </v-card>
          <v-card v-show="sameConfiguredNetwork">
            <v-divider />
            <v-subheader class="text-xs-center">Default contracts</v-subheader>
            <v-divider />
            <div class="text-xs-center">
              <v-progress-circular v-show="loading" indeterminate color="primary" />
            </div>

            <div v-if="newTokenAddress" class="alert alert-success v-content">
              <i class="uiIconSuccess"></i>
              Contract created under address: 
              <code>{{ newTokenAddress }}</code>
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
              <button class="btn btn-primary mt-3" @click="showAddContractModal = true">
                Add Existing contract Address
              </button>
              <deploy-new-contract 
                v-show="sameConfiguredNetwork"
                :account="account"
                :network-id="networkId"
                @list-updated="updateList($event)"/>
              <add-contract-modal
                :net-id="networkId"
                :account="account"
                :open="showAddContractModal"
                :is-default-contract="true"
                @added="contractsModified"
                @close="showAddContractModal = false" />
            </div>
          </v-card>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import DeployNewContract from './DeployNewContract.vue';
import AddContractModal from './AddContractModal.vue';

import {searchSpaces} from '../WalletAddressRegistry.js';
import {getContractsDetails, removeContractAddressFromDefault, getContractDeploymentTransactionsInProgress, removeContractDeploymentTransactionsInProgress, saveContractAddress} from '../WalletToken.js';
import {initWeb3,initSettings, retrieveFiatExchangeRate, computeNetwork, getTransactionReceipt, watchTransactionStatus, gasToFiat} from '../WalletUtils.js';

export default {
  components: {
    DeployNewContract,
    AddContractModal
  },
  data () {
    return {
      isWalletEnabled: false,
      loading: false,
      sameConfiguredNetwork: true,
      accessPermission: '',
      accessPermissionOptions: [],
      accessPermissionSearchTerm: null,
      isLoadingSuggestions: false,
      defaultBlocksToRetrieve: 1000,
      defaultGas: 50000,
      defaultGasFiatPrice: 0,
      fiatSymbol: '$',
      account: null,
      networkId: null,
      newTokenAddress: null,
      showAddContractModal: false,
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
      contracts: []
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
      } else if (!this.metamaskEnabled) {
        return 'Please install or Enable Metamask';
      } else if (!this.metamaskConnected) {
        return 'Please connect Metamask to a network';
      } else if (!this.account) {
        return 'Please select a valid account using Metamask';
      }
      return null;
    },
    showSpecificNetworkFields() {
      return this.selectedNetwork && this.selectedNetwork.value !== 1 && this.selectedNetwork.value !== 3;
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
    accessPermission(newValue, oldValue) {
      if (oldValue) {
        this.accessPermissionSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.accessPermissionAutoComplete.isFocused = false;
      }
    },
    defaultGas() {
      this.defaultGasFiatPrice = gasToFiat(this.defaultGas);
    }
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.loading = true;
      return initSettings()
        .then(() => {
          if (!window.walletSettings || !window.walletSettings.isWalletEnabled) {
            this.isWalletEnabled = false;
            this.$forceUpdate();
            throw new Error("Wallet disabled for current user");
          } else {
            this.$forceUpdate();
            this.isWalletEnabled = true;
          }
        })
        .then(initWeb3)
        .then(account => this.account = window.localWeb3.eth.defaultAccount)
        .then(computeNetwork)
        .then(netDetails => this.networkId = netDetails.netId)
        .then(() => retrieveFiatExchangeRate())
        .then(() => this.fiatSymbol = window.walletSettings ? window.walletSettings.fiatSymbol : '$')
        .then(this.setDefaultValues)
        .then(() => this.sameConfiguredNetwork = String(this.networkId) === String(this.selectedNetwork.value))
        .then(this.refreshContractsList)
        .then(() => this.loading = false)
        .catch(e => {
          console.debug("init method - error", e);
          this.loading = false;
          this.errorMessage = `Error encountered: ${e}`;
        });
    },
    setDefaultValues() {
      if (window.walletSettings.accessPermission) {
        this.accessPermission = window.walletSettings.accessPermission;
        searchSpaces(this.accessPermission)
          .then(items => {
            if (items) {
              this.accessPermissionOptions = items;
            } else {
              this.accessPermissionOptions = [];
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
          this.errorMessage = `Error encountered: ${e}`;
        });
    },
    refreshContractsList() {
      return getContractsDetails(this.account, this.networkId, true)
        .then(contracts => {
          return contracts;
        })
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
                      return saveContractAddress(this.account, contractAddress, this.networkId, contractInProgress.isDefault)
                        .then((added, error) => {
                          if (error) {
                            throw error;
                          }
                          if (added) {
                            this.newTokenAddress = contractAddress;
                            removeContractDeploymentTransactionsInProgress(this.networkId, contractInProgress.hash);
                            this.contractsModified();
                          } else {
                            this.errorMessage = `Address ${contractAddress} is not recognized as ERC20 Token contract's address`;
                          }
                          this.loading = false;
                        })
                        .catch(err => {
                          console.debug("saveContractAddress method - error", err);
                          this.loading = false;
                          this.errorMessage = `${err}`;
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
      fetch('/portal/rest/wallet/api/global-settings/save', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          accessPermission: this.accessPermission,
          providerURL: this.selectedNetwork.httpLink,
          websocketProviderURL: this.selectedNetwork.wsLink,
          defaultBlocksToRetrieve: this.defaultBlocksToRetrieve,
          defaultNetworkId: this.selectedNetwork.value,
          defaultGas: this.defaultGas
        })
      }).then(resp => {
        if (resp && resp.ok) {
          const reloadContract = window.walletSettings.defaultNetworkId !== this.selectedNetwork.value;

          window.walletSettings.defaultNetworkId = this.selectedNetwork.value;
          window.walletSettings.providerURL = this.selectedNetwork.httpLink;
          window.walletSettings.websocketProviderURL = this.selectedNetwork.wsLink;
          window.walletSettings.accessPermission = this.accessPermission;
          window.walletSettings.defaultBlocksToRetrieve = this.defaultBlocksToRetrieve;
          window.walletSettings.defaultGas = this.defaultGas;
          this.sameConfiguredNetwork = String(this.networkId) === String(this.selectedNetwork.value);

          if (reloadContract) {
            return this.contractsModified();
          }
        } else {
          this.errorMessage = 'Error saving global settings';
        }
        this.loading = false;
      }).catch(e => {
        console.debug("fetch global-settings - error", e);
        this.errorMessage = 'Error saving global settings';
      });
    },
    contractsModified() {
      this.refreshContractsList()
        .then(() => this.loading = false)
        .catch(e => {
          console.debug("refreshContractsList method - error", e);
          this.loading = false;
          this.errorMessage = `Error adding new contract address: ${e}`;
        });
    },
    deleteContract(item, event) {
      if (!item || !item.address) {
        this.errorMessage = 'Contract doesn\'t have an address';
      }
      this.loading = true;
      if (item.hash) {
        removeContractDeploymentTransactionsInProgress(this.networkId, item.hash);
        this.contractsModified();
      } else {
        removeContractAddressFromDefault(item.address)
          .then((resp, error) => {
            if (error) {
              this.errorMessage = 'Error deleting contract as default';
            } else {
              return this.refreshContractsList();
            }
          })
          .then(() => this.loading = false)
          .catch(e => {
            console.debug("removeContractAddressFromDefault method - error", e);
            this.loading = false;
            this.errorMessage = 'Error deleting contract as default';
          });
      }
      event.preventDefault();
      event.stopPropagation();
    }
  }
};
</script>