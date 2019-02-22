import TodoListApp from './components/todo-list/TodoListApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#TodoListApp',
  render: (h) => h(TodoListApp),
});
