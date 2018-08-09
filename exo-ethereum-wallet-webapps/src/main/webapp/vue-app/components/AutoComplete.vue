<template>
  <v-autocomplete ref="selectAutoComplete"
                  v-model="selectedValue"
                  :items="items"
                  :loading="isLoadingSuggestions"
                  :search-input.sync="searchTerm"
                  :label="inputLabel"
                  :open-on-click="false"
                  :open-on-hover="false"
                  :open-on-clear="false"
                  :no-filter="true"
                  max-width="100%"
                  item-text="name"
                  item-value="id_type"
                  hide-details hide-selected chips clearable cache-items>

    <template slot="no-data">
      <v-list-tile>
        <v-list-tile-title>
          Search for a <strong>Wallet</strong> of space or user
        </v-list-tile-title>
      </v-list-tile>
    </template>

    <template slot="selection" slot-scope="{ item, selected }">
      <v-chip v-if="item.avatar" :selected="selected" :disabled="addressLoad === 'error'" :class="addressLoad === 'error' ? 'red--text':''" :title="addressLoad === 'error' ? 'the recipient doesn\'t have a valid wallet account yet' : ''" color="blue-grey" class="white--text" @input="selectItem(item)">
        <v-progress-circular v-if="addressLoad === 'loading'" indeterminate color="white" class="mr-2"></v-progress-circular>
        <v-icon v-else-if="addressLoad === 'error'" alt="Invalid address" class="mr-2" color="red">warning</v-icon>
        <v-avatar v-else-if="item.avatar" dark>
          <img :src="item.avatar">
        </v-avatar>
        <span>{{ item.name }}</span>
      </v-chip>
      <v-label v-else :selected="selected" class="black--text" solo @input="selectItem(item)">
        {{ item.name }}
      </v-label>
    </template>

    <template slot="item" slot-scope="{ item, tile }">
      <v-list-tile-avatar v-if="item.avatar">
        <img :src="item.avatar">
      </v-list-tile-avatar>
      <v-list-tile-content>
        <v-list-tile-title v-text="item.name"></v-list-tile-title>
        <v-list-tile-sub-title v-text="item.id"></v-list-tile-sub-title>
      </v-list-tile-content>
    </template>
  </v-autocomplete>
</template>

<script>
import {searchAddress, searchContact} from '../WalletAddressRegistry.js';

export default {
  props: {
    inputLabel: {
      type: String,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      items: [],
      selectedValue: null,
      searchTerm: null,
      address: null,
      isLoadingSuggestions: false,
      addressLoad: '',
      error: null
    };
  },
  watch: {
    searchTerm(value) {
      if(window.localWeb3.utils.isAddress(value)) {
        this.items = [{
          address: value,
          name: value,
          id: value
        }];
      } else if (value && value.length) {
        this.isLoadingSuggestions = true;
        try {
          searchContact(value)
            .then(data => {
              this.items = data;
            });
        } finally {
          this.isLoadingSuggestions = false;
        }
      } else {
        this.items = [];
      }
    },
    selectedValue() {
      this.$refs.selectAutoComplete.isFocused = false;
      this.addressLoad = 'loading';
      if (this.selectedValue) {
        const isAddress = this.selectedValue.indexOf('_') < 0;
        const type = isAddress ? null : this.selectedValue.substring(0, this.selectedValue.indexOf('_'));
        const id = isAddress ? this.selectedValue : this.selectedValue.substring(this.selectedValue.indexOf('_') + 1);
        if (isAddress) {
          this.addressLoad = 'success';
          this.$emit("item-selected", {id: id, type: null, address: id});
        } else {
          searchAddress(id, type)
            .then( address => {
              if (address && address.length) {
                this.addressLoad = 'success';
                this.$emit("item-selected", {id: id, type: type, address: address});
              } else {
                this.addressLoad = 'error';
              }
            })
            .catch(error => {
              this.addressLoad = 'error';
            });
        }
      }
    }
  }
};
</script>

