<template>
  <v-card class="text-xs-center pr-3 pl-3 pt-2" flat>
    <v-card-title> The following settings manages the funds holder and the amount of initial funds to send for a user that has created a new wallet for the first time. You can choose to set initial funds for a token to 0 so that no funds will be send. The funds holder will receive a notification per user per currency (ether and/or token). </v-card-title>
    <v-card-text>
      <v-flex id="fundsHolderAutoComplete" class="contactAutoComplete">
        <v-autocomplete v-if="sameConfiguredNetwork" ref="fundsHolderAutoComplete" v-model="fundsHolder" :items="fundsHolderOptions" :loading="isLoadingSuggestions" :search-input.sync="fundsHolderSearchTerm" attach="#fundsHolderAutoComplete" label="Wallet funds holder" class="contactAutoComplete" placeholder="Start typing to Search a user" content-class="contactAutoCompleteContent" max-width="100%" item-text="name" item-value="id" hide-details hide-selected chips cache-items dense flat>
          <template slot="no-data">
            <v-list-tile>
              <v-list-tile-title> Search for a <strong>user</strong> </v-list-tile-title>
            </v-list-tile>
          </template>

          <template slot="selection" slot-scope="{item, selected}">
            <v-chip :selected="selected" class="autocompleteSelectedItem">
              <span>{{ item.name }}</span>
            </v-chip>
          </template>

          <template slot="item" slot-scope="{item}">
            <v-list-tile-avatar v-if="item.avatar" tile size="20"> <img :src="item.avatar" /> </v-list-tile-avatar>
            <v-list-tile-title v-text="item.name" />
          </template>
        </v-autocomplete>
      </v-flex>

      <v-textarea id="initialFundsRequestMessage" v-model="initialFundsRequestMessage" name="initialFundsRequestMessage" label="Initial funds request message" placeholder="You can enter a custom message to send with initial funds request" class="mt-4" rows="7" flat no-resize />
    </v-card-text>

    <v-card-text class="text-xs-left">
      <v-label light>Default amount of automatic initial funds request</v-label>
      <v-data-table :headers="initialFundsHeaders" :items="initialFunds" :sortable="false" hide-actions>
        <template slot="items" slot-scope="props">
          <td class="text-xs-left">{{ props.item.name ? props.item.name : props.item.address }}</td>
          <td><v-text-field v-model="props.item.amount" single-line /></td>
        </template>
      </v-data-table>
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <button class="btn btn-primary mb-3" @click="save">Save</button>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>
<script>
import {searchUsers} from '../WalletAddressRegistry.js';

export default {
  props: {
    defaultNetworkId: {
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
    loading: {
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
  },
  data() {
    return {
      fundsHolder: '',
      fundsHolderOptions: [],
      fundsHolderSearchTerm: null,
      isLoadingSuggestions: false,
      initialFunds: [],
      initialFundsHeaders: [
        {
          text: 'Name',
          align: 'left',
          sortable: false,
          value: 'name',
        },
        {
          text: 'Amount',
          align: 'center',
          sortable: false,
          value: 'amount',
        },
      ],
    };
  },
  watch: {
    contracts() {
      this.reloadInitialFunds();
    },
    initialFunds() {
      this.$emit('initial-funds-loaded', this.initialFunds);
    },
    fundsHolderSearchTerm() {
      if (!this.fundsHolderSearchTerm || !this.fundsHolderSearchTerm.length) {
        return;
      }
      this.isLoadingSuggestions = true;
      searchUsers(this.fundsHolderSearchTerm, true)
        .then((items) => {
          if (items) {
            this.fundsHolderOptions = items;
          } else {
            this.fundsHolderOptions = [];
          }
          this.isLoadingSuggestions = false;
        })
        .catch((e) => {
          console.debug('searchUsers method - error', e);
          this.isLoadingSuggestions = false;
        });
    },
    fundsHolder(newValue, oldValue) {
      if (oldValue) {
        this.fundsHolderSearchTerm = null;
        // A hack to close on select
        // See https://www.reddit.com/r/vuetifyjs/comments/819h8u/how_to_close_a_multiple_autocomplete_vselect/
        this.$refs.fundsHolderAutoComplete.isFocused = false;
      }
    },
  },
  methods: {
    init() {
      if (window.walletSettings.fundsHolder) {
        this.fundsHolder = window.walletSettings.fundsHolder;
        searchUsers(this.fundsHolder, true).then((items) => {
          if (items) {
            this.fundsHolderOptions = items;
          } else {
            this.fundsHolderOptions = [];
          }
        });
      }
      this.initialFundsRequestMessage = window.walletSettings.initialFundsRequestMessage;
      this.reloadInitialFunds();
    },
    reloadInitialFunds() {
      if (!window.walletSettings) {
        return [];
      }
      this.initialFunds = [];
      if (!window.walletSettings.initialFunds) {
        this.initialFunds = [{name: 'ether', address: 'ether', amount: 0}];
        if (this.contracts) {
          this.contracts.forEach((contract) => {
            this.initialFunds.push({name: contract.name, address: contract.address, amount: 0});
          });
        }
      } else {
        // Add newly added contracts
        this.contracts.forEach((contract) => {
          const contractDetails = window.walletSettings.initialFunds.find((tmpContract) => tmpContract.address === contract.address);
          if (!contractDetails) {
            window.walletSettings.initialFunds.push({name: contract.name, address: contract.address, amount: 0});
          } else if (contractDetails.address === 'ether') {
            contractDetails.name = 'ether';
          } else {
            contractDetails.name = contract.name;
          }
        });

        window.walletSettings.initialFunds.forEach((contract) => {
          const contractDetails = this.contracts.find((tmpContract) => tmpContract.address === contract.address);
          if (contractDetails || contract.address === 'ether') {
            this.initialFunds.push(contract);
          }
        });
      }
    },
    save() {
      const initialFundsMap = {};
      if (this.initialFunds && this.initialFunds.length) {
        this.initialFunds.forEach((initialFund) => {
          initialFundsMap[initialFund.address] = initialFund.amount;
        });
      }
      const globalSettings = {
        fundsHolder: this.fundsHolder,
        fundsHolderType: 'user',
        initialFundsRequestMessage: this.initialFundsRequestMessage,
        initialFunds: initialFundsMap,
      };
      this.$emit('save', globalSettings);
    },
  },
};
</script>
