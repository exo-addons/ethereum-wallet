<template>
  <v-dialog v-model="show" content-class="uiPopup" width="400px" max-width="100vw" @keydown.esc="show = false">
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="show = false"></a>
        <span class="PopupTitle popupTitle">Wallet address</span>
      </div>
      <v-card-text>
        <code>{{ address }}</code>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    address: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data () {
    return {
      show: false
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.show = true;
      }
    },
    show() {
      if(!this.show) {
        this.$emit("close");
      }
    }
  }
};
</script>

