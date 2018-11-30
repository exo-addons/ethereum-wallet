import vuei18nCustomConfig from './vue-i18n-config';
import WalletAdminApp from './components/WalletAdminApp.vue';

import {toFixed} from './WalletUtils.js';

import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

Vue.prototype.toFixed = toFixed;

vuei18nCustomConfig.loadLanguageAsync(lang).then(messages => {
  Vue.use(Vuetify);
  const vueInstance = new Vue({
    el: '#WalletAdminApp',
    render: h => h(WalletAdminApp)
  });
  vueInstance.$i18nMessages = messages;
});
