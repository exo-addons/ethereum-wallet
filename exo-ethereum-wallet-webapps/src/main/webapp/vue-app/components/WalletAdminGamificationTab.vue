<template>
  <v-card flat>
    <v-card-text v-if="error && String(error).trim() != '{}'" class="text-xs-center">
      <div class="alert alert-error v-content">
        <i class="uiIconError"></i> {{ error }}
      </div>
    </v-card-text>
    <v-card-text v-if="isContractDifferentFromPrincipal" class="text-xs-center">
      <div class="alert alert-warning">
        <i class="uiIconWarning"></i> You have chosen a token that is different from principal displayed token
      </div>
    </v-card-text>
    <v-tabs
      v-model="selectedTab"
      grow
      hide-slider>
      <v-tab key="SendRewards">
        Send Rewards
      </v-tab>
      <v-tab key="RewardPools">
        Reward pools
      </v-tab>
      <v-tab key="Configuration">
        Configuration
      </v-tab>
    </v-tabs>

    <v-tabs-items v-model="selectedTab">
      <v-tab-item id="SendRewards">
        <v-card flat>
          <v-card-text v-if="duplicatedWallets && duplicatedWallets.length" class="text-xs-center">
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> Some users are members of multiple pools, the budget computing could be wrong:
              <ul>
                <li v-for="duplicatedWallet in duplicatedWallets" :key="duplicatedWallet.id">
                  <code>{{ duplicatedWallet.name }}</code>
                </li>
              </ul>
            </div>
          </v-card-text>
          <v-card-text
            v-if="selectedTab === 0"
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
          <send-rewards-tab
            ref="sendRewards"
            :wallet-address="walletAddress"
            :loading="loading"
            :identities-list="identitiesList"
            :contract-details="contractDetails"
            :threshold="threshold"
            :period-type="periodType"
            :start-date-in-seconds="selectedStartDateInSeconds"
            :end-date-in-seconds="selectedEndDateInSeconds"
            @open-wallet-transaction="$emit('open-wallet-transaction', $event)"
            @pending="pendingTransaction"
            @success="successTransaction" />
        </v-card>
      </v-tab-item>
      <v-tab-item id="RewardPools">
        <v-card flat>
          <v-card-text v-if="duplicatedWallets && duplicatedWallets.length" class="text-xs-center">
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> Some users are members of multiple pools, the budget computing could be wrong:
              <ul>
                <li v-for="duplicatedWallet in duplicatedWallets" :key="duplicatedWallet.id">
                  <code>{{ duplicatedWallet.name }}</code>
                </li>
              </ul>
            </div>
          </v-card-text>
          <v-card-text v-if="displayDateSelector && selectedTab === 1" class="text-xs-center">
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
                prepend-icon="event"
                width="150px" />
              <v-date-picker
                v-model="selectedDate"
                :first-day-of-week="1"
                :type="!periodType || periodType === 'WEEK' ? 'date' : 'month'"
                @input="selectedDateMenu = false" />
            </v-menu>
          </v-card-text>
          <gamification-teams-tab
            ref="gamificationTeams"
            :wallets="validRecipients"
            :contract-details="contractDetails"
            :period="periodDatesDisplay"
            :computed-total-budget="computedTeamsBudget"
            :sent-total-budget="sentTotalBudget"
            :reward-type="rewardType"
            :budget-per-member="budgetPerMember"
            :total-budget="totalBudget"
            @teams-retrieved="refresh"
            @form-opened="displayDateSelector = false"
            @form-closed="displayDateSelector = true" />
        </v-card>
      </v-tab-item>
      <v-tab-item id="Configuration">
        <configuration-tab
          ref="configurationTab"
          :contracts="contracts"
          @saved="changeSettings"
          @error="error = $event" />
      </v-tab-item>
    </v-tabs-items>
  </v-card>
</template>

<script>
import GamificationTeamsTab from './WalletAdminGamificationTeamsTab.vue';
import SendRewardsTab from './WalletAdminGamificationSendRewardsTab.vue';
import ConfigurationTab from './WalletAdminGamificationConfigurationTab.vue';

import {getSettings, getGamificationPoints} from '../WalletGamificationServices.js';
import {getPeriodRewardDates} from '../WalletRewardServices.js';

export default {
  components: {
    ConfigurationTab,
    SendRewardsTab,
    GamificationTeamsTab,
  },
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      },
    },
    wallets: {
      type: Array,
      default: function() {
        return [];
      },
    },
    contracts: {
      type: Array,
      default: function() {
        return [];
      },
    },
    principalAccount: {
      type: String,
      default: function() {
        return null;
      },
    },
  },
  data() {
    return {
      loading: false,
      error: null,
      selectedDate: new Date().toISOString().substr(0, 10),
      selectedDateMenu: false,
      displayDateSelector: true,
      contractAddress: null,
      threshold: null,
      computedTeamsBudget: null,
      totalBudget: null,
      budgetPerMember: null,
      selectedTab: true,
      periodType: null,
      duplicatedWallets: null,
      rewardType: 'FIXED',
      identitiesList: [],
      selectedIdentitiesList: [],
      selectedStartDate: null,
      selectedEndDate: null,
    };
  },
  computed: {
    isContractDifferentFromPrincipal() {
      return this.contractAddress && this.principalAccount && this.principalAccount.toLowerCase() !== this.contractAddress.toLowerCase();
    },
    contractDetails() {
      return this.contractAddress && this.contracts && this.contracts.find((contract) => contract.address === this.contractAddress);
    },
    selectedStartDateInSeconds() {
      return this.selectedStartDate ? new Date(this.selectedStartDate).getTime() / 1000 : 0;
    },
    selectedEndDateInSeconds() {
      return this.selectedEndDate ? new Date(this.selectedEndDate).getTime() / 1000 : 0;
    },
    periodDatesDisplay() {
      if (this.selectedStartDate && this.selectedEndDate) {
        return `${this.selectedStartDate} to ${this.selectedEndDate}`;
      } else if (this.selectedStartDate) {
        return this.selectedStartDate;
      } else {
        return '';
      }
    },
    sentTotalBudget() {
      if (this.identitiesList && this.identitiesList.length) {
        let sentTokens = 0;
        this.identitiesList.forEach((wallet) => {
          sentTokens += wallet.tokensSent ? Number(wallet.tokensSent) : 0;
        });
        return sentTokens;
      } else {
        return 0;
      }
    },
    validRecipients() {
      return this.identitiesList ? this.identitiesList.filter((item) => item.address && item.points && item.points >= this.threshold) : [];
    },
  },
  watch: {
    selectedDate() {
      this.loadAll();
    },
    periodType() {
      this.loadAll();
    },
  },
  created() {
    getSettings()
      .then((settings) => {
        if (settings) {
          this.contractAddress = settings.contractAddress;
          this.rewardType = settings.rewardType;
          this.budgetPerMember = settings.budgetPerMember;
          this.totalBudget = settings.totalBudget;
          this.threshold = settings.threshold;
          this.periodType = settings.periodType;
          this.$refs.configurationTab.init(settings);
        }
      });
  },
  methods: {
    changeSettings(settings) {
      if (settings) {
        this.contractAddress = settings.contractAddress;
        this.periodType = settings.periodType;
        this.threshold = settings.threshold;
        this.rewardType = settings.rewardType;
        this.totalBudget = settings.totalBudget;
        this.budgetPerMember = settings.budgetPerMember;
        this.$nextTick(() => {
          this.refresh();
        });
      }
    },
    refresh() {
      this.duplicatedWallets = [];
      if (this.$refs && this.$refs.gamificationTeams && this.$refs.gamificationTeams.teamsRetrieved) {
        const teams = this.$refs.gamificationTeams.teams;
        const wallets = this.identitiesList;
        wallets.forEach((wallet) => {
          delete wallet.gamificationTeams;
        });
        teams.forEach((team) => {
          if (team.id && team.members) {
            team.members.forEach((memberObject) => {
              const wallet = wallets.find((wallet) => wallet && wallet.id && wallet.type === 'user' && wallet.id === memberObject.id);
              if (wallet) {
                if (wallet.gamificationTeams && wallet.gamificationTeams.length) {
                  wallet.gamificationTeams.push(team);
                  this.duplicatedWallets.push(wallet);
                } else {
                  this.$set(wallet, 'gamificationTeams', [team]);
                }
              }
            });
          }
        });

        const membersWithEmptyTeam = this.validRecipients.filter((wallet) => !wallet.gamificationTeams || !wallet.gamificationTeams.length);

        let team = teams.find((team) => team.id === 0);
        if (!team) {
          team = {
            id: 0,
            name: 'No pool users',
            description: 'Users with no associated pool',
            members: membersWithEmptyTeam,
            rewardType: 'COMPUTED',
            computedBudget: 0,
          };
          // Members with no Team
          teams.push(team);
        } else {
          team.members = membersWithEmptyTeam;
        }

        if (teams && teams.length) {
          teams.sort((team1, team2) => Number(team1.id) - Number(team2.id));
        }

        this.computeBudgets();
      }
    },
    computeBudgets() {
      console.warn("-------- computeBudgets------");
      if (this.identitiesList && this.identitiesList.length) {
        this.identitiesList.forEach((wallet) => {
          wallet.tokensToSend = 0;
        });
      }

      let teams = [];
      if (this.$refs.gamificationTeams && this.$refs.gamificationTeams.teams && this.$refs.gamificationTeams.teams.length) {
        teams = this.$refs.gamificationTeams.teams;
      }

      let computedRecipientsCount = 0;

      // Compute valid members, 'computed' recipients count, total points per team
      teams.forEach((team) => {
        this.$set(team, 'validMembersWallets', []);
        this.$set(team, 'computedBudget', 0);
        this.$set(team, 'fixedBudget', 0);
        this.$set(team, 'totalValidPoints', 0);
        this.$set(team, 'totalPoints', 0);
        this.$set(team, 'notEnoughRemainingBudget', false);
        this.$set(team, 'exceedingBudget', false);

        if (!team.members || !team.members.length) {
          return;
        }

        team.members.forEach((memberObject) => {
          let wallet = this.validRecipients.find((wallet) => wallet.id === memberObject.id);
          if (wallet) {
            team.totalPoints += wallet.points;
            if(wallet.address && wallet.points && wallet.points >= this.threshold) {
              team.totalValidPoints += wallet.points;
              team.validMembersWallets.push(wallet);
            }
          } else {
            wallet = memberObject.points ? memberObject : this.identitiesList.find((wallet) => wallet.id === memberObject.id);
            if (wallet) {
              team.totalPoints += wallet.points;
            }
          }
        });

        if (team.rewardType === 'COMPUTED') {
          computedRecipientsCount += team.validMembersWallets.length;
        }
      });

      let fixedGlobalBudget = 0;
      let computedTotalBudget = 0;
      this.computedTeamsBudget = 0;

      if (this.rewardType === 'FIXED') {
        fixedGlobalBudget = computedTotalBudget = this.totalBudget ? Number(this.totalBudget) : 0;
      } else if (this.rewardType === 'FIXED_PER_MEMBER') {
        computedTotalBudget = this.budgetPerMember && computedRecipientsCount ? computedRecipientsCount * this.budgetPerMember : 0;
      }

      teams.forEach((team) => {
        if (team.validMembersWallets.length) {
          if (team.rewardType === 'FIXED') {
            this.$set(team, 'fixedBudget', team.budget ? Number(team.budget) : 0);
            this.$set(team, 'computedBudget', team.fixedBudget);
            computedTotalBudget -= team.fixedBudget;
            this.computedTeamsBudget += team.fixedBudget;
          } else if (team.rewardType === 'FIXED_PER_MEMBER') {
            this.$set(team, 'fixedBudget', team.rewardPerMember ? Number(team.rewardPerMember) * team.validMembersWallets.length : 0);
            this.$set(team, 'computedBudget', team.fixedBudget);
            computedTotalBudget -= team.fixedBudget;
            this.computedTeamsBudget += team.fixedBudget;
          }
        }
      });

      this.computedTeamsBudget += computedRecipientsCount > 0 && computedTotalBudget > 0 ? computedTotalBudget : 0;

      const tokenPerRecipient = computedRecipientsCount > 0 && computedTotalBudget > 0 ? computedTotalBudget / computedRecipientsCount : 0;

      teams.forEach((team) => {
        if (!team.validMembersWallets || !team.validMembersWallets.length) {
          return;
        }

        let budget = 0;
        if (team.rewardType === 'FIXED' || team.rewardType === 'FIXED_PER_MEMBER') {
          budget = team.fixedBudget ? Number(team.fixedBudget) : 0;
          team.exceedingBudget = fixedGlobalBudget <= budget;
        } else if (team.rewardType === 'COMPUTED') {
          if (tokenPerRecipient > 0) {
            budget = team.computedBudget = tokenPerRecipient * team.validMembersWallets.length;
          } else {
            team.notEnoughRemainingBudget = true;
          }
        }

        if (budget && budget > 0) {
          // Estimate budget per member
          team.validMembersWallets.forEach((wallet) => {
            const tokensToSend = (budget * wallet.points) / team.totalValidPoints;
            this.$set(wallet, 'tokensToSend', this.toFixed(tokensToSend));
          });
        }
      });
    },
    loadAll() {
      if (!this.selectedDate || !this.periodType) {
        return;
      }
      getPeriodRewardDates(new Date(this.selectedDate), this.periodType).then((period) => {
        this.selectedStartDate = this.formatDate(new Date(period.startDateInSeconds * 1000));
        this.selectedEndDate = this.formatDate(new Date(period.endDateInSeconds * 1000));
        return this.loadGamificationList();
      });
    },
    loadGamificationList() {
      this.$emit('list-retrieved');
      this.error = null;
      this.identitiesList = [];
      this.loading = true;
      const identitiesListPromises = [];
      if (this.wallets && this.wallets.length) {
        const retrievedIdentities = [];
        this.wallets.forEach((wallet) => {
          if (!wallet || wallet.type !== 'user' || retrievedIdentities.indexOf(wallet.id) >= 0) {
            return;
          }
          retrievedIdentities.push(wallet.id);
          wallet = Object.assign({}, wallet);
          wallet.hash = null;
          wallet.status = null;
          wallet.tokensToSend = 0;

          const startDate = new Date(this.selectedStartDate);
          const endDate = new Date(this.selectedEndDate);
          identitiesListPromises.push(
            getGamificationPoints(wallet.id, startDate, endDate).then((object) => {
              wallet.points = object && object.points;
              wallet.url = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/profile/${wallet.id}`;
              this.identitiesList.push(wallet);
            })
          );
        });
      }

      return Promise.all(identitiesListPromises)
        .then(() => this.refresh())
        .then(() => this.$refs.sendRewards && this.$refs.sendRewards.refreshWalletTransactionStatus())
        .then(() => {
          this.loading = false;
        });
    },
    formatDate(date) {
      if (!date) {
        return null;
      }
      const dateString = date.toString();
      // Example: 'Feb 01 2018'
      return dateString.substring(dateString.indexOf(' ') + 1, dateString.indexOf(':') - 3);
    },
    successTransaction(transaction, receipt) {
      if(transaction && transaction.to) {
        const wallet = this.identitiesList.find(wallet => wallet.address && wallet.address.toLowerCase() === transaction.to.toLowerCase());
        if(wallet) {
          this.$set(wallet, 'hash', transaction.hash);
          this.$set(wallet, 'status', receipt && receipt.status ? 'success' : 'error');
          if(transaction.tokensAmountSent || transaction.contractAmount) {
            this.$set(wallet, 'tokensSent', transaction.tokensAmountSent ? transaction.tokensAmountSent : transaction.contractAmount);
          }
        }
      }
    },
    pendingTransaction(transaction) {
      if(transaction && transaction.to) {
        this.$emit('pending', transaction);
        const wallet = this.identitiesList.find(wallet => wallet.address && wallet.address.toLowerCase() === transaction.to.toLowerCase());
        if(wallet) {
          this.$set(wallet, 'hash', transaction.hash);
          this.$set(wallet, 'status', 'pending');
          this.$set(wallet, 'tokensSent', transaction.tokensAmountSent ? transaction.tokensAmountSent : transaction.contractAmount);
        }
      }
    },
  },
};
</script>
