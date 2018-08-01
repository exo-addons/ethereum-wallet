import NodeWeb3 from 'web3';
import TruffleContract from 'truffle-contract';
import $ from 'jquery';

import exoi18n from '../js/lib/exo-i18n';
import AccountsList from './components/AccountsList.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.LocalWeb3 = NodeWeb3;
window.TruffleContract = TruffleContract;
window.$ = $;

exoi18n.loadLanguageAsync(lang).then(i18n => {
  Vue.use(Vuetify);
  new Vue({
    el: '#WalletApp',
    render: h => h(AccountsList),
    i18n
  });
});
