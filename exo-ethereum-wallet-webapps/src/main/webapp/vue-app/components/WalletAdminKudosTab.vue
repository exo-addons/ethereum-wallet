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
      <div class="text-xs-left kudosWalletConfiguration">
        <span>Kudos total budget is </span>
        <v-text-field
          v-model.number="kudosBudget"
          name="kudosBudget"
          class="input-text-center" />
        <div id="selectedKudosContractAddress" class="selectBoxVuetifyParent">
          <v-combobox
            v-model="selectedKudosContractAddress"
            :items="contracts"
            :return-object="false"
            attach="#selectedKudosContractAddress"
            item-value="address"
            item-text="name"
            class="selectedContractAddress"
            hide-no-data
            hide-selected
            small-chips>
            <template slot="selection" slot-scope="data">
              {{ selectedKudosContractName }}
            </template>
          </v-combobox>
        </div>
        <span> per kudos</span>
        <button class="btn btn-primary mb-3" @click="save">
          Save
        </button>
      </div>
    </v-card-text>
    <h3 class="text-xs-left ml-3">Send Rewards</h3>
    <div v-if="isContractDifferentFromPrincipal" class="alert alert-warning">
      <i class="uiIconWarning"></i>
      You have chosen a token that is different from principal displayed token
    </div>
    <v-card-text class="text-xs-center">
      <v-menu
        ref="selectedDateMenu"
        v-model="selectedDateMenu"
        transition="scale-transition"
        lazy
        offset-y
        class="dateSelector">
        <v-text-field
          slot="activator"
          v-model="periodDatesDisplay"
          label="Select the period date"
          prepend-icon="event" />
        <v-date-picker
          v-model="selectedDate"
          :first-day-of-week="1"
          :type="!kudosPeriodType || kudosPeriodType === 'WEEK' ? 'date' : 'month'"
          @input="selectedDateMenu = false" />
      </v-menu>
      <v-data-table
        v-model="selectedKudosIdentitiesList"
        :headers="kudosIdentitiesHeaders"
        :items="kudosIdentitiesList"
        :loading="loading"
        :sortable="true"
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
              <v-text-field v-if="props.item.address && props.item.received && (!props.item.status || props.item.status === 'error')" v-model="props.item.tokensToSend" class="input-text-center"/>
              <template v-else-if="props.item.status === 'success'">{{ toFixed(props.item.tokensSent) }}</template>
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
        :end-date-in-seconds="selectedEndDateInSeconds"
        @sent="newPendingTransaction"
        @error="error = $event" />
    </v-card-text>
  </v-card>
</template>

<script>
import {getKudosContract, getKudosBudget, saveKudosContract, saveKudosTotalBudget, getPeriodTransactions} from '../WalletKudosServices.js';
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
      kudosBudget: null,
      defaultKudosBudget: null,
      selectedDate: null,
      selectedDateMenu: false,
      kudosContractAddress: null,
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
      return this.selectedContractDetails && this.selectedContractDetails.name;
    },
    isContractDifferentFromPrincipal() {
      return this.kudosContractAddress && this.principalAccount && this.principalAccount.toLowerCase() !== this.kudosContractAddress.toLowerCase();
    },
    selectedContractDetails() {
      return this.selectedKudosContractAddress && this.contracts && this.contracts.find(contract => contract.address === this.selectedKudosContractAddress);
    },
    contractDetails() {
      return this.kudosContractAddress && this.contracts && this.contracts.find(contract => contract.address === this.kudosContractAddress);
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
      return this.selectedKudosIdentitiesList ? this.selectedKudosIdentitiesList.filter(item => item.address && item.received && item.tokensToSend && (!item.status || item.status === 'error')) : [];
    },
    totalKudosToSend() {
      return (this.validSelectedKudosIdentitiesList && this.validSelectedKudosIdentitiesList.length) ? this.validSelectedKudosIdentitiesList.reduce((accumulator, item) => accumulator + Number(item.received), 0) : 0;
    },
    totalKudos() {
      return (this.kudosIdentitiesList && this.kudosIdentitiesList.length) ? this.kudosIdentitiesList.reduce((accumulator, item) => accumulator + Number(item.received), 0) : 0;
    },
    validSelectedKudosIdentitiesList() {
      return this.selectedKudosIdentitiesList ? this.selectedKudosIdentitiesList.filter(this.isValidItem) : [];
    },
    selectableRecipients() {
      return this.kudosIdentitiesList ? this.kudosIdentitiesList.filter(this.isValidItem) : [];
    }
  },
  watch: {
    selectedDate() {
      this.loadAll();
    },
    selectedKudosIdentitiesList() {
      this.refreshList();
    },
    defaultKudosBudget() {
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
      if (window.kudosSettings) {
        this.kudosPeriodType = window.kudosSettings.kudosPeriodType;
      }
      this.selectedDate = new Date().toISOString().substr(0, 10);
      document.addEventListener('exo-kudos-get-period-result', this.loadPeriodDates);
    });
    getKudosBudget().then(value => this.defaultKudosBudget = this.kudosBudget = value);
    getKudosContract().then(kudosContract => this.kudosContractAddress = this.selectedKudosContractAddress = kudosContract || this.principalAccount);
    this.tokenEtherscanLink = getTokenEtherscanlink(window.walletSettings.defaultNetworkId);
    this.transactionEtherscanLink = getTransactionEtherscanlink(window.walletSettings.defaultNetworkId);
    this.addressEtherscanLink = getAddressEtherscanlink(window.walletSettings.defaultNetworkId);
  },
  methods: {
    isValidItem(userKudosItem) {
      return userKudosItem.address && userKudosItem.received && (!userKudosItem.status || userKudosItem.status === 'error');
    },
    newPendingTransaction(transaction) {
      this.$emit('pending', transaction);
      if(!transaction || !transaction.to) {
        return;
      }
      this.refreshKudosWalletStatus(transaction);
    },
    refreshKudosWalletStatus(transaction) {
      if(transaction && transaction.to) {
        const kudosWalletIndex = this.kudosIdentitiesList.findIndex(kudosWallet => kudosWallet.address && kudosWallet.address.toLowerCase() === transaction.to.toLowerCase());
        const kudosWallet = this.kudosIdentitiesList[kudosWalletIndex];
        if(kudosWallet) {
          this.$set(kudosWallet, 'hash', transaction.hash);
          if(this.principalAccount) {
            this.$set(kudosWallet, 'status', 'pending');
          }
          if (transaction && transaction.contractAmount) {
            this.$set(kudosWallet, 'tokensSent', transaction.contractAmount);
          }
          this.selectedKudosIdentitiesList.splice(kudosWalletIndex, 1);

          const thiss = this;
          watchTransactionStatus(transaction.hash, receipt => {
            thiss.$set(kudosWallet, 'status', receipt && receipt.status ? 'success' : 'error');
          });
        }
      } else {
        getPeriodTransactions(window.walletSettings.defaultNetworkId, this.kudosPeriodType, this.selectedStartDateInSeconds)
          .then(kudosTransactions => {
            if (kudosTransactions) {
              kudosTransactions.forEach(kudosTransaction => {
                if(kudosTransaction) {
                  const kudosWallet = this.kudosIdentitiesList.find(kudosWallet => kudosWallet.type === kudosTransaction.receiverType && (kudosWallet.id === kudosTransaction.receiverId || kudosWallet.identityId === kudosTransaction.receiverIdentityId));
                  if(kudosWallet) {
                    this.$set(kudosWallet, 'tokensSent', kudosTransaction.tokensAmountSent ? Number(kudosTransaction.tokensAmountSent) : 0);
                    this.$set(kudosWallet, 'hash', kudosTransaction.hash);
                    if(this.principalAccount) {
                      this.$set(kudosWallet, 'status', 'pending');
                    }
                    const thiss = this;
                    watchTransactionStatus(kudosTransaction.hash, receipt => {
                      thiss.$set(kudosWallet, 'status', receipt && receipt.status ? 'success' : 'error');
                    });
                  } else {
                    console.error("Can't find wallet of a sent Kudos token transaction, kudosTransaction=", kudosTransaction);
                  }
                }
              });
              this.$forceUpdate();
            }
          });
      }
    },
    save() {
      this.saveKudosContract().then(() => this.saveKudosTotalBudget());
    },
    saveKudosContract() {
      return saveKudosContract(this.selectedKudosContractAddress)
        .then(() => {
          this.kudosContractAddress = this.selectedKudosContractAddress;
        })
        .catch(error => {
          this.error = "Error while saving 'Kudos contract address' parameter";
        });
    },
    saveKudosTotalBudget() {
      return saveKudosTotalBudget(this.kudosBudget)
        .then(() => {
          this.defaultKudosBudget = this.kudosBudget;
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
      const tokenPerPerson = this.totalKudosToSend && this.defaultKudosBudget / this.totalKudosToSend;
      this.kudosIdentitiesList.forEach(walletKudos => {
        const wallet = this.wallets && this.wallets.find(wallet => wallet && wallet.id && wallet.technicalId && wallet.type === walletKudos.type &&  (wallet.id === walletKudos.id || wallet.technicalId === walletKudos.identityId));
        if(wallet && wallet.address) {
          this.$set(walletKudos, "address", wallet.address);
          if (this.defaultKudosBudget) {
            const tokensToSend = Number(walletKudos.received) * Number(tokenPerPerson);
            if(this.selectedKudosIdentitiesList.find(identity => identity.id === walletKudos.id)) {
              this.$set(walletKudos, "tokensToSend", this.toFixed(tokensToSend));
            } else {
              this.$set(walletKudos, "tokensToSend", 0);
            }
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