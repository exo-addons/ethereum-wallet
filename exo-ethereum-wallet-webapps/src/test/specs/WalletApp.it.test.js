import WalletApp from '../../main/webapp/vue-app/components/WalletApp';
import WalletAppMenu from '../../main/webapp/vue-app/components/WalletAppMenu';
import WalletSetup from '../../main/webapp/vue-app/components/WalletSetup';
import WalletSummary from '../../main/webapp/vue-app/components/WalletSummary';
import WalletAccountsList from '../../main/webapp/vue-app/components/WalletAccountsList';
import AccountDetail from '../../main/webapp/vue-app/components/AccountDetail';
import WalletSettingsModal from '../../main/webapp/vue-app/components/WalletSettingsModal';
import AddContractModal from '../../main/webapp/vue-app/components/AddContractModal';
import WalletBrowserSetup from '../../main/webapp/vue-app/components/WalletBrowserSetup.vue';
import WalletMetamaskSetup from '../../main/webapp/vue-app/components/WalletMetamaskSetup.vue';
import WalletBackupModal from '../../main/webapp/vue-app/components/WalletBackupModal.vue';
import WalletResetModal from '../../main/webapp/vue-app/components/WalletResetModal.vue';

const { shallowMount, mount, wrapper} = require('@vue/test-utils');

describe('WalletApp.test.js', () => {
  let app;
  const overviewAccounts = [{
    title : 'ether',
    icon : 'fab fa-ethereum',
    symbol : 'ether',
    isContract : true,
  }];

  beforeEach(() => {
    app = mount(WalletApp, {
      stubs: {
        'wallet-accounts-list': WalletAccountsList,
        'wallet-app-menu': WalletAppMenu,
        'wallet-settings-modal': WalletSettingsModal,
        'wallet-setup': WalletSetup,
        'wallet-summary': WalletSummary,
        'account-detail': AccountDetail,
        'wallet-browser-setup': WalletBrowserSetup,
        'wallet-metamask-setup': WalletMetamaskSetup,
        'wallet-backup-modal': WalletBackupModal,
        'wallet-reset-modal': WalletResetModal,
      },
      propsData: {
        isSpace: false
      },
      attachToDocument: true
    });
    return app.vm.init();
  });

  it('Verify WalletApp maximized class', () => {
    expect(app.find('#WalletApp').classes()).toContain('maximized');
    expect(app.find('#WalletApp').classes()).not.toContain('minimized');
  });

  it('Verify WalletApp visible components', () => {
    expect(app.findAll('#walletSetup')).toHaveLength(1);
    expect(app.findAll('#walletBrowserSetup')).toHaveLength(0);
    expect(app.findAll('#walletMetamaskSetup')).toHaveLength(0);
    expect(app.findAll('#waletSummary')).toHaveLength(1);
    expect(app.findAll('#walletAccountsList')).toHaveLength(1);
    expect(app.findAll('#walletAppMenu')).toHaveLength(1);
    expect(app.findAll('#accountDetail')).toHaveLength(0);
  });

});