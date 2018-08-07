import exoi18n from '../js/lib/exo-i18n';
import WalletApp from './components/WalletApp.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

Vue.prototype.isMaximized = window.walletAppMaximize === "true";

exoi18n.loadLanguageAsync(lang).then(i18n => {
  Vue.use(Vuetify);
  new Vue({
    el: '#WalletApp',
    render: h => h(WalletApp),
    i18n
  });
});
