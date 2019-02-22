<template>
  <v-list dense>
    <task-item
      v-for="task in tasks"
      :key="task.id"
      :task="task" />
  </v-list>
</template>

<script>
import TaskItem from './TaskItem.vue';

export default {
  components: {
    TaskItem,
  },
  props: {
    tasks: {
      type: Array,
      default: function() {
        return [];
      },
    },
  },
};
</script>