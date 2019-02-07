<template>
  <div class="copyToClipboard walletAddress">
    <code :class="allowCopy && 'pr-4'" class="pt-2 pb-1">{{ value }}</code>
    <v-btn
      v-if="allowCopy"
      title="Copy to clipboard"
      icon
      ripple
      small
      absolute
      color="blue-grey lighten-4"
      class="mt-0 mb-0 mr-0 ml-0"
      @click="copyToClipboard">
      <v-icon size="12" dark>
        fa-copy
      </v-icon>
    </v-btn>
    <input
      v-if="allowCopy"
      ref="clipboardInput"
      v-model="value"
      type="text">
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: function() {
        return '';
      },
    },
    allowCopy: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
  },
  methods: {
    copyToClipboard() {
      this.$refs.clipboardInput.select();
      try {
        document.execCommand('copy');
      } catch(e) {
        console.debug('Error executing document.execCommand', e);
      }
    },
  },
};
</script>
