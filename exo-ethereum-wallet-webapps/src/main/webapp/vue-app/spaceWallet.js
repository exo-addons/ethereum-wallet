import vuei18nCustomConfig from './vue-i18n-config';
import SpaceWalletApp from './components/SpaceWalletApp.vue';

import {toFixed} from './WalletUtils.js';

import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

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
