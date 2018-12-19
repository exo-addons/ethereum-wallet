<template>
  <v-dialog
    v-model="dialog"
    :disabled="disabled"
    content-class="uiPopup"
    width="600px"
    max-width="100vw"
    persistent
    @keydown.esc="dialog = false">
    <button
      slot="activator"
      :disabled="disabled"
      :value="true"
      class="btn btn-primary mt-1 mb-1"
      @click="dialog = true">
      Send rewards
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a
          class="uiIconClose pull-right"
          aria-hidden="true"
          @click="dialog = false"></a> <span class="PopupTitle popupTitle">
            Send rewards
          </span>
      </div>
      <send-reward-form
        ref="sendRewardForm"
        :recipients="recipients"
        :contract-details="contractDetails"
        :period-type="periodType"
        :start-date-in-seconds="startDateInSeconds"
        :end-date-in-seconds="endDateInSeconds"
        :default-transaction-label="defaultTransactionLabel"
        :default-transaction-message="defaultTransactionMessage"
        :reward-count-field="rewardCountField"
        :reward-type="rewardType"
        class="pt-4"
        @sent="$emit('sent', $event, contractDetails)"
        @close="dialog = false"
        @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendRewardForm from './SendRewardForm.vue';

export default {
  components: {
    SendRewardForm,
  },
  props: {
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      },
    },
    recipients: {
      type: Array,
      default: function() {
        return [];
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
    defaultTransactionLabel: {
      type: String,
      default: function() {
        return null;
      },
    },
    defaultTransactionMessage: {
      type: String,
      default: function() {
        return null;
      },
    },
    rewardCountField: {
      type: String,
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
  },
  data() {
    return {
      dialog: false,
    };
  },
  computed: {
    disabled() {
      // disable button when no recipient
      return !this.contractDetails || !this.recipients || !this.recipients.filter((wallet) => wallet.address).length;
    },
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.sendRewardForm.init();
      } else {
        this.$emit('close');
      }
    },
  },
};
</script>
