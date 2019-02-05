<template>
  <v-app
    id="RewardApp"
    color="transaprent"
    flat>
    <main>
      <v-layout column>
        <v-flex>
          <v-card class="applicationToolbar mb-3" flat>
            <v-card-text class="pt-2 pb-2">
              <strong>Reward administration</strong>
            </v-card-text>
          </v-card>
        </v-flex>
        <v-flex class="white text-xs-center" flat>
          <div v-if="error && !loading" class="alert alert-error v-content">
            <i class="uiIconError"></i>{{ error }}
          </div>

          <wallet-setup
            ref="walletSetup"
            :wallet-address="originalWalletAddress"
            :loading="loading"
            is-administration
            @refresh="init()"
            @error="error = $event" />

          <v-dialog
            v-model="loading"
            persistent
            width="300">
            <v-card color="primary" dark>
              <v-card-text>
                Loading ...
                <v-progress-linear
                  indeterminate
                  color="white"
                  class="mb-0" />
              </v-card-text>
            </v-card>
          </v-dialog>

          <v-flex v-if="isContractDifferentFromPrincipal" class="text-xs-center">
            <div class="alert alert-warning">
              <i class="uiIconWarning"></i> You have chosen a token that is different from principal displayed token
            </div>
          </v-flex>

          <v-tabs v-model="selectedTab" grow>
            <v-tabs-slider color="primary" />
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
              <send-rewards-tab
                ref="sendRewards"
                :wallets="wallets"
                :wallet-address="walletAddress"
                :contract-details="contractDetails"
                :period-type="rewardSettings.periodType"
                :transaction-etherscan-link="transactionEtherscanLink"
                :total-budget="totalBudget"
                :sent-budget="sentBudget"
                :eligible-users-count="eligibleUsersCount"
                @dates-changed="refreshRewards"
                @pending="pendingTransaction"
                @success="successTransaction"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item id="RewardPools">
              <teams-list-tab
                ref="rewardTeams"
                :wallets="wallets"
                :contract-details="contractDetails"
                :period="periodDatesDisplay"
                :total-budget="totalBudget"
                :sent-budget="sentBudget"
                :eligible-users-count="eligibleUsersCount"
                @teams-refreshed="refreshRewardSettings"
                @refresh="refreshRewards"
                @error="error = $event" />
            </v-tab-item>
            <v-tab-item id="Configuration">
              <configuration-tab
                ref="configurationTab"
                :contracts="contracts"
                @saved="refreshRewardSettings"
                @error="error = $event" />
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import SendRewardsTab from './SendRewardsTab.vue';
import TeamsListTab from './TeamsListTab.vue';
import ConfigurationTab from './ConfigurationTab.vue';

import WalletSetup from '../WalletSetup.vue';

import * as constants from '../../WalletConstants.js';

import {getRewardSettings, computeRewards} from '../../WalletRewardServices.js';
import {getContractsDetails} from '../../WalletToken.js';
import {initWeb3, initSettings, getWallets, getTransactionEtherscanlink, getAddressEtherscanlink} from '../../WalletUtils.js';

export default {
  components: {
    SendRewardsTab,
    TeamsListTab,
    ConfigurationTab,
    WalletSetup,
  },
  data() {
    return {
      loading: false,
      selectedTab: null,
      transactionEtherscanLink: null,
      addressEtherscanLink: null,
      contractDetails: null,
      selectedStartDate: null,
      selectedEndDate: null,
      periodType: null,
      networkId: null,
      walletAddress: null,
      originalWalletAddress: null,
      duplicatedWallets: [],
      rewardSettings: {},
      teams: [],
      wallets: [],
      contracts: [],
    };
  },
  computed: {
    validUsers() {
      return this.wallets.filter(wallet => wallet.enabled && !wallet.disabled);
    },
    eligibleUsersCount() {
      return this.validUsers.filter(wallet =>  wallet.tokensToSend || wallet.tokensSent).length;
    },
    sentBudget() {
      if (this.wallets && this.wallets.length) {
        let sentTokens = 0;
        this.wallets.forEach((wallet) => {
          sentTokens += (Number(wallet.tokensSent) || 0);
        });
        return sentTokens;
      } else {
        return 0;
      }
    },
    totalBudget() {
      if (this.wallets && this.wallets.length) {
        let totalBudget = 0;
        this.wallets.forEach((wallet) => {
          totalBudget += (Number(wallet.tokensSent) || Number(wallet.tokensToSend) || 0);
        });
        return totalBudget;
      } else {
        return 0;
      }
    },
  },
  created() {
    this.init()
      .then(() => (this.transactionEtherscanLink = getTransactionEtherscanlink(this.networkId)))
      .then(() => (this.addressEtherscanLink = getAddressEtherscanlink(this.networkId)));
  },
  methods: {
    init() {
      this.loading = true;
      return initSettings()
        .then(() => {
          if (!window.walletSettings) {
            throw new Error('Wallet settings are empty for current user');
          }
        })
        .then(() => initWeb3(false, true))
        .catch((error) => {
          if (String(error).indexOf(constants.ERROR_WALLET_NOT_CONFIGURED) < 0) {
            console.debug('Error connecting to network', error);
            this.error = 'Error connecting to network';
          } else {
            this.error = 'Please configure your wallet';
            throw error;
          }
        })
        .then(() => {
          this.walletAddress = window.localWeb3 && window.localWeb3.eth.defaultAccount && window.localWeb3.eth.defaultAccount.toLowerCase();
          this.originalWalletAddress = window.walletSettings.userPreferences.walletAddress;
          this.networkId = window.walletSettings.currentNetworkId;

          return getContractsDetails(this.walletAddress, this.networkId, true, true);
        })
        .then((contracts) => (this.contracts = contracts ? contracts.filter((contract) => contract.isDefault) : []))
        .then(() => getWallets())
        .then((wallets) => {
          this.wallets = wallets ? wallets.filter(wallet => wallet && wallet.address && wallet.enabled && wallet.type === 'user') : [];
        })
        .then(() => this.refreshRewardSettings())
        .catch((e) => {
          console.debug('init method - error', e);
          this.error = `Error encountered: ${e}`;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshRewardSettings() {
      this.loading = true;
      return getRewardSettings()
        .then(settings => {
          this.rewardSettings = settings;
          if(this.rewardSettings) {
            if (this.contracts && this.contracts.length && this.rewardSettings.contractAddress) {
              const contractAddress = this.rewardSettings.contractAddress.toLowerCase();
              this.contractDetails = this.contracts.find(contract  => contract && contract.address && contract.address.toLowerCase() === contractAddress);
            }
            this.periodType = this.rewardSettings.periodType;
          }
          this.$refs.configurationTab.init();
          return this.$nextTick();
        })
        .then(() => this.$refs.rewardTeams.refreshTeams())
        .then(() => this.$refs.sendRewards.refreshDates())
        .then(() => this.$nextTick())
        .then(() => this.refreshRewards())
        .finally(() => this.loading = false);
    },
    refreshRewards() {
      this.loading = true;
      const teams = this.$refs.rewardTeams.teams;

      this.duplicatedWallets = [];

      // Check enabled/disabled wallets
      this.wallets.forEach((wallet) => wallet.disabled = false);
      teams.forEach((team) => {
        if (team.id && team.members) {
          team.members.forEach((memberObject) => {
            const wallet = this.wallets.find((wallet) => wallet && wallet.id && wallet.technicalId === memberObject.identityId);
            if (wallet) {
              wallet.disabled = team.disabled;
            }
          });
        }
      });

      const validWallets = this.wallets.filter(wallet => wallet.enabled && !wallet.disabled);
      return this.$nextTick()
        .then(() => {
          const identityIds = validWallets.map(wallet => wallet.technicalId);
          return computeRewards(identityIds, this.$refs.sendRewards.selectedDateInSeconds)
        })
        .then(rewardDetails => {
          if(rewardDetails.error) {
            this.error = rewardDetails.error;
            return;
          }
          this.wallets.forEach(wallet => {
            wallet.rewards = [];
            wallet.tokensToSend = 0;
            delete wallet.rewardTeams;
          });

          validWallets.forEach(wallet => {
            wallet.rewards = rewardDetails.filter(rewardDetail => rewardDetail.identityId === wallet.technicalId);
            wallet.tokensToSend = wallet.rewards.reduce((sum, reward) => sum + reward.amount, 0);
            delete wallet.rewardTeams;
          });

          teams.forEach((team) => {
            team.validMembersWallets = [];
            team.computedBudget = 0;

            if (team.id && team.members) {
              team.members.forEach((memberObject) => {
                const wallet = this.wallets.find((wallet) => wallet && wallet.id && wallet.technicalId === memberObject.identityId);
                if (wallet) {
                  if (wallet.rewardTeams && wallet.rewardTeams.length) {
                    wallet.rewardTeams.push(team);
                    this.duplicatedWallets.push(wallet);
                  } else {
                    this.$set(wallet, 'rewardTeams', [team]);
                  }
                  if(wallet.tokensSent || wallet.tokensToSend) {
                    team.validMembersWallets.push(wallet);
                  }
                  wallet.disabled = team.disabled;
                  if(wallet.disabled) {
                    wallet.rewards = [];
                    wallet.tokensToSend = 0;
                  }
                }
              });
            }
          });

          const membersWithEmptyTeam = this.wallets.filter((wallet) => !wallet.rewardTeams || !wallet.rewardTeams.length);
          if (membersWithEmptyTeam && membersWithEmptyTeam.length) {
            const validMembersWallets = [];
            membersWithEmptyTeam.forEach(wallet => {
              if(wallet.tokensSent || wallet.tokensToSend) {
                validMembersWallets.push(wallet);
              }
            });

            // Members with no Team
            let noTeamMembers = teams.find(team => !team.id);
            if(!noTeamMembers) {
              noTeamMembers = {
                id: 0,
                name: 'No pool users',
                description: 'Users with no associated pool',
                rewardType: 'COMPUTED',
                computedBudget: 0,
              };
              teams.push(noTeamMembers);
            }
            noTeamMembers.members = membersWithEmptyTeam;
            noTeamMembers.validMembersWallets = validMembersWallets;
          }

          if (teams && teams.length) {
            teams.sort((team1, team2) => Number(team1.id) - Number(team2.id));
          }

          teams.forEach((team) => {
            if (team.validMembersWallets && team.validMembersWallets.length) {
              team.computedBudget = team.validMembersWallets.reduce((total, wallet) => total += wallet.tokensSent || wallet.tokensToSend || 0, 0);
            }
          });
        })
        .finally(() => this.loading = false);
    },
  },
};
</script>
