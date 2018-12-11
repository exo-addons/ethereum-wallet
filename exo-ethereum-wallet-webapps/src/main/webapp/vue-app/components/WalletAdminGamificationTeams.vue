<template>
  <div>
    <h3 class="text-xs-left ml-3">
      Reward pools
      <add-team-modal
        ref="teamModal"
        :team="selectedTeam"
        :wallets="wallets"
        @closed="selectedTeam = null"
        @saved="refresh" />
    </h3>
    <v-container fluid grid-list-md>
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
          <v-card :style="props.item.spacePrettyName && `background: url(/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/banner)  0 0/100% auto no-repeat`">
            <v-card flat class="transparent">
              <v-card-title :class="props.item.description && 'pb-0'">
                <v-chip dark>
                  <v-avatar v-if="props.item.spacePrettyName">
                    <img :src="`/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/avatar`">
                  </v-avatar>
                  <h3 class="headline">{{ props.item.name }}</h3>
                </v-chip>
              </v-card-title>
              <v-card-title v-if="props.item.description" class="pt-0">
                <v-chip dark>
                  <h4>
                    {{ props.item.description }}
                  </h4>
                </v-chip>
              </v-card-title>
              <v-divider></v-divider>
              <v-list dense>
                <v-list-tile>
                  <v-list-tile-content>Members:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ props.item.members ? props.item.members.length : 0 }}</v-list-tile-content>
                </v-list-tile>
                <v-list-tile v-if="props.item.budget">
                  <v-list-tile-content>Fixed budget:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ props.item.budget }}</v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>Computed budget:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ Number(toFixed(props.item.computedBudget)) }}</v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>Total valid points:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ props.item.totalValidPoints }}</v-list-tile-content>
                </v-list-tile>
              </v-list>
            </v-card>
            <v-card-actions>
              <v-spacer />
              <v-btn v-if="props.item.id" flat color="primary" @click="selectedTeam = props.item;">Edit</v-btn>
              <v-btn v-if="props.item.id" flat color="primary" @click="removeTeam(props.item.id)">Delete</v-btn>
              <v-spacer />
            </v-card-actions>
          </v-card>
        </v-flex>
      </v-data-iterator>
    </v-container>
  </div>
</template>

<script>
import AddTeamModal from './WalletAdminGamificationAddTeamModal.vue';

import {getTeams, removeTeam} from '../WalletGamificationServices.js';

export default {
  components: {
    AddTeamModal
  },
  props: {
    wallets: {
      type: Array,
      default: function() {
        return [];
      }
    }
  },
  data: () => ({
    teams: [],
    teamsRetrieved: false,
    selectedTeam: null
  }),
  created() {
    this.refresh();
  },
  methods: {
    refresh() {
      getTeams()
        .then(teams => {
          if(teams && teams.length) {
            teams = teams.sort((team1, team2) => team2.id - team1.id);
          }
          this.teams = teams;
          this.teamsRetrieved = true;
          this.$emit('teams-retrieved');
        })
        .catch(e => {
          console.debug('Error getting teams list', e);
          this.error = 'Error getting teams list, please contact your administrator';
        });
    },
    removeTeam(id) {
      removeTeam(id)
        .then(status => {
          if(status) {
            this.refresh();
          } else {
            this.error = 'Error removing team';
          }
        })
        .catch(e => {
          console.debug('Error getting team with id', id, e);
          this.error = 'Error removing team';
        });
    }
  }
};
</script>