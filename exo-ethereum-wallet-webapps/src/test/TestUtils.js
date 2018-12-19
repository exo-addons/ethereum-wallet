import WalletApp from '../main/webapp/vue-app/components/WalletApp';
import WalletAppMenu from '../main/webapp/vue-app/components/WalletAppMenu';
import WalletSetup from '../main/webapp/vue-app/components/WalletSetup';
import WalletSummary from '../main/webapp/vue-app/components/WalletSummary';
import WalletAccountsList from '../main/webapp/vue-app/components/WalletAccountsList';
import AccountDetail from '../main/webapp/vue-app/components/AccountDetail';
import WalletSettingsModal from '../main/webapp/vue-app/components/WalletSettingsModal';
import WalletBrowserSetup from '../main/webapp/vue-app/components/WalletBrowserSetup';
import WalletMetamaskSetup from '../main/webapp/vue-app/components/WalletMetamaskSetup';
import WalletBackupModal from '../main/webapp/vue-app/components/WalletBackupModal';
import WalletResetModal from '../main/webapp/vue-app/components/WalletResetModal';

const {mount} = require('@vue/test-utils');

export function initApp(app) {
  return app.vm.init();
}

export function getWalletApp() {
  return mount(WalletApp, {
    stubs: {
      'wallet-setup': WalletSetup,
      'wallet-app-menu': WalletAppMenu,
      'wallet-accounts-list': WalletAccountsList,
      'wallet-settings-modal': WalletSettingsModal,
      'wallet-summary': WalletSummary,
      'account-detail': AccountDetail,
      'wallet-browser-setup': WalletBrowserSetup,
      'wallet-metamask-setup': WalletMetamaskSetup,
      'wallet-backup-modal': WalletBackupModal,
      'wallet-reset-modal': WalletResetModal,
    },
    propsData: {
      isSpace: false,
    },
    attachToDocument: true,
  });
}

export function expectCountElement(app, id, count) {
  expect(app.findAll(`#${id}`)).toHaveLength(count);
}

export function expectHasClass(app, id, className) {
  expect(app.find(`#${id}`).classes()).toContain(className);
}

export function expectObjectValueEqual(value, expected, ignoredKeys) {
  expect(expected).not.toBeNull();
  expect(value).not.toBeNull();

  expect(typeof expected).toBe('object');
  expect(typeof value).toBe('object');

  Object.keys(expected).forEach((key) => {
    if(ignoredKeys && ignoredKeys.indexOf(key) >= 0) {
      return;
    }
    try {
      expect(value.hasOwnProperty(key)
          || (value.hasOwnProperty('_computedWatchers') && value['_computedWatchers'] && Object.keys(value['_computedWatchers']).indexOf(key) >= 0)
          || (value.hasOwnProperty('_props') && value['_props'] && Object.keys(value['_props']).indexOf(key) >= 0)
          ).toBeTruthy();
    } catch(e) {
      console.error("can't find element with key in result", key);
      throw e;
    }
    const expectedValue = expected[key];
    try {
      if(typeof expectedValue === 'boolean') {
        if(expectedValue) {
          expect(value[key]).toBeTruthy();
        } else {
          expect(value[key]).toBeFalsy();
        }
      } else if(typeof expectedValue === 'string' || typeof expectedValue === 'number') {
        expect(value[key]).toBe(expectedValue);
      } else if(typeof expectedValue === 'object' && expectedValue.length) {
        expect(value[key]).toEqual(expectedValue);
      } else if(typeof expectedValue === 'object') {
        expect(value[key]).toEqual(expectedValue);
      } else {
        throw new Error(`cannot find type of key: ${  key}`);
      }
    } catch(e) {
      console.error("Wrong value for key in result", key);
      throw e;
    }
  });
}