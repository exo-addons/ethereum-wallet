import WalletAdminApp from './components/admin/WalletAdminApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminApp),
});
