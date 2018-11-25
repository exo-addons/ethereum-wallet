<template>
  <v-dialog v-model="dialog" content-class="uiPopup" width="600px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <button slot="activator" :value="true" class="btn btn-primary mt-1 mb-1" @click="dialog = true">
      Send kudos reward
    </button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Send kudos reward</span>
      </div>
      <send-kudos-form ref="sendKudosForm"
                       :recipients="recipients"
                       :contract-details="contractDetails"
                       class="pt-4"
                       @sent="$emit('sent', $event, contractDetails)"
                       @close="dialog = false"
                       @error="$emit('error', $event)" />
    </v-card>
  </v-dialog>
</template>

<script>
import SendKudosForm from './SendKudosForm.vue';

export default {
  components: {
    SendKudosForm
  },
  props: {
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    },
    recipients: {
      type: Array,
      default: function() {
        return [];
      }
    }
  },
  data () {
    return {
      dialog: false
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.sendKudosForm.init();
      } else {
        this.$emit('close');
      }
    }
  }
};
</script>

