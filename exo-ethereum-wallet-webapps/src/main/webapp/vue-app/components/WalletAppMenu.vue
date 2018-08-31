<template>
  <v-menu v-model="walletConfigurationMenu" content-class="walletConfigurationMenu">
    <v-btn slot="activator" icon>
      <v-icon>more_vert</v-icon>
    </v-btn>
    <v-list>
      <v-list-tile @click="$emit('refresh')">
        <v-list-tile-avatar>
          <v-icon>refresh</v-icon>
        </v-list-tile-avatar>
        <v-list-tile-title>Refresh</v-list-tile-title>
      </v-list-tile>
      <v-list-tile v-if="!isSpace" @click="$emit('modify-settings')">
        <v-list-tile-avatar>
          <v-icon>fa-cog</v-icon>
        </v-list-tile-avatar>
        <v-list-tile-title>Settings</v-list-tile-title>
      </v-list-tile>
    </v-list>
  </v-menu>
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
    isAccountDetails: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      walletConfigurationMenu: false
    };
  },
  created() {
    $(document).on("click", () => {
      this.walletConfigurationMenu = false;
    });
  }
};
</script>