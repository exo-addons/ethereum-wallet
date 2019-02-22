<template>
  <v-list-tile
    :key="task.title"
    avatar
    @click="openTask(task)">
    <v-list-tile-avatar
      tile
      size="40"
      style="min-width: 40px">
      <v-icon>{{ icon }}</v-icon>
    </v-list-tile-avatar>
    <v-list-tile-content>
      <v-list-tile-title :title="detail" v-text="detail" />
      <v-list-tile-sub-title
        v-if="detail && task.message"
        :title="task.message"
        v-text="task.message" />
    </v-list-tile-content>
  </v-list-tile>
</template>

<script>
export default {
  props: {
    task: {
      type: Object,
      default: function() {
        return {};
      },
    },
  },
  computed: {
    icon() {
      if (this.task.type === 'reward') {
        return 'fa-gift';
      } else if (this.task.type === 'wallet-init') {
        return 'fa-briefcase';
      } else {
        return 'fa-cog';
      }
    },
    detail() {
      if (this.task.type === 'reward') {
        return `Reward for period ${this.task.parameters[0]}`;
      } else if (this.task.type === 'wallet-init') {
        return `${this.task.parameters[0]} new wallets`;
      } else {
        return this.task.message;
      }
    },
  },
  methods: {
    openTask(task) {
      if (this.task.link) {
        window.location.href = this.task.link;
      }
    }
  },
};
</script>