<template>
  <v-dialog v-model="show" width="400px" max-width="100vw" @keydown.esc="show = false">
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Wallet address</v-toolbar-title>
        <v-spacer />
        <v-btn icon dark @click.native="show = false">
          <v-icon>close</v-icon>
        </v-btn>
      </v-toolbar>
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

