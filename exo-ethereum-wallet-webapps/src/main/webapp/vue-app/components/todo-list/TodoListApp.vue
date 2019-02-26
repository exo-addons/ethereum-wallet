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
        <tasks-list :tasks="tasks" @refresh="init" />
      </v-card>
      <div id="walletDialogsParent">
      </div>
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
      fetch('/portal/rest/wallet/api/task/list', {credentials: 'include'})
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error while requesting tasks list', resp);
          }
        })
        .then(tasks => this.tasks = tasks)
        .catch(e => {
          console.debug(e);
          this.tasks = [];
        });
    },
  },
};
</script>