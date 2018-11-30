import NodeWeb3 from 'web3';
import EthereumQRPlugin from 'ethereum-qr-code';
import $ from 'jquery';

import vuei18nCustomConfig from './vue-i18n-config';
import SpaceWalletApp from './components/SpaceWalletApp.vue';

import {toFixed} from './WalletUtils.js';

import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.LocalWeb3 = NodeWeb3;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.isMaximized = "true";

Vue.prototype.toFixed = toFixed;

vuei18nCustomConfig.loadLanguageAsync(lang).then(messages => {
  Vue.use(Vuetify);
  const vueInstance = new Vue({
    el: '#SpaceWalletApp',
    render: h => h(SpaceWalletApp)
  });
  vueInstance.$i18nMessages = messages;
});
