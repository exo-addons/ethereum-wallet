import Vue from 'vue';
import Vuetify from 'vuetify';
import exoi18n from '../js/lib/exo-i18n';
import App from './components/App.vue';
import AccountsList from './components/AccountsList.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

window.localWeb3 = null;

exoi18n.loadLanguageAsync(lang).then(i18n => {
  Vue.use(Vuetify);
  let id = '#WalletApp';
  let app = App;
  if (!document.getElementById('WalletApp')) {
    id = '#WalletApp2';
    app = AccountsList;
  }
  new Vue({
    el: id,
    render: h => h(app),
    i18n
  });
});
