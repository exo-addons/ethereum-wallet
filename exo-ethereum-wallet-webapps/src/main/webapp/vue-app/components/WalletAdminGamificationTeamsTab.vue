<template>
  <div>
    <v-dialog
      v-model="removeTeamConfirm"
      content-class="uiPopup"
      width="290px"
      max-width="100vw"
      @keydown.esc="removeTeamConfirm = false">
      <v-card class="elevation-12">
        <div class="popupHeader ClearFix">
          <a
            class="uiIconClose pull-right"
            aria-hidden="true"
            @click="removeTeamConfirm = false"></a> <span class="PopupTitle popupTitle">
              Delete pool confirmation
            </span>
        </div>
        <v-card-text>
          Would you like to delete pool <strong>
            {{ teamToDelete && teamToDelete.name }}
          </strong>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <button
            :disabled="loading"
            :loading="loading"
            class="btn btn-primary mr-2"
            @click="removeTeam(teamToDelete.id)">
            Delete
          </button> <button
            :disabled="loading"
            :loading="loading"
            class="btn ml-2"
            @click="removeTeamConfirm = false">
            Cancel
          </button>
          <v-spacer />
        </v-card-actions>
      </v-card>
    </v-dialog>
    <add-team-form
      v-show="selectedTeam"
      ref="teamModal"
      :team="selectedTeam"
      :wallets="wallets"
      @saved="refresh"
      @close="selectedTeam = null" />
    <h3
      v-show="!selectedTeam"
      id="addTeamButton"
      class="text-xs-left ml-3">
      <v-btn
        title="Add a new pool"
        color="primary"
        class="btn btn-primary"
        icon
        large
        @click="selectedTeam = {}">
        <v-icon>
          add
        </v-icon>
      </v-btn>
    </h3>
    <h4 v-show="!selectedTeam">
      Total budget: <strong>
        {{ computedTotalBudget }} {{ symbol }}
      </strong>
            <template v-if="rewardType === 'FIXED'">
              ({{ eligibleUsersCount }} eligible users)
            </template>
    </h4>
    <h4 v-show="!selectedTeam">
      Configured budget: <strong>
        {{ configuredBudget }} {{ symbol }}
      </strong>
                 <template v-if="rewardType === 'FIXED_PER_MEMBER'">
                   ({{ budgetPerMember }} {{ symbol }} per eligible user with {{ eligibleUsersCount }} eligible users)
                 </template>
    </h4>
    <v-container
      v-show="!selectedTeam"
      fluid
      grid-list-md>
      <v-data-iterator
        :items="teams"
        content-tag="v-layout"
        no-data-text=""
        row
        wrap
        hide-actions>
        <v-flex
          slot="item"
          slot-scope="props"
          xs12
          sm6
          md4
          lg3>
          <v-card :style="props.item.spacePrettyName && `background: url(/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/banner)  0 0/100% auto no-repeat`" class="elevation-3">
            <v-card flat class="transparent">
              <v-card-title :class="props.item.description && 'pb-0'">
                <v-chip dark>
                  <v-avatar v-if="props.item.spacePrettyName">
                    <img :src="`/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/avatar`">
                  </v-avatar>
                  <h3 class="headline">
                    {{ props.item.name }}
                  </h3>
                </v-chip>
              </v-card-title>
              <v-card-title v-if="props.item.description" class="pt-0">
                <v-chip dark>
                  <h4>
                    {{ props.item.description }}
                  </h4>
                </v-chip>
              </v-card-title>
              <v-divider />
              <v-list dense class="pb-0">
                <v-list-tile>
                  <v-list-tile-content>
                    Members:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.members ? props.item.members.length : 0 }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'FIXED'">
                  <v-list-tile-content>
                    Fixed total budget:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.budget }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'FIXED_PER_MEMBER'">
                  <v-list-tile-content>
                    Fixed budget per member:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ Number(toFixed(props.item.rewardPerMember)) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.rewardType === 'COMPUTED'">
                  <v-list-tile-content>
                    Budget:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    Computed
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-flex class="align-start pr-1">
                    <v-divider />
                  </v-flex>
                  <v-flex
                    class="align-center">
                    <strong>
                      {{ period }}
                    </strong>
                  </v-flex>
                  <v-flex class="align-end pl-1">
                    <v-divider />
                  </v-flex>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Eligible members:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.validMembersWallets ? props.item.validMembersWallets.length : 0 }} / {{ props.item.members ? props.item.members.length : 0 }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Eligible/Total earnings:
                  </v-list-tile-content>
                  <v-list-tile-content class="align-end">
                    {{ props.item.totalValidPoints ? props.item.totalValidPoints : 0 }} / {{ props.item.totalPoints ? props.item.totalPoints : 0 }} points
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Budget:
                  </v-list-tile-content>
                  <v-list-tile-content
                    v-if="!Number(props.item.computedBudget) || !props.item.validMembersWallets || !props.item.validMembersWallets.length"
                    class="align-end red--text">
                    <strong>
                      0 {{ symbol }}
                    </strong>
                  </v-list-tile-content>
                  <v-list-tile-content v-else class="align-end">
                    {{ Number(toFixed(props.item.computedBudget)) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>
                    Budget per member:
                  </v-list-tile-content>
                  <v-list-tile-content
                    v-if="!Number(props.item.computedBudget) || !props.item.validMembersWallets || !props.item.validMembersWallets.length"
                    class="align-end red--text">
                    <strong>
                      0 {{ symbol }}
                    </strong>
                  </v-list-tile-content>
                  <v-list-tile-content v-else class="align-end">
                    {{ toFixed(Number(props.item.computedBudget) / props.item.validMembersWallets.length) }} {{ symbol }}
                  </v-list-tile-content>
                </v-list-tile>

                <v-list-tile v-if="props.item.notEnoughRemainingBudget" class="teamCardWarning">
                  <v-list-tile-content>
                    <div class="alert alert-warning">
                      <i class="uiIconWarning"></i> No remaining budget for this pool, please review :
                      <ul>
                        <li>
                          - Total budget allowed in global configuration
                        </li>
                        <li>
                          - <strong>
                            Fixed
                          </strong> budget allowed for other pools.
                        </li>
                      </ul>
                    </div>
                  </v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-else-if="props.item.exceedingBudget" class="teamCardWarning">
                  <v-list-tile-content>
                    <div class="alert alert-warning">
                      <i class="uiIconWarning"></i> The pool total budget exceeds the global total budget.
                    </div>
                  </v-list-tile-content>
                </v-list-tile>
              </v-list>
            </v-card>
            <v-card-actions>
              <v-spacer />
              <v-btn
                v-if="props.item.id"
                flat
                color="primary"
                @click="selectedTeam = props.item">
                Edit
              </v-btn>
              <v-btn
                v-if="props.item.id"
                flat
                color="primary"
                @click="
                  teamToDelete = props.item;
                  removeTeamConfirm = true;
                ">
                Delete
              </v-btn>
              <v-spacer />
            </v-card-actions>
          </v-card>
        </v-flex>
      </v-data-iterator>
    </v-container>
  </div>
</template>

<script>
import AddTeamForm from './WalletAdminGamificationAddTeamForm.vue';

import {getTeams, removeTeam} from '../WalletGamificationServices.js';

export default {
  components: {
    AddTeamForm,
  },
  props: {
    wallets: {
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
    rewardType: {
      type: String,
      default: function() {
        return null;
      },
    },
    period: {
      type: String,
      default: function() {
        return null;
      },
    },
    budgetPerMember: {
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
    computedTotalBudget: {
      type: Number,
      default: function() {
        return 0;
      },
    },
  },
  data: () => ({
    teams: [],
    teamToDelete: null,
    removeTeamConfirm: false,
    teamsRetrieved: false,
    selectedTeam: null,
  }),
  computed: {
    symbol() {
      return this.contractDetails && this.contractDetails.symbol ? this.contractDetails.symbol : '';
    },
    eligibleUsersCount() {
      return this.wallets ? this.wallets.length : 0;
    },
    configuredBudget() {
      if (this.rewardType === 'FIXED') {
        return this.totalBudget;
      } else if (this.rewardType === 'FIXED_PER_MEMBER') {
        return this.budgetPerMember * this.eligibleUsersCount;
      }
      return 0;
    },
  },
  watch: {
    selectedTeam() {
      if (this.selectedTeam) {
        this.$emit('form-opened');
      } else {
        this.$emit('form-closed');
      }
    },
  },
  created() {
    this.refresh();
  },
  methods: {
    refresh() {
      this.selectedTeam = null;
      getTeams()
        .then((teams) => {
          this.teams = teams;
          this.teamsRetrieved = true;
          this.$emit('teams-retrieved');
        })
        .catch((e) => {
          console.debug('Error getting teams list', e);
          this.error = 'Error getting teams list, please contact your administrator';
        });
    },
    removeTeam(id) {
      removeTeam(id)
        .then((status) => {
          if (status) {
            this.removeTeamConfirm = false;
            this.teamToDelete = null;
            return this.refresh();
          } else {
            this.error = 'Error removing team';
          }
        })
        .catch((e) => {
          console.debug('Error getting team with id', id, e);
          this.error = 'Error removing team';
        });
    },
  },
};
</script>
