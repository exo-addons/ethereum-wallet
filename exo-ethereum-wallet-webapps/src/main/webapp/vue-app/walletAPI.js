import WalletAPIApp from './components/WalletAPIApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

Vue.prototype.isMaximized = window.walletAppMaximize === 'true';
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletAPIApp',
  render: (h) => h(WalletAPIApp),
});
