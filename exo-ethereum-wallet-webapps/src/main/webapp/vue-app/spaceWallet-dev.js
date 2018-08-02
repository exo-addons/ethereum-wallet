import NodeWeb3 from 'web3';
import TruffleContract from 'truffle-contract';
import EthereumQRPlugin from 'ethereum-qr-code';
import $ from 'jquery';

import exoi18n from '../js/lib/exo-i18n';
import SpaceWalletApp from './components/SpaceWalletApp.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.LocalWeb3 = NodeWeb3;
window.TruffleContract = TruffleContract;
window.EthereumQRPlugin = EthereumQRPlugin;
window.$ = $;

exoi18n.loadLanguageAsync(lang).then(i18n => {
  Vue.use(Vuetify);
  new Vue({
    el: '#SpaceWalletApp',
    render: h => h(SpaceWalletApp),
    i18n
  });
});
