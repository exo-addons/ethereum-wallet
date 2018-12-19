import NodeWeb3 from '../main/webapp/js/lib/web3.min.js';
import EthereumQRPlugin from '../main/webapp/js/lib/ethereum-qr-code.min.js';
import $ from 'jquery';

import WalletApp from './components/WalletApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

window.LocalWeb3 = NodeWeb3;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.isMaximized = true;
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletApp',
  render: (h) => h(WalletApp),
});
