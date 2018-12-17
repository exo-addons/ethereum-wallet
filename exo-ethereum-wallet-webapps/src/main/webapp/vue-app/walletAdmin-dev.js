import NodeWeb3 from 'web3';
import $ from 'jquery';

import WalletAdminApp from './components/WalletAdminApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

window.LocalWeb3 = NodeWeb3;
window.$ = $;
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletAdminApp',
  render: h => h(WalletAdminApp)
});
