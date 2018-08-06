<template>
  <v-app id="WalletAdminApp">
    <main>
      <v-layout row>
        <v-flex xs12 sm6 offset-sm3>
          <v-toolbar dark color="primary">
            <v-toolbar-title>Wallet application administration</v-toolbar-title>
          </v-toolbar>
          <v-alert :value="error" type="error" class="v-content">
            {{ error }}
          </v-alert>
          <v-card>
            <v-subheader>Default contracts</v-subheader>
            <div class="text-xs-center">
              <v-progress-circular v-show="loading" indeterminate color="primary"></v-progress-circular>
            </div>
            <v-divider />
            <v-data-table :headers="headers" :items="contracts" :sortable="false" class="elevation-1" hide-actions>
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
      errorMessage: '',
      account: null,
      networkId: null,
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
        .then(initWeb3)
        .then(account => this.account = window.localWeb3.eth.defaultAccount)
        .then(() => window.localWeb3.eth.net.getId())
        .then(netId => this.networkId = netId)
        .then(retrieveUSDExchangeRate)
        .then(this.refreshContractsList)
        .catch(e => {
          this.loading = false;
          this.errorMessage = `Error encountered: ${e}`;
        });
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
    deleteContract(item, event) {
      this.loading = true;
      fetch('/portal/rest/wallet/api/contract/remove', {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          address: item.address
        })
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