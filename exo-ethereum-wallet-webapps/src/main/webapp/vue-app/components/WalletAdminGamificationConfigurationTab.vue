<template>
  <v-card flat>
    <v-card-text>
      <div class="text-xs-left gamificationWalletConfiguration">
        <span>Minimal gamification points threshold to reward users: </span>
        <v-text-field v-model.number="selectedThreshold" :disabled="!configurationEditable" name="threshold" class="input-text-center" />
      </div>
      <div class="text-xs-left gamificationWalletConfiguration">
        <span>Reward periodicity: </span>
        <div id="selectedPeriodType" class="selectBoxVuetifyParent v-input">
          <v-combobox v-model="selectedPeriodType" :disabled="!configurationEditable" :items="periods" :return-object="false" attach="#selectedPeriodType" class="selectBoxVuetify" hide-no-data hide-selected small-chips>
            <template slot="selection">
              {{ selectedPeriodName }}
            </template>
          </v-combobox>
        </div>
      </div>
      <div class="text-xs-left mt-4">
        <div>The total gamification reward buget is set:</div>
        <v-flex class="ml-4">
          <v-radio-group v-model="selectedRewardType">
            <v-radio :disabled="!configurationEditable" value="FIXED" label="By a fixed budget of" />
            <v-flex v-if="selectedRewardType === 'FIXED'" class="gamificationWalletConfiguration mb-2">
              <v-text-field v-model.number="selectedTotalBudget" :disabled="!configurationEditable" placeholder="Enter the fixed total budget" type="number" class="pt-0 pb-0" name="totalBudget" />
              <span> using token </span>
              <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
                <v-combobox v-model="selectedContractAddress" :disabled="!configurationEditable" :items="contracts" :return-object="false" attach="#selectedContractAddress" item-value="address" item-text="name" class="selectBoxVuetify" hide-no-data hide-selected small-chips>
                  <!-- Without slot-scope, the template isn't displayed -->
                  <!-- eslint-disable-next-line vue/no-unused-vars -->
                  <template slot="selection" slot-scope="data">
                    {{ selectedContractName }}
                  </template>
                </v-combobox>
              </div>
            </v-flex>
            <v-radio :disabled="!configurationEditable" value="FIXED_PER_MEMBER" label="By a fixed budget per eligible member on period of" />
            <v-flex v-if="selectedRewardType === 'FIXED_PER_MEMBER'" class="gamificationWalletConfiguration mb-2">
              <v-text-field v-model="selectedBudgetPerMember" :disabled="!configurationEditable" placeholder="Enter the fixed budget per eligible member on period" type="number" class="pt-0 pb-0" name="budgetPerMember" />
              <span> using token </span>
              <div id="selectedContractAddress" class="selectBoxVuetifyParent v-input">
                <v-combobox v-model="selectedContractAddress" :disabled="!configurationEditable" :items="contracts" :return-object="false" attach="#selectedContractAddress" item-value="address" item-text="name" class="selectBoxVuetify" hide-no-data hide-selected small-chips>
                  <!-- Without slot-scope, the template isn't displayed -->
                  <!-- eslint-disable-next-line vue/no-unused-vars -->
                  <template slot="selection" slot-scope="data">
                    {{ selectedContractName }}
                  </template>
                </v-combobox>
              </div>
            </v-flex>
          </v-radio-group>
        </v-flex>
      </div>
    </v-card-text>
    <v-card-actions>
      <v-spacer />
      <v-btn v-if="configurationEditable" :loading="loadingSettings" class="btn btn-primary ml-2" dark @click="save"> Save </v-btn>
      <v-btn v-else class="btn btn-primary ml-2" dark @click="configurationEditable = true"> Edit </v-btn>
      <v-spacer />
    </v-card-actions>
  </v-card>
</template>

<script>
import {saveSettings} from '../WalletGamificationServices.js';

export default {
  props: {
    contracts: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
  data() {
    return {
      loadingSettings: false,
      configurationEditable: false,
      selectedContractAddress: null,
      selectedThreshold: null,
      selectedBudgetPerMember: null,
      selectedRewardType: 'FIXED',
      selectedTotalBudget: null,
      selectedPeriodType: null,
      periods: [
        {
          text: 'Week',
          value: 'WEEK',
        },
        {
          text: 'Month',
          value: 'MONTH',
        },
        {
          text: 'Quarter',
          value: 'QUARTER',
        },
        {
          text: 'Semester',
          value: 'SEMESTER',
        },
        {
          text: 'Year',
          value: 'YEAR',
        },
      ],
    };
  },
  computed: {
    selectedPeriodName() {
      const selectedPeriodName = this.periods.find((period) => period.value === this.selectedPeriodType);
      return selectedPeriodName && selectedPeriodName.text ? selectedPeriodName.text : this.selectedPeriodType && (this.selectedPeriodType.text || this.selectedPeriodType);
    },
    selectedContractDetails() {
      return this.selectedContractAddress && this.contracts && this.contracts.find((contract) => contract.address === this.selectedContractAddress);
    },
    selectedContractName() {
      return this.selectedContractDetails && this.selectedContractDetails.name;
    },
  },
  methods: {
    init(settings) {
      if (settings) {
        this.selectedContractAddress = settings.contractAddress;
        this.selectedRewardType = settings.rewardType;
        this.selectedBudgetPerMember = this.budgetPerMember = settings.budgetPerMember;
        this.selectedTotalBudget = this.totalBudget = settings.totalBudget;
        this.selectedThreshold = this.threshold = settings.threshold;
        this.selectedPeriodType = this.periodType = settings.periodType;
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
          contractAddress: thiss.selectedContractAddress,
          periodType: thiss.selectedPeriodType && (thiss.selectedPeriodType.value || thiss.selectedPeriodType),
        })
          .then((settings) => {
            if (settings) {
              this.init(settings);
              thiss.configurationEditable = false;
              return this.$emit('saved', {
                contractAddress: settings.contractAddress,
                periodType: settings.periodType,
                threshold: settings.threshold,
                rewardType: settings.rewardType,
                totalBudget: settings.totalBudget,
                budgetPerMember: settings.budgetPerMember,
              });
            } else {
              throw new Error("Settings aren't loaded from REST call", settings);
            }
          })
          .catch((error) => {
            console.debug("Error while saving 'Gamification settings'", error);
            thiss.$emit('error', "Error while saving 'Gamification settings'");
          })
          .finally(() => {
            thiss.loadingSettings = false;
          });
      }, 200);
    },
  },
};
</script>
