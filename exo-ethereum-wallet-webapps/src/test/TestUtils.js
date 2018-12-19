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

