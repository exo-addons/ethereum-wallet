<template>
  <v-flex>
    <v-card-title class="ml-5 mr-5"> <v-text-field v-model="search" append-icon="search" label="Search in name, pools, wallet address" single-line hide-details /> </v-card-title>
    <v-data-table v-model="selectedIdentitiesList" :headers="identitiesHeaders" :items="filteredIdentitiesList" :loading="loading" :sortable="true" item-key="address" class="elevation-1 mr-3 mb-2" select-all disable-initial-sort hide-actions>
      <template slot="items" slot-scope="props">
        <tr :active="props.selected">
          <td><v-checkbox v-if="props.item.address && props.item.points && props.item.points >= threshold && (!props.item.status || props.item.status === 'error')" :input-value="props.selected" hide-details @click="props.selected = !props.selected" /></td>
          <td>
            <v-avatar size="36px"> <img :src="props.item.avatar" onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'" /> </v-avatar>
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
          <td class="text-xs-left">
            <ul v-if="props.item.gamificationTeams && props.item.gamificationTeams.length">
              <li v-for="team in props.item.gamificationTeams" :key="team.id">{{ team.name }}</li>
            </ul>
            <div v-else>-</div>
          </td>
          <td><a v-if="props.item.hash" href="javascript:void(0);" target="_blank" title="Open transaction in wallet" @click="$emit('open-wallet-transaction', props.item)"> Open in wallet </a> <span v-else>-</span></td>
          <td>
            <template v-if="!props.item.status">
              <v-icon v-if="!props.item.address" color="warning" title="No address"> warning </v-icon>
              <v-icon v-else-if="!props.item.points || props.item.points < threshold" :title="`Not enough points, minimum: ${threshold} points`" color="warning"> warning </v-icon>
              <div v-else>-</div>
            </template>
            <v-progress-circular v-else-if="props.item.status === 'pending'" color="primary" indeterminate size="20" />
            <v-icon v-else :color="props.item.status === 'success' ? 'success' : 'error'" :title="props.item.status === 'success' ? 'Successfully proceeded' : props.item.status === 'pending' ? 'Transaction in progress' : 'Transaction error'" v-text="props.item.status === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'" />
          </td>
          <td>
            <v-text-field v-if="props.item.address && props.item.points && props.item.points >= threshold && (!props.item.status || props.item.status === 'error')" v-model.number="props.item.tokensToSend" type="number" class="input-text-center" />
            <template v-else-if="props.item.status === 'success'">{{ toFixed(props.item.tokensSent) }}</template>
            <template v-else
              >-</template
            >
          </td>
          <td>{{ props.item.points }}</td>
        </tr>
      </template>
      <template slot="footer">
        <td :colspan="identitiesHeaders.length - 1"><strong>Total</strong></td>
        <td>
          <strong>{{ totalTokens }}</strong>
        </td>
        <td>
          <strong>{{ totalPoints }}</strong>
        </td>
      </template>
    </v-data-table>

    <send-reward-modal :account="walletAddress" :contract-details="contractDetails" :recipients="recipients" :period-type="periodType" :start-date-in-seconds="startDateInSeconds" :end-date-in-seconds="endDateInSeconds" :reward-type="rewardType" default-transaction-label="gamification points reward" default-transaction-message="gamification points reward" reward-count-field="points" @sent="newPendingTransaction" @error="error = $event" />
  </v-flex>
</template>

<script>
import SendRewardModal from './SendRewardModal.vue';

import {getPeriodRewardTransactions} from '../WalletRewardServices.js';
import {watchTransactionStatus, getTransactionEtherscanlink} from '../WalletUtils.js';

export default {
  components: {
    SendRewardModal,
  },
  props: {
    walletAddress: {
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
    identitiesList: {
      type: Array,
      default: function() {
        return [];
      },
    },
    contractDetails: {
      type: Object,
      default: function() {
        return null;
      },
    },
    threshold: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    periodType: {
      type: String,
      default: function() {
        return null;
      },
    },
    startDateInSeconds: {
      type: Number,
      default: function() {
        return 0;
      },
    },
    endDateInSeconds: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data() {
    return {
      search: '',
      selectedIdentitiesList: [],
      rewardType: 'GAMIFICATION_PERIOD_TRANSACTIONS',
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
          value: 'gamificationTeams',
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
          width: '70px',
        },
        {
          text: 'Points',
          align: 'center',
          sortable: true,
          value: 'points',
        },
      ],
    };
  },
  computed: {
    recipients() {
      return this.selectedIdentitiesList ? this.selectedIdentitiesList.filter((item) => item.address && item.points && item.points >= this.threshold && item.tokensToSend && (!item.status || item.status === 'error')) : [];
    },
    validRecipients() {
      return this.identitiesList ? this.identitiesList.filter((item) => item.address && item.points >= this.threshold) : [];
    },
    filteredIdentitiesList() {
      return this.identitiesList ? this.identitiesList.filter((wallet) => this.filterItemFromList(wallet, this.search)) : [];
    },
    selectableRecipients() {
      return this.validRecipients.filter((item) => !item.status || item.status === 'error');
    },
    totalPoints() {
      if (this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach((wallet) => {
          result += Number(wallet.points);
        });
        return result;
      } else {
        return 0;
      }
    },
    totalTokens() {
      if (this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach((wallet) => {
          result += wallet.tokensToSend ? Number(wallet.tokensToSend) : 0 + wallet.tokensSent ? Number(wallet.tokensSent) : 0;
        });
        return result;
      } else {
        return 0;
      }
    },
  },
  methods: {
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
      if (searchText === '-' && (!wallet.gamificationTeams || !wallet.gamificationTeams.length)) {
        return true;
      }
      const teams =
        wallet.gamificationTeams && wallet.gamificationTeams.length
          ? wallet.gamificationTeams
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
          this.$set(resultWallet, 'hash', transaction.hash);
          this.$set(resultWallet, 'status', 'pending');
          if (transaction.contractAmount) {
            this.$set(resultWallet, 'tokensSent', transaction.contractAmount);
          }
          this.selectedIdentitiesList.splice(resultWalletIndex, 1);
          const thiss = this;
          watchTransactionStatus(transaction.hash, (receipt) => {
            thiss.$set(resultWallet, 'status', receipt && receipt.status ? 'success' : 'error');
          });
        }
      } else {
        getPeriodRewardTransactions(window.walletSettings.defaultNetworkId, this.periodType, this.startDateInSeconds, this.rewardType).then((resultTransactions) => {
          if (resultTransactions) {
            resultTransactions.forEach((resultTransaction) => {
              if (resultTransaction) {
                const resultWallet = this.identitiesList.find((resultWallet) => resultWallet.type === resultTransaction.receiverType && (resultWallet.id === resultTransaction.receiverId || resultWallet.identityId === resultTransaction.receiverIdentityId));
                if (resultWallet) {
                  this.$set(resultWallet, 'tokensSent', resultTransaction.tokensAmountSent ? Number(resultTransaction.tokensAmountSent) : 0);
                  this.$set(resultWallet, 'hash', resultTransaction.hash);
                  this.$set(resultWallet, 'status', 'pending');
                  const thiss = this;
                  watchTransactionStatus(resultTransaction.hash, (receipt) => {
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
  },
};
</script>
