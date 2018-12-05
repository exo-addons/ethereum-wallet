<template>
  <v-card flat>
    <v-card-title class="text-xs-center">
      <div v-if="error && String(error).trim() != '{}'" class="alert alert-error v-content">
        <i class="uiIconError"></i>
        {{ error }}
      </div>
    </v-card-title>
    <h3 class="text-xs-left ml-3 ">Configuration</h3>
    <v-card-text class="text-xs-left">
      <div class="text-xs-left gamificationWalletConfiguration">
        <span>Users will be rewarded starting from </span>
        <v-text-field
          v-model.number="selectedThreshold"
          name="threshold"
          class="input-text-center" />
        <span> gamification points per </span>
        <div id="selectedPeriodType" class="selectBoxVuetifyParent v-input">
          <v-combobox
            v-model="selectedPeriodType"
            :items="periods"
            :return-object="false"
            attach="#selectedPeriodType"
            label="Period type"
            class="selectBoxVuetify"
            hide-no-data
            hide-selected
            small-chips>
            <template slot="selection" slot-scope="data">
              {{ selectedPeriodName }}
            </template>
          </v-combobox>
        </div>
        <span> using contract </span>
        <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
          <v-combobox
            v-model="selectedContractAddress"
            :items="contracts"
            :return-object="false"
            attach="#selectedContractAddress"
            item-value="address"
            item-text="name"
            class="selectBoxVuetify"
            hide-no-data
            hide-selected
            small-chips>
            <template slot="selection" slot-scope="data">
              {{ selectedContractName }}
            </template>
          </v-combobox>
        </div>
        <button class="btn btn-primary mb-3" @click="save">
          Save
        </button>
      </div>
    </v-card-text>
    <v-card-text class="text-xs-center">
      <div v-if="isContractDifferentFromPrincipal" class="alert alert-warning">
        <i class="uiIconWarning"></i>
        You have chosen a token that is different from principal displayed token
      </div>
    </v-card-text>
    <gamification-teams
      :wallets="wallets"/>
    <h3 class="text-xs-left ml-3">Send Rewards</h3>
    <v-card-text class="text-xs-center">
      <v-menu
        ref="selectedDateMenu"
        v-model="selectedDateMenu"
        transition="scale-transition"
        lazy
        offset-y
        class="gamificationDateSelector">
        <v-text-field
          slot="activator"
          v-model="periodDatesDisplay"
          label="Select the period date"
          prepend-icon="event" />
        <v-date-picker
          v-model="selectedDate"
          :first-day-of-week="1"
          :type="!periodType || periodType === 'WEEK' ? 'date' : 'month'"
          @input="selectedDateMenu = false" />
      </v-menu>
      <v-data-table
        v-model="selectedIdentitiesList"
        :headers="identitiesHeaders"
        :items="identitiesList"
        :loading="loading"
        :sortable="true"
        :pagination.sync="pagination"
        select-all
        item-key="address"
        class="elevation-1 mr-3 mb-2"
        hide-actions>
        <template slot="headers" slot-scope="props">
          <tr>
            <th v-if="selectableRecipients.length">
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
              :class="['column sortable', header.value === pagination.sortBy ? 'active' : '', header.align === 'center' ? 'text-xs-center' : header.align === 'right' ? 'text-xs-right' : 'text-xs-left']"
              @click="changeSort(header.value)">
              <template v-if="header.value === pagination.sortBy">
                <v-icon small>{{ paginationIcon }}</v-icon>
              </template>
              {{ header.text }}
            </th>
          </tr>
        </template>
        <template slot="items" slot-scope="props">
          <tr :active="props.selected">
            <td v-if="selectableRecipients.length">
              <v-checkbox
                v-if="props.item.address && props.item.points && props.item.points > threshold && (!props.item.status || props.item.status === 'error')"
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
            <td>
              <a
                v-if="addressEtherscanLink"
                :href="`${addressEtherscanLink}${props.item.address}`"
                target="_blank"
                title="Open on etherscan">{{ props.item.address }}</a>
              <span v-else>{{ props.item.address }}</span>
            </td>
            <td>
              <a
                v-if="props.item.hash"
                href="javascript:void(0);"
                target="_blank"
                title="Open transaction in wallet"
                @click="$emit('open-wallet-transaction', props.item)">
                Open in wallet
              </a>
              <span v-else>-</span>
            </td>
            <td>
              <div v-if="!props.item.status">-</div>
              <v-progress-circular
                v-else-if="props.item.status === 'pending'"
                color="primary"
                indeterminate
                size="20" />
              <v-icon
                v-else
                :color="props.item.status === 'success' ? 'success' : 'error'"
                :title="props.item.status === 'success' ? 'Successfully proceeded' : props.item.status === 'pending' ? 'Transaction in progress' : 'Transaction error'"
                v-text="props.item.status === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'" />
            </td>
            <td>
              <v-text-field v-if="props.item.address && props.item.points && props.item.points > threshold && (!props.item.status || props.item.status === 'error')" v-model="props.item.tokensToSend" class="input-text-center"/>
              <template v-else-if="props.item.status === 'success'">{{ toFixed(props.item.tokensSent) }}</template>
              <template v-else>-</template>
            </td>
            <td v-html="props.item.points">
            </td>
          </tr>
        </template>
      </v-data-table>
    </v-card-text>
  </v-card>
</template>

<script>
import GamificationTeams from "./WalletAdminGamificationTeams.vue";

import {getSettings, saveSettings, getPeriodTransactions, getPeriodDates, getGamificationPoints} from '../WalletGamificationServices.js';
import {watchTransactionStatus, getTokenEtherscanlink, getAddressEtherscanlink, getTransactionEtherscanlink} from '../WalletUtils.js';

export default {
  components: {
    GamificationTeams
  },
  props: {
    wallets: {
      type: Array,
      default: function() {
        return [];
      }
    },
    contracts: {
      type: Array,
      default: function() {
        return [];
      }
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      selectedDate: new Date().toISOString().substr(0, 10),
      selectedDateMenu: false,
      contractAddress: null,
      threshold: null,
      periodType: null,
      selectedContractAddress: null,
      selectedThreshold: null,
      selectedPeriodType: null,
      tokenEtherscanLink: null,
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
      identitiesList: [],
      selectedIdentitiesList: [],
      selectedStartDate: null,
      selectedEndDate: null,
      pagination: {
        descending: true,
        sortBy: 'received'
      },
      periods: [
        {
          text: 'Week',
          value: 'WEEK'
        },
        {
          text: 'Month',
          value: 'MONTH'
        },
        {
          text: 'Quarter',
          value: 'QUARTER'
        },
        {
          text: 'Semester',
          value: 'SEMESTER'
        },
        {
          text: 'Year',
          value: 'YEAR'
        }
      ],
      identitiesHeaders: [
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
          text: 'Transaction',
          align: 'center',
          sortable: true,
          value: 'hash'
        },
        {
          text: 'Status',
          align: 'center',
          sortable: true,
          value: 'status'
        },
        {
          text: 'Tokens',
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
          width: '70px'
        },
        {
          text: 'Points',
          align: 'center',
          sortable: true,
          value: 'points'
        }
      ]
    };
  },
  computed: {
    selectedPeriodName() {
      const selectedPeriodName = this.periods.find(period => period.value === this.selectedPeriodType);
      return selectedPeriodName && selectedPeriodName.text ? selectedPeriodName.text : this.selectedPeriodType && (this.selectedPeriodType.text || this.selectedPeriodType);
    },
    selectedContractName() {
      return this.selectedContractDetails && this.selectedContractDetails.name;
    },
    isContractDifferentFromPrincipal() {
      return this.contractAddress && this.principalAccount && this.principalAccount.toLowerCase() !== this.contractAddress.toLowerCase();
    },
    selectedContractDetails() {
      return this.selectedContractAddress && this.contracts && this.contracts.find(contract => contract.address === this.selectedContractAddress);
    },
    contractDetails() {
      return this.contractAddress && this.contracts && this.contracts.find(contract => contract.address === this.contractAddress);
    },
    paginationDescending() {
      return this.pagination && this.pagination.descending;
    },
    paginationIcon() {
      return this.paginationDescending ? 'arrow_downward' : 'arrow_upward';
    },
    selectedStartDateInSeconds() {
      return this.selectedStartDate ? new Date(this.selectedStartDate).getTime() / 1000 : 0;
    },
    selectedEndDateInSeconds() {
      return this.selectedEndDate ? new Date(this.selectedEndDate).getTime() / 1000 : 0;
    },
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
      return this.selectedIdentitiesList ? this.selectedIdentitiesList.filter(item => item.address && item.points && item.points > this.threshold && item.tokensToSend && !item.tokensSent && (!item.status || item.status === 'error')) : [];
    },
    selectableRecipients() {
      return this.identitiesList ? this.identitiesList.filter(item => item.address && item.points > this.threshold && !item.tokensSent && (!item.status || item.status === 'error')) : [];
    }
  },
  watch: {
    selectedDate() {
      this.loadAll();
    },
    threshold() {
      this.refreshList();
    },
    periodType() {
      this.loadAll();
    }
  },
  created() {
    getSettings()
      .then(settings => {
        if (settings) {
          this.selectedContractAddress = this.contractAddress = settings.contractAddress;
          this.selectedThreshold = this.threshold = settings.threshold;
          this.selectedPeriodType = this.periodType = settings.periodType;
        }
      });
  },
  methods: {
    newPendingTransaction(transaction) {
      this.$emit('pending', transaction);
      if(!transaction || !transaction.to) {
        return;
      }
      this.refreshWalletTransactionStatus(transaction);
    },
    refreshWalletTransactionStatus(transaction) {
      if(transaction && transaction.to) {
        const resultWalletIndex = this.selectedIdentitiesList.findIndex(resultWallet => resultWallet.address && resultWallet.address.toLowerCase() === transaction.to.toLowerCase());
        const resultWallet = this.selectedIdentitiesList[resultWalletIndex];
        if(resultWallet) {
          this.$set(resultWallet, 'hash', transaction.hash);
          this.$set(resultWallet, 'status', 'pending');
          if (transaction.contractAmount) {
            this.$set(resultWallet, 'tokensSent', transaction.contractAmount);
          }
          this.selectedIdentitiesList.splice(resultWalletIndex, 1);
          const thiss = this;
          watchTransactionStatus(transaction.hash, receipt => {
            thiss.$set(resultWallet, 'status', receipt.status ? 'success' : 'error');
          });
        }
      } else {
        getPeriodTransactions(window.walletSettings.defaultNetworkId, this.periodType, this.selectedStartDateInSeconds)
          .then(resultTransactions => {
            if (resultTransactions) {
              resultTransactions.forEach(resultTransaction => {
                if(resultTransaction) {
                  const resultWallet = this.identitiesList.find(resultWallet => resultWallet.type === resultTransaction.receiverType && (resultWallet.id === resultTransaction.receiverId || resultWallet.identityId === resultTransaction.receiverIdentityId));
                  if(resultWallet) {
                    this.$set(resultWallet, 'tokensSent', resultTransaction.tokensAmountSent ? Number(resultTransaction.tokensAmountSent) : 0);
                    this.$set(resultWallet, 'hash', resultTransaction.hash);
                    this.$set(resultWallet, 'status', 'pending');
                    const thiss = this;
                    watchTransactionStatus(resultTransaction.hash, receipt => {
                      thiss.$set(resultWallet, 'status', receipt.status ? 'success' : 'error');
                    });
                  } else {
                    console.error("Can't find wallet of a sent result token transaction, resultTransaction=", resultTransaction);
                  }
                }
              });
              this.$forceUpdate();
            }
          });
      }
    },
    save() {
      return saveSettings({
        threshold: this.selectedThreshold,
        contractAddress : this.selectedContractAddress,
        periodType: this.selectedPeriodType && (this.selectedPeriodType.value || this.selectedPeriodType)
      })
        .then(() => {
          this.contractAddress = this.selectedContractAddress;
          this.periodType = this.selectedPeriodType;
          this.threshold = this.selectedThreshold;
        })
        .catch(error => {
          this.error = "Error while saving 'Gamification settings'";
        });
    },
    loadAll() {
      if (!this.selectedDate || !this.periodType) {
        return;
      }
      getPeriodDates(new Date(this.selectedDate), this.periodType)
        .then(period => {
          this.selectedStartDate = this.formatDate(new Date(period.startDateInSeconds * 1000));
          this.selectedEndDate = this.formatDate(new Date(period.endDateInSeconds * 1000));
          return this.loadGamificationList();
        });
    },
    loadGamificationList() {
      this.$emit("list-retrieved");
      this.error = null;
      this.identitiesList = [];
      this.loading = true;
      const identitiesListPromises = [];
      if(this.wallets && this.wallets.length) {
        const retrievedIdentities = [];
        this.wallets.forEach(wallet => {
          if(!wallet || wallet.type !== 'user' || retrievedIdentities.indexOf(wallet.id) >= 0) {
            return;
          }
          retrievedIdentities.push(wallet.id);
          wallet = Object.assign({}, wallet);
          const startDate = new Date(this.selectedStartDate);
          const endDate = new Date(this.selectedEndDate);
          identitiesListPromises.push(
            getGamificationPoints(wallet.id, startDate, endDate)
              .then(object => {
                wallet.points = object && object.points;
                this.identitiesList.push(wallet);
              })
          );
        });
      }
      Promise.all(identitiesListPromises)
        .then(() => this.loading = false);
    },
    refreshList() {
      this.identitiesList.forEach(walletResult => {
        /*
        Calculate Tokens to send per wallet

        let result = Number(walletResult.points) * 100000 * Number(this.defaultTokenPerResult) / 100000;
        // attempt to simply avoid floatting point problem
        const resultString = String(result);
        if(resultString.length - resultString.indexOf(".") > 5) {
          result = Number(walletResult.received) * 1000000 * Number(this.defaultTokenPerResult) / 1000000;
        }
        this.$set(walletResult, "tokensToSend", result);
        */
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
      if (this.selectedIdentitiesList.length === this.identitiesList.length){
        this.selectedIdentitiesList = [];
      } else {
        this.selectedIdentitiesList = this.identitiesList.slice();
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