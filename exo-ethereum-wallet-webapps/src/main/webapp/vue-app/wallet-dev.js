import NodeWeb3 from 'web3';
import EthereumQRPlugin from 'ethereum-qr-code';
import $ from 'jquery';

import WalletApp from './components/WalletApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

window.LocalWeb3 = NodeWeb3;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.isMaximized=true;
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#WalletApp',
  render: h => h(WalletApp)
});
