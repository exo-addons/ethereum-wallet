import WalletAPIApp from './components/reward/RewardApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#RewardApp',
  render: (h) => h(WalletAPIApp),
});
