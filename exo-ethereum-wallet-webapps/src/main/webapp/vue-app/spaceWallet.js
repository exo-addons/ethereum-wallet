import exoi18n from '../js/lib/exo-i18n';
import SpaceWalletApp from './components/SpaceWalletApp.vue';
import './../css/main.less';

const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language ? eXo.env.portal.language : 'en';

Vue.prototype.isMaximized = "true";

exoi18n.loadLanguageAsync(lang).then(i18n => {
  Vue.use(Vuetify);
  new Vue({
    el: '#SpaceWalletApp',
    render: h => h(SpaceWalletApp),
    i18n
  });
});
