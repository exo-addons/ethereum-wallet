import NodeWeb3 from '../main/webapp/js/lib/web3.min.js';
import EthereumQRPlugin from '../main/webapp/js/lib/ethereum-qr-code.min.js';
import $ from 'jquery';

import WalletAdminApp from './components/admin/WalletAdminApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

window.LocalWeb3 = NodeWeb3;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletAdminApp',
  render: (h) => h(WalletAdminApp),
});
