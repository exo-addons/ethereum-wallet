<template>
  <v-card class="text-xs-center pr-3 pl-3 pt-2" flat>
    <v-flex id="selectedNetworkParent" class="selectBoxVuetifyParent">
      <v-combobox
        v-model="selectedNetwork"
        :items="networks"
        attach="#selectedNetworkParent"
        label="Select ethereum network" />
    </v-flex>

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
              Search for a <strong>
                Space
              </strong>
            </v-list-tile-title>
          </v-list-tile>
        </template>

        <template slot="selection" slot-scope="{item, selected}">
          <v-chip
            v-if="item.error"
            :selected="selected"
            class="autocompleteSelectedItem">
            <del>
              <span>
                {{ item.name }}
              </span>
            </del>
          </v-chip>
          <v-chip
            v-else
            :selected="selected"
            class="autocompleteSelectedItem">
            <span>
              {{ item.name }}
            </span>
          </v-chip>
        </template>

        <template slot="item" slot-scope="{item}">
          <v-list-tile-avatar
            v-if="item.avatar"
            tile
            size="20">
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
      <button class="btn btn-primary mb-3" @click="save">
        Save
      </button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
import {searchSpaces} from '../../WalletAddressRegistry.js';

export default {
  props: {
    networkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultNetworkId: {
      type: String,
      default: function() {
        return null;
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    etherBalance: {
      type: String,
      default: function() {
        return null;
      },
    },
    sameConfiguredNetwork: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    contracts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    loading: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
    loadingSettings: {
      type: Boolean,
      default: function() {
        return false;
      },
    },
  },
  data() {
    return {
      accessPermission: '',
      accessPermissionOptions: [],
      accessPermissionSearchTerm: null,
      isLoadingSuggestions: false,
      selectedOverviewAccounts: [],
      selectedPrincipalAccount: null,
      etherAccount: {text: 'Ether', value: 'ether', disabled: false},
      fiatAccount: {text: 'Fiat', value: 'fiat', disabled: false},
      mandatoryRule: [(v) => !!v || 'Field is required'],
      selectedNetwork: {
        text: '',
        value: '',
      },
      networks: [
        {
          text: 'Ethereum Main Network',
          value: 1,
          wsLink: 'wss://mainnet.infura.io/ws',
          httpLink: 'https://mainnet.infura.io',
        },
        {
          text: 'Ropsten',
          value: 3,
          wsLink: 'wss://ropsten.infura.io/ws',
          httpLink: 'https://ropsten.infura.io',
        },
        {
          text: 'Other',
          value: 0,
          wsLink: 'ws://127.0.0.1:8546',
          httpLink: 'http://127.0.0.1:8545',
        },
      ],
    };
  },
  computed: {
    showSpecificNetworkFields() {
      return this.selectedNetwork && this.selectedNetwork.value !== 1 && this.selectedNetwork.value !== 3;
    },
    accountsList() {
      const accountsList = [];
      accountsList.push(Object.assign({}, this.etherAccount), Object.assign({}, this.fiatAccount));
      if (this.contracts) {
        this.contracts.forEach((contract) => {
          accountsList.push({text: contract.name, value: contract.address, disabled: false});
        });
      }
      return accountsList;
    },
    principalContract() {
      let principalContract = null;
      if (this.selectedPrincipalAccount && this.selectedPrincipalAccount.value && this.selectedPrincipalAccount.value !== 'ether') {
        principalContract = this.contracts.find((contract) => contract.isContract && contract.address === this.selectedPrincipalAccount.value);
      }
      return principalContract;
    },
    principalAccountAddress() {
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
        this.selectedOverviewAccounts.forEach((account) => {
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
        this.contracts.forEach((contract) => {
          accountsDetails[contract.address] = contract;
        });
      }
      accountsDetails[this.walletAddress] = {
        title: 'ether',
        icon: 'ether',
        balance: this.etherBalance,
        symbol: 'ether',
        isContract: false,
        address: this.walletAddress,
      };
      return accountsDetails;
    },
  },
  watch: {
    principalContract(value) {
      this.$emit('principal-contract-loaded', value);
    },
    selectedPrincipalAccount() {
      if (this.selectedPrincipalAccount) {
        this.selectedOverviewAccounts.forEach((account) => {
          this.$set(account, 'disabled', false);
        });

        this.accountsList.forEach((account, index) => {
          if (this.selectedPrincipalAccount.value === account.value) {
            this.$set(account, 'disabled', true);
            const accountIndex = this.selectedOverviewAccounts.findIndex((foundSelectedAccount) => foundSelectedAccount.value === account.value);
            if (accountIndex >= 0) {
              this.selectedOverviewAccounts.splice(accountIndex, 1);
            }
            this.selectedOverviewAccounts.unshift(account);
          } else {
            this.$set(account, 'disabled', false);
          }
        });
        this.$forceUpdate();
      }
      this.$emit('principal-account-loaded', this.selectedPrincipalAccount);
    },
    principalAccountAddress(value) {
      this.$emit('principal-account-address-loaded', value);
    },
    overviewAccounts(value) {
      this.$emit('overview-accounts-loaded', value);
    },
    accountsDetails(value) {
      this.$emit('accounts-details-loaded', value);
    },
    accessPermission(newValue, oldValue) {
      if (oldValue) {
        this.accessPermissionSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.accessPermissionAutoComplete.isFocused = false;
      }
    },
    accessPermissionSearchTerm() {
      this.isLoadingSuggestions = true;
      searchSpaces(this.accessPermissionSearchTerm)
        .then((items) => {
          if (items) {
            this.accessPermissionOptions = items;
          } else {
            this.accessPermissionOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug('searchSpaces method - error', e);
          this.isLoadingSuggestions = false;
        });
    },
  },
  methods: {
    init() {
      if (window.walletSettings.accessPermission) {
        this.accessPermission = window.walletSettings.accessPermission;
        searchSpaces(this.accessPermission).then((items) => {
          if (items) {
            this.accessPermissionOptions = items;
          } else {
            this.accessPermissionOptions = [];
          }
          if (!this.accessPermissionOptions.find((item) => item.id === this.accessPermission)) {
            this.accessPermissionOptions.push({id: this.accessPermission, name: this.accessPermission, error: true});
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
    },
    setSelectedValues() {
      this.selectedOverviewAccounts = [];
      const selectedOverviewAccountsValues = window.walletSettings.defaultOverviewAccounts;
      if (selectedOverviewAccountsValues) {
        selectedOverviewAccountsValues.forEach((selectedValue) => {
          const selectedObject = this.getOverviewAccountObject(selectedValue);
          if (selectedObject) {
            this.selectedOverviewAccounts.push(selectedObject);
          }
        });
      }
      this.selectedPrincipalAccount = this.getOverviewAccountObject(window.walletSettings.defaultPrincipalAccount);
    },
    getOverviewAccountObject(selectedValue) {
      if (selectedValue === 'fiat') {
        return Object.assign({}, this.fiatAccount);
      } else if (selectedValue === 'ether') {
        return Object.assign({}, this.etherAccount);
      } else if (this.contracts && this.contracts.length) {
        const selectedContractAddress = this.contracts.findIndex((contract) => contract.address === selectedValue);
        if (selectedContractAddress >= 0) {
          const contract = this.contracts[selectedContractAddress];
          if (!contract.error) {
            return {text: contract.name, value: contract.address, disabled: false};
          }
        }
      }
    },
    save() {
      const globalSettings = {
        accessPermission: this.accessPermission,
        providerURL: this.selectedNetwork.httpLink,
        websocketProviderURL: this.selectedNetwork.wsLink,
        defaultNetworkId: this.selectedNetwork.value,
        defaultPrincipalAccount: this.selectedPrincipalAccount && this.selectedPrincipalAccount.value,
        defaultOverviewAccounts: this.selectedOverviewAccounts && this.selectedOverviewAccounts.map((item) => item.value),
      };
      this.$emit('save', globalSettings);
    },
  },
};
</script>
