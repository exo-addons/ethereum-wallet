<template>
  <v-dialog v-model="show" content-class="uiPopup" width="300px" max-width="100vw" @keydown.esc="show = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="show = false"></a>
        <span class="PopupTitle popupTitle">{{ title }}</span>
      </div>
      <v-card-text>
        <qr-code ref="qrCode"
                 :net-id="netId"
                 :from="from"
                 :to="to"
                 :is-contract="isContract"
                 :function-payable="functionPayable"
                 :function-name="functionName"
                 :args-names="argsNames"
                 :args-types="argsTypes"
                 :args-values="argsValues"
                 :amount="amount"
                 :open="open"
                 :information="information" />
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
import QrCode from './QRCode.vue';

export default {
  components: {
    QrCode
  },
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      }
    },
    information: {
      type: String,
      default: function() {
        return null;
      }
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    amount: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    from: {
      type: String,
      default: function() {
        return null;
      }
    },
    to: {
      type: String,
      default: function() {
        return null;
      }
    },
    isContract: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    functionName: {
      type: String,
      default: function() {
        return null;
      }
    },
    functionPayable: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    argsNames: {
      type: Array,
      default: function() {
        return [];
      }
    },
    argsTypes: {
      type: Array,
      default: function() {
        return [];
      }
    },
    argsValues: {
      type: Array,
      default: function() {
        return [];
      }
    }
  },
  data () {
    return {
      show: false,
      netId: null
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.show = true;
        this.computeCanvas();
      }
    },
    show() {
      if(!this.show) {
        this.netId = null;
        this.$emit("close");
      }
    }
  },
  methods: {
    computeCanvas() {
      this.$refs.qrCode.computeCanvas();
    }
  }
};
</script>

