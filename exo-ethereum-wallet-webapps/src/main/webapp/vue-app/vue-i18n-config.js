import Vue from 'vue';

let messages = null;

function loadLanguageAsync (lang) {
  if(!lang) {
    lang = 'en';
  }
  if (!messages) {
    return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.WalletPortlet-${lang}.json`, {
      credentials: 'include'
    }).then(resp => resp.json()).then(msgs => messages = msgs);
  } else {
    return Promise.resolve(messages);
  }
}

export default {
  loadLanguageAsync
};