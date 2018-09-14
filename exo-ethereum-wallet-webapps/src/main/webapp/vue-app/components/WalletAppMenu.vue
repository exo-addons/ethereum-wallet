<template>
  <div class="walletConfigurationMenu">
    <v-btn icon flat title="Refresh wallet" @click="$emit('refresh')">
      <v-icon size="20px">refresh</v-icon>
    </v-btn>
    <v-btn v-if="!isMaximized && !isSpace && walletAddress" icon flat title="Settings" @click="$emit('modify-settings')">
      <v-icon size="17px">fa-cog</v-icon>
    </v-btn>
    <v-btn v-if="!isMaximized" icon title="Open wallet application" class="maximizeIcon" @click="$emit('maximize')">
      <v-icon size="15px">fa-external-link-alt</v-icon>
    </v-btn>
  </div>
</template>

<script>
export default {
  props: {
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    isMaximized: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  }
};
</script>