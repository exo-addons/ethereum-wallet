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
    <v-list-tile-action>
      <v-btn
        :loading="loading"
        small
        icon
        title="Mark as completed"
        @click="markCompleted">
        <v-icon color="primary">check</v-icon>
      </v-btn>
    </v-list-tile-action>
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
  data() {
    return {
      loading: false,
    };
  },
  computed: {
    icon() {
      if (this.task.type === 'reward') {
        return 'fa-gift';
      } else if (this.task.type === 'new-wallet' || this.task.type === 'modify-wallet') {
        return 'fa-briefcase';
      } else {
        return 'fa-cog';
      }
    },
    parameters() {
      return this.task.parameters ? this.task.parameters : [];
    },
    detail() {
      if (this.task.type === 'reward') {
        return `Reward for period ${this.task.parameters[0]}`;
      } else if (this.task.type === 'new-wallet') {
        return `${this.parameters.length} new wallets to initialize`;
      } else if (this.task.type === 'modify-wallet') {
        return `${this.parameters.length} modified wallets to approve`;
      } else {
        return this.task.message;
      }
    },
    link() {
      if (this.task.type === 'reward') {
        return '/portal/g/:platform:rewarding/rewardAdministration';
      } else if (this.task.type === 'new-wallet' || this.task.type === 'modify-wallet') {
        return '/portal/g/:platform:rewarding/walletAdministration';
      } else {
        return this.task.link;
      }
    },
  },
  methods: {
    openTask(task) {
      if (this.link) {
        window.location.href = this.link;
      }
    },
    markCompleted(event) {
      event.preventDefault();
      event.stopPropagation();

      this.loading = true;
      fetch(`/portal/rest/wallet/api/task/markCompleted?taskId=${this.task.id}`, {credentials: 'include'})
        .then(resp => {
          if (!resp || !resp.ok) {
            throw new Error('Error while marking task as completed', resp);
          }
        })
        .then(() => {
          this.$emit('refresh');
        })
        .catch(e => {
          console.debug(e);
          this.tasks = [];
        })
      .finally(() => {
        this.loading = false;
      });
    },
  },
};
</script>