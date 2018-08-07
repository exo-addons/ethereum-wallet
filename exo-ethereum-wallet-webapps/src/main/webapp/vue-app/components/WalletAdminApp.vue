<template>
  <v-app id="WalletAdminApp" color="transaprent">
    <main>
      <v-layout class="mr-3 ml-3">
        <v-flex>
          <v-alert :value="error" type="error" class="v-content">
            {{ error }}
          </v-alert>
          <v-card class="text-xs-center pt-3">
            <v-form class="mr-3 ml-3">
              <v-text-field
                v-model="providerURL"
                :rules="mandatoryRule"
                type="text"
                name="providerURL"
                label="Ethereum Network URL" />
              <v-text-field
                v-model="accessPermission"
                type="text"
                name="accessPermission"
                label="Wallet access permission" />
              <v-slider
                v-model="defaultGas"
                :label="`Default Gas for transactions: ${defaultGas}`"
                :min="21000"
                :max="100000"
                :step="1000"
                type="number"
                required />
              <v-slider
                v-model="defaultBlocksToRetrieve"
                :label="`Default blocks to retrieve for ether transactions: ${defaultBlocksToRetrieve}`"
                :min="100"
                :max="10000"
                :step="100"
                type="number"
                required />
              <v-text-field
                v-model="defaultNetworkId"
                :rules="mandatoryRule"
                :label="`Ethereum Network ID (current id: ${networkId})`"
                type="text"
                name="defaultNetworkId" />

              <v-btn color="primary" @click="saveGlobalSettings">
                Save
              </v-btn>
            </v-form>
          </v-card>
          <v-card>
            <v-subheader class="text-xs-center">Default contracts</v-subheader>
            <div class="text-xs-center">
              <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
            </div>
            <v-divider />
            <v-data-table :headers="headers" :items="contracts" :sortable="false" class="elevation-1 mr-3 ml-3" hide-actions>
              <template slot="items" slot-scope="props">
                <td>{{ props.item.name }}</td>
                <td class="text-xs-right">{{ props.item.address }}</td>
                <td class="text-xs-right">
                  <v-btn icon ripple @click="deleteContract(props.item, $event)">
                    <v-icon color="primary">delete</v-icon>
                  </v-btn>
                </td>
              </template>
            </v-data-table>
            <v-divider />
            <deploy-new-contract v-show="!error" :account="account" :network-id="networkId" @list-updated="refreshContractsList"/>
          </v-card>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import DeployNewContract from './DeployNewContract.vue';

import {getContractsDetails, deleteContractFromStorage} from '../WalletToken.js';
import {initWeb3,initSettings,retrieveUSDExchangeRate} from '../WalletUtils.js';

export default {
  components: {
    DeployNewContract
  },
  data () {
    return {
      loading: false,
      accessPermission: '',
      providerURL: 'http://localhost:8545',
      defaultBlocksToRetrieve: 1000,
      defaultNetworkId: 0,
      defaultGas: 50000,
      account: null,
      networkId: null,
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
    }
  },
  created() {
    this.init()
      .then(this.refreshContractsList);
  },
  methods: {
    init() {
      this.loading = true;
      return initSettings()
        .then(this.setDefaultValues)
        .then(initWeb3)
        .then(account => this.account = window.localWeb3.eth.defaultAccount)
        .then(() => window.localWeb3.eth.net.getId())
        .then(netId => this.networkId = netId)
        .then(netId => this.defaultNetworkId = this.defaultNetworkId ? this.defaultNetworkId : netId)
        .then(retrieveUSDExchangeRate)
        .then(this.refreshContractsList)
        .catch(e => {
          this.loading = false;
          this.errorMessage = `Error encountered: ${e}`;
        });
    },
    setDefaultValues() {
      if (window.walletSettings.accessPermission) {
        this.accessPermission = window.walletSettings.accessPermission;
      }
      if (window.walletSettings.providerURL) {
        this.providerURL = window.walletSettings.providerURL;
      }
      if (window.walletSettings.defaultBlocksToRetrieve) {
        this.defaultBlocksToRetrieve = window.walletSettings.defaultBlocksToRetrieve;
      }
      if (window.walletSettings.defaultNetworkId) {
        this.defaultNetworkId = window.walletSettings.defaultNetworkId;
      }
      if (window.walletSettings.defaultGas) {
        this.defaultGas = window.walletSettings.defaultGas;
      }
    },
    refreshContractsList() {
      this.loading = true;
      getContractsDetails(this.account, this.networkId)
        .then(contracts => this.contracts = contracts ? contracts.filter(contract => contract.isDefault) : [])
        .then(() => this.loading = false)
        .catch(e => {
          this.loading = false;
          this.errorMessage = `Error encountered: ${e}`;
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
          providerURL: this.providerURL,
          defaultBlocksToRetrieve: this.defaultBlocksToRetrieve,
          defaultNetworkId: this.defaultNetworkId,
          defaultGas: this.defaultGas
        })
      }).then(resp => {
        if (resp && resp.ok) {
          window.walletSettings.accessPermission = this.accessPermission;
          window.walletSettings.providerURL = this.providerURL;
          window.walletSettings.defaultBlocksToRetrieve = this.defaultBlocksToRetrieve;
          window.walletSettings.defaultNetworkId = this.defaultNetworkId;
          window.walletSettings.defaultGas = this.defaultGas;
        } else {
          this.errorMessage = 'Error saving global settings';
        }
        this.loading = false;
      }).catch(e => {
        this.errorMessage = 'Error saving global settings';
      });
    },
    deleteContract(item, event) {
      this.loading = true;
      fetch('/portal/rest/wallet/api/contract/remove', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: `address=${item.address}`
      }).then(resp => {
        if (resp && resp.ok) {
          window.walletSettings.defaultContractsToDisplay.splice(window.walletSettings.defaultContractsToDisplay.indexOf(item.address), 1);
          this.refreshContractsList();
        } else {
          this.errorMessage = 'Error deleting contract as default';
        }
        this.loading = false;
      }).catch(e => {
        this.errorMessage = 'Error deleting contract as default';
      });
      event.preventDefault();
      event.stopPropagation();
    }
  }
};
</script>