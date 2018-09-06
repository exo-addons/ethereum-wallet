<template>
  <div class="copyToClipboard">
    <code class="pt-2 pb-1 pr-4">{{ value }}</code>
    <v-btn title="Copy to clipboard" icon ripple small absolute color="blue-grey lighten-4" class="mt-0 mb-0 mr-0 ml-0" @click="copyToClipboard">
      <v-icon size="12">fa-clipboard</v-icon>
    </v-btn>
    <input ref="clipboardInput" v-model="value" type="text" />
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: function() {
        return '';
      }
    }
  },
  methods: {
    copyToClipboard() {
      this.$refs.clipboardInput.select();
      document.execCommand('copy');
    }
  }
};
</script>