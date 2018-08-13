import NodeWeb3 from 'web3';
import TruffleContract from 'truffle-contract';
import $ from 'jquery';

import vuei18nCustomConfig from './vue-i18n-config';
import WalletAdminApp from './components/WalletAdminApp.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.LocalWeb3 = NodeWeb3;
window.TruffleContract = TruffleContract;
window.$ = $;

vuei18nCustomConfig.loadLanguageAsync(lang).then(messages => {
  Vue.use(Vuetify);
  const vueInstance = new Vue({
    el: '#WalletAdminApp',
    render: h => h(WalletAdminApp)
  });
  vueInstance.$i18nMessages = messages;
});
