import NodeWeb3 from 'web3';
import EthereumQRPlugin from 'ethereum-qr-code';
import $ from 'jquery';

import SpaceWalletApp from './components/SpaceWalletApp.vue';
import {toFixed} from './WalletUtils.js';
import './../css/main.less';

window.LocalWeb3 = NodeWeb3;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.isMaximized = "true";
Vue.prototype.toFixed = toFixed;
Vue.use(Vuetify);

const vueInstance = new Vue({
  el: '#SpaceWalletApp',
  render: h => h(SpaceWalletApp)
});
