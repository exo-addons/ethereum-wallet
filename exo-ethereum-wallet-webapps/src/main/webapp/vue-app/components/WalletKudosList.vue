<template>
  <v-card flat>
    <v-card-title class="text-xs-center">
      <div v-if="error && String(error).trim() != '{}'" class="alert alert-error v-content">
        <i class="uiIconError"></i>
        {{ error }}
      </div>
    </v-card-title>
    <v-card-text class="text-xs-center">
      <div id="tokenRatioAmount">
        <v-text-field
          v-model.number="tokenPerKudo"
          :readonly="!isEditingRatio"
          label="Tokens per Kudo"
          name="tokenPerKudo"
          placeholder="Select an amount of tokens per kudo" />
        <v-slide-x-reverse-transition
          slot="append-outer"
          mode="out-in">
          <v-icon
            :color="isEditingRatio ? 'success' : 'info'"
            :key="`icon-${isEditingRatio}`"
            @click="saveTokensPerKudos"
            v-text="isEditingRatio ? 'fa-check-circle' : 'fa-edit'" />
        </v-slide-x-reverse-transition>
      </div>
    
      <v-menu
        ref="selectedDateMenu"
        v-model="selectedDateMenu"
        transition="scale-transition"
        lazy
        offset-y
        class="kudosDateSelector">
        <v-text-field
          slot="activator"
          v-model="periodDatesDisplay"
          label="Select the period date"
          prepend-icon="event" />
        <v-date-picker
          v-model="selectedDate"
          @input="selectedDateMenu = false" />
      </v-menu>
      <v-data-table
        v-model="selectedKudosIdentitiesList"
        :headers="kudosIdentitiesHeaders"
        :items="kudosIdentitiesList"
        :loading="loading"
        :sortable="true"
        :pagination.sync="pagination"
        select-all
        item-key="idType"
        class="elevation-1 mr-3 mb-2"
        hide-actions>
        <template slot="headers" slot-scope="props">
          <tr>
            <th>
              <v-checkbox
                :input-value="props.all"
                :indeterminate="props.indeterminate"
                primary
                hide-details
                @click.native="toggleAll" />
            </th>
            <th
              v-for="header in props.headers"
              :key="header.text"
              :class="['column sortable', pagination.descending ? 'desc' : 'asc', header.value === pagination.sortBy ? 'active' : '']"
              @click="changeSort(header.value)">
              <v-icon v-if="pagination.descending" small>arrow_upward</v-icon>
              <v-icon v-else small>arrow_downward</v-icon>
              {{ header.text }}
            </th>
          </tr>
        </template>
        <template slot="items" slot-scope="props">
          <tr :active="props.selected">
            <td>
              <v-checkbox
                v-if="props.item.address && props.item.received"
                :input-value="props.selected"
                hide-details
                @click="props.selected = !props.selected" />
            </td>
            <td>
              <v-avatar size="36px">
                <img
                  :src="props.item.avatar"
                  onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
              </v-avatar>
            </td>
            <td class="text-xs-left">
              <a v-if="props.item.address" :href="props.item.url" rel="nofollow" target="_blank">{{ props.item.name }}</a>
              <div v-else>
                <del>
                  <a :href="props.item.url" rel="nofollow" target="_blank" class="red--text">{{ props.item.name }}</a>
                </del>
                (No address)
              </div>
            </td>
            <td v-html="props.item.address">
            </td>
            <td>
              <v-text-field v-if="props.item.address && props.item.received" v-model="props.item.tokensToSend"/>
              <template v-else>-</template>
            </td>
            <td v-html="props.item.received">
            </td>
            <td v-html="props.item.sent">
            </td>
          </tr>
        </template>
      </v-data-table>
      <send-kudos-modal
        :account="walletAddress"
        :contract-details="contractDetails"
        :recipients="recipients"
        @sent="$emit('pending', $event)"
        @error="error = $event" />
    </v-card-text>
  </v-card>
</template>

<script>
import {getTokensPerKudos, saveTokensPerKudos} from '../WalletExtServices.js';

import SendKudosModal from './SendKudosModal.vue';

export default {
  components: {
    SendKudosModal
  },
  props: {
    wallets: {
      type: Array,
      default: function() {
        return [];
      }
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      isEditingRatio: false,
      tokenPerKudo: null,
      defaultTokenPerKudos: null,
      selectedDate: null,
      selectedDateMenu: false,
      kudosPeriodType: null,
      kudosIdentitiesList: [],
      selectedKudosIdentitiesList: [],
      selectedStartDate: null,
      selectedEndDate: null,
      pagination: {
        descending: true,
        sortBy: 'received'
      },
      kudosIdentitiesHeaders: [
        {
          text: '',
          align: 'right',
          sortable: false,
          value: 'avatar',
          width: '36px'
        },
        {
          text: 'Name',
          align: 'left',
          sortable: false,
          value: 'name'
        },
        {
          text: 'Address',
          align: 'center',
          sortable: true,
          value: 'address'
        },
        {
          text: 'Tokens to send',
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
          width: '70px'
        },
        {
          text: 'Received',
          align: 'center',
          sortable: true,
          value: 'received'
        },
        {
          text: 'Sent',
          align: 'center',
          sortable: true,
          value: 'sent'
        }
      ]
    };
  },
  computed: {
    periodDatesDisplay() {
      if(this.selectedStartDate && this.selectedEndDate) {
        return `${this.selectedStartDate} to ${this.selectedEndDate}`;
      } else if(this.selectedStartDate) {
        return this.selectedStartDate;
      } else {
        return '';
      }
    },
    recipients() {
      return this.selectedKudosIdentitiesList ? this.selectedKudosIdentitiesList.filter(item => item.address && item.received && item.tokensToSend) : [];
    },
    displayButton() {
      return this.contractDetails && this.kudosIdentitiesList && this.kudosIdentitiesList.length;
    }
  },
  watch: {
    selectedDate() {
      this.loadAll();
    },
    defaultTokenPerKudos() {
      this.refreshList();
    },
    kudosPeriodType() {
      this.loadAll();
    }
  },
  created() {
    document.addEventListener('exo-kudos-get-kudos-list-loading', () => this.loading = true);
    document.addEventListener('exo-kudos-get-kudos-list-result', this.loadKudosList);
    this.$nextTick(() => {
      this.kudosPeriodType = window.kudosSettings.kudosPeriodType;
      this.selectedDate = new Date().toISOString().substr(0, 10);
      document.addEventListener('exo-kudos-get-period-result', this.loadPeriodDates);
    });
    getTokensPerKudos().then(value => this.defaultTokenPerKudos = this.tokenPerKudo = value);
  },
  methods: {
    saveTokensPerKudos() {
      if(this.isEditingRatio) {
        saveTokensPerKudos(this.tokenPerKudo)
          .then(() => {
            this.defaultTokenPerKudos = this.tokenPerKudo;
            this.isEditingRatio = false;
          })
          .catch(error => {
            this.error = "Error while saving 'Token per Kudos' parameter";
            this.isEditingRatio = true;
          });
      } else {
        this.isEditingRatio = true;
      }
    },
    loadAll() {
      if (!this.selectedDate || !this.kudosPeriodType) {
        return;
      }
      this.selectedStartDate = this.formatDate(new Date(this.selectedDate));
      this.selectedEndDate = null;
      document.dispatchEvent(new CustomEvent('exo-kudos-get-period', {'detail' : {'date' : new Date(this.selectedDate), 'periodType': this.kudosPeriodType}}));
    },
    loadPeriodDates(event) {
      if(event && event.detail && event.detail.period) {
        this.selectedStartDate = this.formatDate(new Date(event.detail.period.startDateInSeconds * 1000));
        this.selectedEndDate = this.formatDate(new Date(event.detail.period.endDateInSeconds * 1000));
        document.dispatchEvent(new CustomEvent('exo-kudos-get-kudos-list', {'detail' : {'startDate' : new Date(this.selectedStartDate), 'endDate' : new Date(this.selectedEndDate)}}));
      } else {
        console.debug("Retrieved event detail doesn't have the period as result");
      }
    },
    loadKudosList(event) {
      this.$emit("list-retrieved");
      this.error = null;
      this.kudosIdentitiesList = [];
      if(!event || !event.detail) {
        this.error = 'Empty kudos list is retrieved';
      } else if(event.detail.error) {
        console.debug(event.detail.error);
        this.error = event.detail.error;
      } else if(event.detail.list) {
        const list = event.detail.list;
        list.forEach(element => element.idType = `${element.type}_${element.id}`);
        this.kudosIdentitiesList = list;
        this.refreshList();
      } else {
        this.error = 'Empty kudos list is retrieved';
      }
      this.loading = false;
    },
    refreshList() {
      this.kudosIdentitiesList.forEach(walletKudos => {
        const wallet = this.wallets && this.wallets.find(wallet => wallet && wallet.id && wallet.id === walletKudos.id && wallet.type === walletKudos.type);
        if(wallet && wallet.address) {
          this.$set(walletKudos, "address", wallet.address);
          if (this.defaultTokenPerKudos) {
            let result = Number(walletKudos.received) * 100000 * Number(this.defaultTokenPerKudos) / 100000;
            // attempt to simply avoid floatting point problem
            const resultString = String(result);
            if(resultString.length - resultString.indexOf(".") > 5) {
              result = Number(walletKudos.received) * 1000000 * Number(this.defaultTokenPerKudos) / 1000000;
            }
            this.$set(walletKudos, "tokensToSend", result);
          }
        }
      });
    },
    formatDate (date) {
      if (!date){
        return null;
      }
      const dateString = date.toString();
      // Example: 'Feb 01 2018'
      return dateString.substring(dateString.indexOf(' ') + 1, dateString.indexOf(":") - 3);
    },
    toggleAll() {
      if (this.selectedKudosIdentitiesList.length === this.kudosIdentitiesList.length){
        this.selectedKudosIdentitiesList = [];
      } else {
        this.selectedKudosIdentitiesList = this.kudosIdentitiesList.slice();
      }
    },
    changeSort(column) {
      if (this.pagination.sortBy === column) {
        this.pagination.descending = !this.pagination.descending;
      } else {
        this.pagination.sortBy = column;
        this.pagination.descending = false;
      }
    }
  }
};
</script>