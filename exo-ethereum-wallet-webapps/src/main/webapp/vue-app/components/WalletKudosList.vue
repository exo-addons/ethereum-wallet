<template>
  <v-card flat>
    <v-card-title class="text-xs-center">
      <div v-if="error && String(error).trim() != '{}'" class="alert alert-error v-content">
        <i class="uiIconError"></i>
        {{ error }}
      </div>
    </v-card-title>
    <h3 class="text-xs-left ml-3">Configuration</h3>
    <v-card-text>
      <div class="text-xs-left kudosWalletConfiguration">
        <v-text-field
          v-model.number="tokenPerKudo"
          label="Tokens per Kudo"
          name="tokenPerKudo" />
        <span> per kudos using </span>
        <v-combobox
          v-model="selectedKudosContractAddress"
          :items="contracts"
          :return-object="false"
          label="Token type"
          item-value="address"
          item-text="name"
          hide-no-data
          hide-selected
          small-chips>
          <template slot="selection" slot-scope="data">
            <v-chip
              :selected="data.selected"
              :disabled="data.disabled"
              :key="data.value"
              @input="data.parent.selectItem(data.item)">
              {{ selectedKudosContractName }}
            </v-chip>
          </template>
        </v-combobox>
        <button class="btn btn-primary mb-3" @click="save">
          Save
        </button>
      </div>
    </v-card-text>
    <h3 class="text-xs-left ml-3">Send Rewards</h3>
    <v-card-text class="text-xs-center">
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
                v-if="props.item.address && props.item.received && (!props.item.status || props.item.status === 'error')"
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
                v-if="transactionEtherscanLink && props.item.txHash"
                :href="`${transactionEtherscanLink}${props.item.txHash}`"
                target="_blank" title="Open on etherscan">
                Etherscan link
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
              <v-text-field v-if="props.item.address && props.item.received && (!props.item.status || props.item.status === 'error')" v-model="props.item.tokensToSend"/>
              <template v-else-if="props.item.status === 'success'">{{ props.item.tokensSent }}</template>
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
        :period-type="kudosPeriodType"
        :start-date-in-seconds="selectedStartDateInSeconds"
        @sent="newPendingTransaction"
        @error="error = $event" />
    </v-card-text>
  </v-card>
</template>

<script>
import {getKudosContract, getTokensPerKudos, saveKudosContract, saveTokensPerKudos, getPeriodTransactions} from '../WalletExtServices.js';
import {watchTransactionStatus, getTokenEtherscanlink, getAddressEtherscanlink, getTransactionEtherscanlink} from '../WalletUtils.js';

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
      tokenPerKudo: null,
      defaultTokenPerKudos: null,
      selectedDate: null,
      selectedDateMenu: false,
      selectedKudosContractAddress: null,
      tokenEtherscanLink: null,
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
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
          text: 'Transaction',
          align: 'center',
          sortable: true,
          value: 'txHash'
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
    selectedKudosContractName() {
      return this.contractDetails && this.contractDetails.name;
    },
    contractDetails() {
      return this.selectedKudosContractAddress && this.contracts && this.contracts.find(contract => contract.address === this.selectedKudosContractAddress);
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
      return this.selectedKudosIdentitiesList ? this.selectedKudosIdentitiesList.filter(item => item.address && item.received && item.tokensToSend && !item.tokensSent && (!item.status || item.status === 'error')) : [];
    },
    selectableRecipients() {
      return this.kudosIdentitiesList ? this.kudosIdentitiesList.filter(item => item.address && item.received && !item.tokensSent && (!item.status || item.status === 'error')) : [];
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
    getKudosContract().then(kudosContract => this.selectedKudosContractAddress = kudosContract || this.principalAccount);
    this.tokenEtherscanLink = getTokenEtherscanlink(window.walletSettings.defaultNetworkId);
    this.transactionEtherscanLink = getTransactionEtherscanlink(window.walletSettings.defaultNetworkId);
    this.addressEtherscanLink = getAddressEtherscanlink(window.walletSettings.defaultNetworkId);
  },
  methods: {
    newPendingTransaction(transaction) {
      this.$emit('pending', transaction);
      if(!transaction || !transaction.to) {
        return;
      }
      this.refreshKudosWalletStatus(transaction);
    },
    refreshKudosWalletStatus(transaction) {
      if(transaction && transaction.to) {
        const kudosWalletIndex = this.selectedKudosIdentitiesList.findIndex(kudosWallet => kudosWallet.address && kudosWallet.address.toLowerCase() === transaction.to.toLowerCase());
        const kudosWallet = this.selectedKudosIdentitiesList[kudosWalletIndex];
        if(kudosWallet) {
          this.$set(kudosWallet, 'txHash', transaction.hash);
          this.$set(kudosWallet, 'status', 'pending');
          if (transaction.contractAmount) {
            this.$set(kudosWallet, 'tokensSent', transaction.contractAmount);
          }
          this.selectedKudosIdentitiesList.splice(kudosWalletIndex, 1);
          const thiss = this;
          watchTransactionStatus(transaction.hash, receipt => {
            thiss.$set(kudosWallet, 'status', receipt.status ? 'success' : 'error');
          });
        }
      } else {
        getPeriodTransactions(window.walletSettings.defaultNetworkId, this.kudosPeriodType, this.selectedStartDateInSeconds)
          .then(kudosTransactions => {
            if (kudosTransactions) {
              kudosTransactions.forEach(kudosTransaction => {
                if(kudosTransaction) {
                  const kudosWallet = this.kudosIdentitiesList.find(kudosWallet => kudosWallet.id === kudosTransaction.receiverId && kudosWallet.type === kudosTransaction.receiverType);
                  this.$set(kudosWallet, 'tokensSent', kudosTransaction.tokensAmountSent ? Number(kudosTransaction.tokensAmountSent) : 0);
                  this.$set(kudosWallet, 'txHash', kudosTransaction.hash);
                  this.$set(kudosWallet, 'status', 'pending');

                  const thiss = this;
                  watchTransactionStatus(kudosTransaction.hash, receipt => {
                    thiss.$set(kudosWallet, 'status', receipt.status ? 'success' : 'error');
                  });
                }
              });
              this.$forceUpdate();
            }
          });
      }
    },
    save() {
      this.saveKudosContract().then(() => this.saveTokensPerKudos());
    },
    saveKudosContract() {
      return saveKudosContract(this.selectedKudosContractAddress)
        .catch(error => {
          this.error = "Error while saving 'Kudos contract address' parameter";
        });
    },
    saveTokensPerKudos() {
      return saveTokensPerKudos(this.tokenPerKudo)
        .then(() => {
          this.defaultTokenPerKudos = this.tokenPerKudo;
        })
        .catch(error => {
          this.error = "Error while saving 'Token per Kudos' parameter";
        });
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
        this.refreshKudosWalletStatus();
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