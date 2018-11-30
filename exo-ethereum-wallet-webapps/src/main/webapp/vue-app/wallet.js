import vuei18nCustomConfig from './vue-i18n-config';
import WalletApp from './components/WalletApp.vue';

import {toFixed} from './WalletUtils.js';

import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

Vue.prototype.isMaximized = window.walletAppMaximize === "true";

Vue.prototype.toFixed = toFixed;

vuei18nCustomConfig.loadLanguageAsync(lang).then(messages => {
  Vue.use(Vuetify);
  const vueInstance = new Vue({
    el: '#WalletApp',
    render: h => h(WalletApp)
  });
  vueInstance.$i18nMessages = messages;
});
