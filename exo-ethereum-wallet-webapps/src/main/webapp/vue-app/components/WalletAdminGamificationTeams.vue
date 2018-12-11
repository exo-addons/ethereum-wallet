<template>
  <div>
    <h3 class="text-xs-left ml-3">
      Reward pools
      <add-team-modal
        ref="teamModal"
        :team="selectedTeam"
        :wallets="wallets"
        @closed="selectedTeam = null"
        @saved="addTeam" />
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
          <v-card>
            <v-img
              v-if="props.item.spacePrettyName && !props.item.imageError"
              :src="`/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/banner`"
              @error="$set(props.item, 'imageError', true)" />
            <v-card flat>
              <v-card-title :class="props.item.description && 'pb-0'">
                <h3 class="headline mb-0">
                  <v-avatar v-if="props.item.spacePrettyName">
                    <img :src="`/portal/rest/v1/social/spaces/${props.item.spacePrettyName}/avatar`">
                  </v-avatar>
                  {{ props.item.name }}
                </h3>
              </v-card-title>
              <v-card-title v-if="props.item.description" class="pt-0">
                <h4>
                  {{ props.item.description }}
                </h4>
              </v-card-title>
              <v-divider></v-divider>
              <v-list dense>
                <v-list-tile>
                  <v-list-tile-content>Members:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ props.item.members ? props.item.members.length : 0 }}</v-list-tile-content>
                </v-list-tile>
                <v-list-tile>
                  <v-list-tile-content>Budget:</v-list-tile-content>
                  <v-list-tile-content class="align-end">{{ props.item.budget }}</v-list-tile-content>
                </v-list-tile>
              </v-list>
            </v-card>
            <v-card-actions>
              <v-spacer />
              <v-btn flat color="primary" @click="selectedTeam = props.item;">Edit</v-btn>
              <v-btn flat color="primary">Delete</v-btn>
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

import {getTeams} from '../WalletGamificationServices.js';

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
    selectedTeam: null
  }),
  created() {
    this.refresh();
  },
  methods: {
    refresh() {
      getTeams()
        .then(teams => {
          this.teams = teams;
        })
        .catch(e => {
          console.debug('Error getting teams list', e);
          this.error = 'Error getting teams list, please contact your administrator';
        });
    },
    addTeam(addedTeam) {
      if(addedTeam) {
        const teamIndex = this.teams.findIndex(team => addedTeam.id === team.id);
        if(teamIndex >= 0) {
          this.teams.splice(teamIndex, 1, addedTeam);
        } else {
          this.teams.push(addedTeam);
        }
      }
    }
  }
};
</script>