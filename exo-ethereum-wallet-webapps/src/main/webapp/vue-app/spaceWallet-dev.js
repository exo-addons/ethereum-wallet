import NodeWeb3 from 'web3';
import TruffleContract from 'truffle-contract';
import EthereumQRPlugin from 'ethereum-qr-code';
import $ from 'jquery';

import vuei18nCustomConfig from './vue-i18n-config';
import SpaceWalletApp from './components/SpaceWalletApp.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.LocalWeb3 = NodeWeb3;
window.TruffleContract = TruffleContract;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

Vue.prototype.isMaximized = "true";

vuei18nCustomConfig.loadLanguageAsync(lang).then(messages => {
  Vue.use(Vuetify);
  const vueInstance = new Vue({
    el: '#SpaceWalletApp',
    render: h => h(SpaceWalletApp)
  });
  vueInstance.$i18nMessages = messages;
});
