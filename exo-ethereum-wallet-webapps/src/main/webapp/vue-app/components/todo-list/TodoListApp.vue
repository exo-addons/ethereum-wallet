<template>
  <v-app
    v-if="tasks && tasks.length"
    id="TodoListApp"
    color="transaprent"
    class="ml-0 mr-0 mb-3"
    flat>
    <main>
      <v-card flat>
        <h6 class="pb-0 pt-2 text-xs-center">
          Wallet todo list
        </h6>
        <tasks-list :tasks="tasks" />
      </v-card>
    </main>
  </v-app>
</template>

<script>
import TasksList from './TasksList.vue';

export default {
  components: {
    TasksList,
  },
  data() {
    return {
      tasks: []
    };
  },
  created() {
    this.init();
  },
  methods: {
    init() {
      this.tasks = [{
        id: 1,
        link: '/portal/g/:platform:rewarding/rewardAdministration',
        message: null,
        parameters: ['28/02/2019'],
        type: 'reward',
        completed: false,
      },
      {
        id: 2,
        link: '/portal/g/:platform:rewarding/walletAdministration',
        message: null,
        parameters: ['2'],
        type: 'wallet-init',
        completed: false,
      }];
    },
  },
};
</script>