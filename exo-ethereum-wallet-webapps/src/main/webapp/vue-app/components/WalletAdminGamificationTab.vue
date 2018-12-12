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
        <span>Minimal gamification points threshold to reward users: </span>
        <v-text-field
          v-model.number="selectedThreshold"
          name="threshold"
          class="input-text-center" />
      </div>
      <div class="text-xs-left gamificationWalletConfiguration">
        <span>The total gamification reward buget is set:</span>
        <v-flex class="mt-3" xs12>
          <v-radio-group v-model="selectedRewardType">
            <v-radio value="FIXED" label="By a fixed budget" />
            <v-flex v-if="selectedRewardType === 'FIXED'" xs12 sm6>
              <v-text-field
                v-model.number="selectedTotalBudget"
                placeholder="Enter the fixed total budget"
                type="number"
                class="pt-0 pb-0"
                name="totalBudget" />
              <span> using token </span>
              <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
                <v-combobox
                  v-model="selectedContractAddress"
                  :items="contracts"
                  :return-object="false"
                  attach="#selectedContractAddress"
                  item-value="address"
                  item-text="name"
                  class="selectBoxVuetify xs12 sm2"
                  hide-no-data
                  hide-selected
                  small-chips>
                  <template slot="selection" slot-scope="data">
                    {{ selectedContractName }}
                  </template>
                </v-combobox>
              </div>
            </v-flex>
            <v-radio value="FIXED_PER_MEMBER" label="By a fixed budget per valid member on period" />
            <v-flex v-if="selectedRewardType === 'FIXED_PER_MEMBER'">
              <v-text-field
                v-model="selectedBudgetPerMember"
                placeholder="Enter the fixed budget per valid member on period"
                type="number"
                class="pt-0 pb-0 xs12 sm2"
                name="budgetPerMember" />
              <span> using token </span>
              <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
                <v-combobox
                  v-model="selectedContractAddress"
                  :items="contracts"
                  :return-object="false"
                  attach="#selectedContractAddress"
                  item-value="address"
                  item-text="name"
                  class="selectBoxVuetify xs12 sm2"
                  hide-no-data
                  hide-selected
                  small-chips>
                  <template slot="selection" slot-scope="data">
                    {{ selectedContractName }}
                  </template>
                </v-combobox>
              </div>
            </v-flex>
          </v-radio-group>
        </v-flex>
      </div>
      <div class="text-xs-left gamificationWalletConfiguration">
        <span>Reward priodicity: </span>
        <div id="selectedPeriodType" class="selectBoxVuetifyParent v-input">
          <v-combobox
            v-model="selectedPeriodType"
            :items="periods"
            :return-object="false"
            attach="#selectedPeriodType"
            class="selectBoxVuetify"
            hide-no-data
            hide-selected
            small-chips>
            <template slot="selection" slot-scope="data">
              {{ selectedPeriodName }}
            </template>
          </v-combobox>
        </div>
      </div>
    </v-card-text>
    <v-card-actions>
      <v-btn :loading="loadingSettings" class="btn btn-primary ml-2" dark @click="save">
        Save
      </v-btn>
    </v-card-actions>
    <v-card-text class="text-xs-center">
      <div v-if="isContractDifferentFromPrincipal" class="alert alert-warning">
        <i class="uiIconWarning"></i>
        You have chosen a token that is different from principal displayed token
      </div>
    </v-card-text>
    <gamification-teams
      ref="gamificationTeams"
      :wallets="validRecipients"
      :contract-details="selectedContractDetails"
      @teams-retrieved="refreshTeams" />
    <h3 class="text-xs-left ml-3">Send Rewards</h3>
    <v-card-text class="text-xs-center">
      <div v-if="duplicatedWallets && duplicatedWallets.length" class="alert alert-warning">
        <i class="uiIconWarning"></i>
        There is some user(s) with multiple teams, thus the calculation could be wrong:
        <ul>
          <li v-for="duplicatedWallet in duplicatedWallets" :key="duplicatedWallet.id">
            <code>{{ duplicatedWallet.name }}</code>
          </li>
        </ul>
      </div>
    </v-card-text>
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
      <v-card-title>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search"
          single-line
          hide-details />
      </v-card-title>
      <v-data-table
        v-model="selectedIdentitiesList"
        :headers="identitiesHeaders"
        :items="filteredIdentitiesList"
        :loading="loading"
        :sortable="true"
        select-all
        item-key="address"
        class="elevation-1 mr-3 mb-2"
        disable-initial-sort
        hide-actions>
        <template slot="items" slot-scope="props">
          <tr :active="props.selected">
            <td>
              <v-checkbox
                v-if="props.item.address && props.item.points && props.item.points >= threshold && (!props.item.status || props.item.status === 'error')"
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
            <td class="text-xs-left">
              <ul v-if="props.item.gamificationTeams && props.item.gamificationTeams.length">
                <li
                  v-for="team in props.item.gamificationTeams"
                  :key="team.id">
                  {{ team.name }}
                </li>
              </ul>
              <div v-else>
                -
              </div>
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
              <template v-if="!props.item.status">
                <v-icon
                  v-if="!props.item.address"
                  color="warning"
                  title="No address">
                  warning
                </v-icon>
                <v-icon
                  v-else-if="!props.item.points || props.item.points < threshold"
                  :title="`Not enough points, minimum: ${threshold} points`"
                  color="warning">
                  warning
                </v-icon>
                <div v-else>-</div>
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
              <v-text-field v-if="props.item.address && props.item.points && props.item.points >= threshold && (!props.item.status || props.item.status === 'error')" v-model="props.item.tokensToSend" class="input-text-center"/>
              <template v-else-if="props.item.status === 'success'">{{ toFixed(props.item.tokensSent) }}</template>
              <template v-else>-</template>
            </td>
            <td v-html="props.item.points">
            </td>
          </tr>
        </template>
        <template slot="footer">
          <td :colspan="identitiesHeaders.length - 1">
            <strong>Total</strong>
          </td>
          <td>
            <strong>{{ totalTokens }}</strong>
          </td>
          <td>
            <strong>{{ totalPoints }}</strong>
          </td>
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
      loadingSettings: false,
      error: null,
      selectedDate: new Date().toISOString().substr(0, 10),
      selectedDateMenu: false,
      contractAddress: null,
      threshold: null,
      totalBudget: null,
      budgetPerMember: null,
      periodType: null,
      duplicatedWallets: null,
      search: '',
      selectedContractAddress: null,
      selectedThreshold: null,
      selectedBudgetPerMember: null,
      selectedRewardType: 'FIXED',
      selectedTotalBudget: null,
      selectedPeriodType: null,
      rewardType: 'FIXED',
      tokenEtherscanLink: null,
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
      identitiesList: [],
      selectedIdentitiesList: [],
      selectedStartDate: null,
      selectedEndDate: null,
      pagination: {
        descending: true,
        sortBy: 'points'
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
          sortable: true,
          value: 'name'
        },
        {
          text: 'Teams',
          align: 'center',
          sortable: true,
          value: 'gamificationTeams'
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
      return this.selectedIdentitiesList ? this.selectedIdentitiesList.filter(item => item.address && item.points && item.points >= this.threshold && item.tokensToSend && !item.tokensSent && (!item.status || item.status === 'error')) : [];
    },
    validRecipients() {
      return this.identitiesList ? this.identitiesList.filter(item => item.address && item.points >= this.threshold) : [];
    },
    computedTotalBudget() {
      if(this.rewardType === 'FIXED') {
        return this.totalBudget ? Number(this.totalBudget) : 0;
      } else if(this.rewardType === 'FIXED_PER_MEMBER') {
        return this.budgetPerMember && this.validRecipients && this.validRecipients.length ? this.validRecipients.length * this.budgetPerMember : 0;
      }
    },
    filteredIdentitiesList() {
      return this.identitiesList ? this.identitiesList.filter(wallet => this.filterItemFromList(wallet, this.search)) : [];
    },
    selectableRecipients() {
      return this.validRecipients.filter(item => !item.tokensSent && (!item.status || item.status === 'error'));
    },
    totalPoints() {
      if(this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach(wallet => {
          result += Number(wallet.points);
        });
        return result;
      } else {
        return 0;
      }
    },
    totalTokens() {
      if(this.filteredIdentitiesList) {
        let result = 0;
        this.filteredIdentitiesList.forEach(wallet => {
          result += wallet.tokensToSend ? Number(wallet.tokensToSend) : 0 + wallet.tokensSent ? Number(wallet.tokensSent) : 0;
        });
        return result;
      } else {
        return 0;
      }
    }
  },
  watch: {
    selectedDate() {
      this.loadAll();
    },
    wallets() {
      this.refreshTeams();
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
          this.selectedRewardType = this.rewardType = settings.rewardType;
          this.selectedBudgetPerMember = this.budgetPerMember = settings.budgetPerMember;
          this.selectedTotalBudget = this.totalBudget = settings.totalBudget;
          this.selectedThreshold = this.threshold = settings.threshold;
          this.selectedPeriodType = this.periodType = settings.periodType;
        }
      });
  },
  methods: {
    refreshTeams() {
      this.duplicatedWallets = [];
      if(this.$refs && this.$refs.gamificationTeams && this.$refs.gamificationTeams.teamsRetrieved) {
        const teams = this.$refs.gamificationTeams.teams;
        const wallets = this.identitiesList;
        wallets.forEach(wallet => {
          delete wallet.gamificationTeams;
        });
        teams.forEach(team => {
          if (team.members) {
            team.members.forEach(memberObject => {
              const wallet = wallets.find(wallet => wallet && wallet.id && wallet.type === 'user' && wallet.id ===  memberObject.id);
              if (wallet) {
                if(wallet.gamificationTeams && wallet.gamificationTeams.length) {
                  wallet.gamificationTeams.push(team);
                  this.duplicatedWallets.push(wallet);
                } else {
                  this.$set(wallet, 'gamificationTeams', [team]);
                }
              }
            });
          }
        });
        this.computeBudgets();
      }
    },
    computeBudgets() {
      if(this.identitiesList && this.identitiesList.length) {
        this.identitiesList.forEach(wallet => {
          wallet.tokensToSend = 0;
        });
      }

      if(!this.validRecipients.length) {
        return;
      }
      const validRecipients = this.validRecipients.slice();
      let teams = [];
      if(this.$refs.gamificationTeams && this.$refs.gamificationTeams.teams && this.$refs.gamificationTeams.teams.length) {
        teams = this.$refs.gamificationTeams.teams.slice();
      }
      const membersWithEmptyTeam = validRecipients.filter(wallet => !wallet.gamificationTeams || !wallet.gamificationTeams.length);

      // Members with no Team
      teams.push({
        members: membersWithEmptyTeam,
        rewardType: 'COMPUTED',
        computedBudget: 0
      });

      let totalComputedBudget = this.computedTotalBudget;
      let computedRecipientsCount = 0;
      teams.forEach(team => {
        team.validMembersWallets = [];
        team.computedBudget = 0;
        team.fixedBudget = 0;
        team.totalValidPoints = 0;
        if(!team.members || !team.members.length) {
          return;
        }

        team.members.forEach(memberObject => {
          const wallet = memberObject.address ? memberObject : validRecipients.find(wallet => wallet.id === memberObject.id);
          if(wallet) {
            team.validMembersWallets.push(wallet);
            team.totalValidPoints += wallet.points;
          }
        });

        if(team.validMembersWallets.length) {
          if (team.rewardType === 'FIXED') {
            team.computedBudget = team.fixedBudget = team.budget ? Number(team.budget) : 0;
            totalComputedBudget -= team.computedBudget;
          } else if (team.rewardType === 'FIXED_PER_MEMBER') {
            team.computedBudget = team.fixedBudget = team.rewardPerMember ? Number(team.rewardPerMember) * team.validMembersWallets.length : 0;
            totalComputedBudget -= team.computedBudget;
          } else if (team.rewardType === 'COMPUTED') {
            computedRecipientsCount += team.validMembersWallets.length;
          }
        }
      });

      const tokenPerRecipient = computedRecipientsCount > 0 && totalComputedBudget > 0 ? totalComputedBudget / computedRecipientsCount : 0;

      teams.forEach(team => {
        if(!team.validMembersWallets || !team.validMembersWallets.length) {
          return;
        }

        let budget = 0;
        if (team.rewardType === 'FIXED' || team.rewardType === 'FIXED_PER_MEMBER') {
          budget = team.fixedBudget ? Number(team.fixedBudget) : 0;
        } else if (team.rewardType === 'COMPUTED') {
          budget = team.computedBudget = tokenPerRecipient * team.validMembersWallets.length;
        }

        if (budget) {
          team.validMembersWallets.forEach(wallet => {
            const tokensToSend = budget * wallet.points / team.totalValidPoints;
            this.$set(wallet, 'tokensToSend', this.toFixed(tokensToSend));
          });
        }
      });
    },
    filterItemFromList(wallet, searchText) {
      if(!searchText || !searchText.length) {
        return true;
      }
      searchText = searchText.trim().toLowerCase();
      const name = wallet.name.toLowerCase();
      if(name.indexOf(searchText) > -1) {
        return true;
      }
      const address = wallet.address.toLowerCase();
      if(address.indexOf(searchText) > -1) {
        return true;
      }
      if(searchText === '-' && (!wallet.gamificationTeams || !wallet.gamificationTeams.length)) {
        return true;
      }
      const teams = wallet.gamificationTeams && wallet.gamificationTeams.length ? wallet.gamificationTeams.map(team => team.name).join(',').toLowerCase() : '';
      return teams.indexOf(searchText) > -1;
    },
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
            thiss.$set(resultWallet, 'status', receipt && receipt.status ? 'success' : 'error');
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
      const thiss = this;
      this.loadingSettings = true;
      this.selectedTotalBudget = this.selectedRewardType === 'FIXED' ? this.selectedTotalBudget : 0;
      this.selectedBudgetPerMember = this.selectedRewardType === 'FIXED_PER_MEMBER' ? this.selectedBudgetPerMember : 0;
      window.setTimeout(() => {
        saveSettings({
          totalBudget: thiss.selectedTotalBudget,
          budgetPerMember: thiss.selectedBudgetPerMember,
          rewardType: thiss.selectedRewardType,
          threshold: thiss.selectedThreshold,
          contractAddress : thiss.selectedContractAddress,
          periodType: thiss.selectedPeriodType && (thiss.selectedPeriodType.value || thiss.selectedPeriodType)
        })
          .then(() => {
            thiss.contractAddress = thiss.selectedContractAddress;
            thiss.periodType = thiss.selectedPeriodType;
            thiss.threshold = thiss.selectedThreshold;
            thiss.rewardType = thiss.selectedRewardType;
            thiss.totalBudget = thiss.selectedTotalBudget;
            thiss.budgetPerMember = thiss.selectedBudgetPerMember;
            return this.refreshTeams();
          })
          .catch(error => {
            thiss.error = "Error while saving 'Gamification settings'";
          })
          .finally(() => {
            thiss.loadingSettings = false;
          });
      }, 200);
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
        .then(() => {
          this.refreshTeams();
          this.loading = false;
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
    }
  }
};
</script>