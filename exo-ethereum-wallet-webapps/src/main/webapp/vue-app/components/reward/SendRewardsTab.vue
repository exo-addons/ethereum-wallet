<template>
  <v-card flat>
    <v-card-text v-if="error && String(error).trim() != '{}'" class="text-xs-center">
      <div class="alert alert-error">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-text>
    <v-card-text
      class="text-xs-center"
      data-app>
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
          :type="!periodType || periodType === 'WEEK' ? 'date' : 'month'"
          @input="selectedDateMenu = false" />
      </v-menu>
    </v-card-text>
    <v-container fluid grid-list-md>
      <v-layout
        row
        wrap
        class="text-xs-center">
        <v-flex md4 xs12>
          <h4>Eligible users: <strong>{{ eligibleUsersCount }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>Total budget: <strong>{{ toFixed(totalBudget) }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex md4 xs12>
          <h4>Sent tokens: <strong>{{ toFixed(sentBudget) }} {{ symbol }}</strong></h4>
        </v-flex>
        <v-flex
          v-for="totalReward in totalRewards"
          :key="totalReward.pluginId"
          md4
          xs12>
          <h4>Total {{ totalReward.pluginId }}: <strong>{{ totalReward.total }}</strong></h4>
        </v-flex>
      </v-layout>
    </v-container>
    <v-container>
      <v-layout>
        <v-flex md4 xs12>
          <v-switch v-model="displayDisabledUsers" label="Display disabled users" />
        </v-flex>
      </v-layout>
      <v-flex>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search in name, pools, wallet address"
          single-line
          hide-details />
      </v-flex>
    </v-container>

    <v-data-table
      v-model="selectedIdentitiesList"
      :headers="identitiesHeaders"
      :items="filteredIdentitiesList"
      :loading="loading"
      :sortable="true"
      item-key="address"
      class="elevation-1 mr-3 mb-2"
      select-all
      disable-initial-sort
      hide-actions>
      <template slot="items" slot-scope="props">
        <tr :active="props.selected">
          <td>
            <v-checkbox
              v-if="props.item.address && props.item.enabled && props.item.tokensToSend && (!props.item.status || props.item.status === 'error')"
              :input-value="props.selected"
              hide-details
              @click="props.selected = !props.selected" />
          </td>
          <td>
            <v-avatar size="36px">
              <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
            </v-avatar>
          </td>
          <td class="text-xs-left">
            <profile-chip
              :address="props.item.address"
              :profile-id="props.item.id"
              :profile-technical-id="props.item.technicalId"
              :space-id="props.item.spaceId"
              :profile-type="props.item.type"
              :display-name="props.item.name"
              :enabled="props.item.enabled"
              :disabled-in-reward-pool="props.item.disabledPool"
              :disapproved="props.item.disapproved"
              :deleted-user="props.item.deletedUser"
              :disabled-user="props.item.disabledUser"
              :avatar="props.item.avatar"
              display-no-address />
          </td>
          <td class="text-xs-left">
            <ul v-if="props.item.rewardTeams && props.item.rewardTeams.length">
              <li v-for="team in props.item.rewardTeams" :key="team.id">
                <template v-if="team.disabled">
                  <del class="red--text">{{ team.name }}</del> (Disabled)
                </template>
                <template v-else>
                  {{ team.name }}
                </template>
              </li>
            </ul>
            <div v-else>
              -
            </div>
          </td>
          <td>
            <a
              v-if="props.item.hash"
              :href="`${transactionEtherscanLink}${props.item.hash}`"
              target="_blank"
              title="Open in etherscan">
              Open in etherscan
            </a> <span v-else>
              -
            </span>
          </td>
          <td>
            <template v-if="!props.item.status">
              <v-icon
                v-if="!props.item.address"
                color="warning"
                title="No address">
                warning
              </v-icon>
              <v-icon
                v-else-if="!props.item.tokensToSend"
                :title="`No enough earned points`"
                color="warning">
                warning
              </v-icon>
              <div v-else>
                -
              </div>
            </template>
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
            <v-text-field
              v-if="props.item.address && props.item.enabled && (!props.item.status || props.item.status === 'error')"
              v-model.number="props.item.tokensToSend"
              type="number"
              class="input-text-center" />
            <template v-else-if="props.item.status === 'success' || props.item.status === 'pending'">
              <span>{{ toFixed(props.item.tokensSent) }} {{ symbol }}</span>
            </template>
            <template
              v-else>
              -
            </template>
          </td>
          <td>
            <v-btn
              :disabled="!props.item.rewards || !props.item.rewards.length"
              icon
              small
              @click="selectedWallet = props.item">
              <v-icon size="16">fa-plus</v-icon>
            </v-btn>
          </td>
        </tr>
      </template>
      <template slot="footer">
        <td :colspan="identitiesHeaders.length - 1">
          <strong>
            Total
          </strong>
        </td>
        <td>
          <strong>
            {{ totalTokens }} {{ symbol }}
          </strong>
        </td>
        <td>
        </td>
      </template>
    </v-data-table>

    <v-card-actions>
      <v-spacer />
      <send-reward-modal
        :account="walletAddress"
        :contract-details="contractDetails"
        :recipients="recipients"
        :period-type="periodType"
        :start-date-in-seconds="selectedStartDateInSeconds"
        :end-date-in-seconds="selectedEndDateInSeconds"
        :default-transaction-label="defaultRewardLabelTemplate"
        :default-transaction-message="defaultRewardMessageTemplate"
        reward-count-field="points"
        @sent="newPendingTransaction"
        @error="error = $event" />
      <v-spacer />
    </v-card-actions>

    <reward-detail-modal
      ref="rewardDetails"
      :wallet="selectedWallet"
      :period="periodDatesDisplay"
      :symbol="symbol"
      @closed="selectedWallet = null" />
  </v-card>
</template>

<script>
import SendRewardModal from './modal/SendModal.vue';
import RewardDetailModal from './modal/RewardDetailModal.vue';
import ProfileChip from '../ProfileChip.vue';

import {getRewardTransactions, getRewardDates} from '../../WalletRewardServices.js';
import {watchTransactionStatus} from '../../WalletUtils.js';

export default {
  components: {
    RewardDetailModal,
    SendRewardModal,
    ProfileChip,
  },
  props: {
    wallets: {
      type: Array,
      default: function() {
        return [];
      },
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    periodType: {
      type: String,
      default: function() {
        return null;
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    transactionEtherscanLink: {
      type: String,
      default: function() {
        return null;
      },
    },
    eligibleUsersCount: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalBudget: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    sentBudget: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    totalRewards: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      search: '',
      displayDisabledUsers: false,
      defaultRewardLabelTemplate: '{name} is rewarded {amount} {symbol} for period: {startDate} to {endDate}',
      defaultRewardMessageTemplate: 'You have earned {amount} {symbol} in reward for your {rewardCount} {pluginName} {earned in pool_label} for period: {startDate} to {endDate}',
      selectedIdentitiesList: [],
      selectedDate: `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`,
      selectedDateMenu: false,
      selectedStartDate: null,
      selectedEndDate: null,
      loading: false,
      selectedWallet: null,
      identitiesHeaders: [
        {
          text: '',
          align: 'right',
          sortable: false,
          value: 'avatar',
          width: '36px',
        },
        {
          text: 'Name',
          align: 'left',
          sortable: true,
          value: 'name',
        },
        {
          text: 'Pools',
          align: 'center',
          sortable: false,
          value: 'rewardTeams',
        },
        {
          text: 'Transaction',
          align: 'center',
          sortable: true,
          value: 'hash',
        },
        {
          text: 'Status',
          align: 'center',
          sortable: true,
          value: 'status',
        },
        {
          text: 'Tokens',
          align: 'center',
          sortable: true,
          value: 'tokensToSend',
          width: '80px',
        },
        {
          text: '',
          align: 'center',
          sortable: false,
          value: 'actions',
          width: '80px',
        },
      ],
    };
  },
  computed: {
    periodDatesDisplay() {
      if (this.selectedStartDate && this.selectedEndDate) {
        return `${this.selectedStartDate} to ${this.selectedEndDate}`;
      } else if (this.selectedStartDate) {
        return this.selectedStartDate;
      } else {
        return '';
      }
    },
    selectedDateInSeconds() {
      return this.selectedDate ? new Date(this.selectedDate).getTime() / 1000 : 0;
    },
    selectedStartDateInSeconds() {
      return this.selectedStartDate ? new Date(this.selectedStartDate).getTime() / 1000 : 0;
    },
    selectedEndDateInSeconds() {
      return this.selectedEndDate ? new Date(this.selectedEndDate).getTime() / 1000 : 0;
    },
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
    recipients() {
      return this.selectedIdentitiesList ? this.selectedIdentitiesList.filter((item) => item.address && item.enabled && item.tokensToSend && (!item.status || item.status === 'error')) : [];
    },
    validRecipients() {
      return this.wallets ? this.wallets.filter((item) => item.address && item.tokensToSend && item.enabled) : [];
    },
    filteredIdentitiesList() {
      return this.wallets ? this.wallets.filter((wallet) => (this.displayDisabledUsers || wallet.enabled || wallet.tokensSent || wallet.tokensToSend) && this.filterItemFromList(wallet, this.search)) : [];
    },
    selectableRecipients() {
      return this.validRecipients.filter((item) => !item.status || item.status === 'error');
    },
    totalTokens() {
      if (this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach((wallet) => {
          result += wallet.tokensSent ? (wallet.tokensSent ? Number(wallet.tokensSent) : 0) : (wallet.tokensToSend && wallet.tokensToSend > 0 ? Number(wallet.tokensToSend) : 0);
        });
        return this.toFixed(result);
      } else {
        return 0;
      }
    },
  },
  watch: {
    selectedWallet() {
      if(this.selectedWallet) {
        this.$refs.rewardDetails.open();
      }
    },
    selectedDate() {
      this.refreshDates()
        .then(() => this.$nextTick())
        .then(() => this.$emit('dates-changed'));
    }
  },
  methods: {
    refreshDates() {
      this.loading = true;
      return getRewardDates(new Date(this.selectedDate), this.periodType)
        .then((period) => {
          this.selectedStartDate = this.formatDate(new Date(period.startDateInSeconds * 1000));
          this.selectedEndDate = this.formatDate(new Date(period.endDateInSeconds * 1000));
        })
        .finally(() => this.loading = false);
    },
    computeTokensToSend() {
      this.$forceUpdate();
    },
    filterItemFromList(wallet, searchText) {
      if (!searchText || !searchText.length) {
        return true;
      }
      searchText = searchText.trim().toLowerCase();
      const name = wallet.name.toLowerCase();
      if (name.indexOf(searchText) > -1) {
        return true;
      }
      const address = wallet.address.toLowerCase();
      if (address.indexOf(searchText) > -1) {
        return true;
      }
      if (searchText === '-' && (!wallet.rewardTeams || !wallet.rewardTeams.length)) {
        return true;
      }
      const teams =
        wallet.rewardTeams && wallet.rewardTeams.length
          ? wallet.rewardTeams
              .map((team) => team.name)
              .join(',')
              .toLowerCase()
          : '';
      return teams.indexOf(searchText) > -1;
    },
    newPendingTransaction(transaction) {
      if (!transaction || !transaction.to) {
        return;
      }
      this.$emit('pending', transaction);
      this.refreshWalletTransactionStatus(transaction);
    },
    refreshWalletTransactionStatus(transaction) {
      if (transaction && transaction.to) {
        const resultWalletIndex = this.selectedIdentitiesList.findIndex((resultWallet) => resultWallet.address && resultWallet.address.toLowerCase() === transaction.to.toLowerCase());
        const resultWallet = this.selectedIdentitiesList[resultWalletIndex];
        if (resultWallet) {
          if(transaction.contractAmount) {
            this.$set(resultWallet, 'tokensSent', transaction.contractAmount);
            this.$set(resultWallet, 'hash', transaction.hash);
            this.$set(resultWallet, 'status', 'pending');

            this.$emit('pending', transaction);

            this.selectedIdentitiesList.splice(resultWalletIndex, 1);
          }
          const thiss = this;
          watchTransactionStatus(transaction.hash, (receipt) => {
            thiss.$set(resultWallet, 'status', receipt && receipt.status ? 'success' : 'error');
            this.$emit('success', transaction, receipt);
          });
        }
      } else {
        this.loading = true;
        return getRewardTransactions(window.walletSettings.defaultNetworkId, this.periodType, this.selectedStartDateInSeconds).then((resultTransactions) => {
          if (resultTransactions) {
            resultTransactions.forEach((resultTransaction) => {
              if (resultTransaction) {
                const resultWallet = this.wallets.find((resultWallet) => resultWallet.type === resultTransaction.receiverType && (resultWallet.id === resultTransaction.receiverId || resultWallet.identityId === resultTransaction.receiverIdentityId));
                if (resultWallet) {
                  this.$set(resultWallet, 'tokensSent', resultTransaction.tokensAmountSent ? Number(resultTransaction.tokensAmountSent) : 0);
                  this.$set(resultWallet, 'hash', resultTransaction.hash);
                  this.$set(resultWallet, 'status', 'pending');
                  this.$emit('pending', resultTransaction);

                  const thiss = this;
                  watchTransactionStatus(resultTransaction.hash, (receipt) => {
                    thiss.$set(resultWallet, 'status', receipt && receipt.status ? 'success' : 'error');
                    this.$emit('success', resultTransaction, receipt);
                  });
                } else {
                  console.warn("Can't find wallet of a sent result token transaction, resultTransaction=", resultTransaction);
                }
              }
            });
          }
        })
        .finally(() => this.loading = false);
      }
    },
    formatDate(date) {
      if (!date) {
        return null;
      }
      const dateString = date.toString();
      // Example: 'Feb 01 2018'
      return dateString.substring(dateString.indexOf(' ') + 1, dateString.indexOf(':') - 3);
    },
  },
};
</script>
