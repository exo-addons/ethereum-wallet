import WalletApp from '../../main/webapp/vue-app/components/WalletApp';
import WalletAppMenu from '../../main/webapp/vue-app/components/WalletAppMenu';
import WalletSetup from '../../main/webapp/vue-app/components/WalletSetup';
import WalletSummary from '../../main/webapp/vue-app/components/WalletSummary';
import WalletAccountsList from '../../main/webapp/vue-app/components/WalletAccountsList';
import AccountDetail from '../../main/webapp/vue-app/components/AccountDetail';
import WalletSettingsModal from '../../main/webapp/vue-app/components/WalletSettingsModal';
import WalletBrowserSetup from '../../main/webapp/vue-app/components/WalletBrowserSetup';
import WalletMetamaskSetup from '../../main/webapp/vue-app/components/WalletMetamaskSetup';
import WalletBackupModal from '../../main/webapp/vue-app/components/WalletBackupModal';
import WalletResetModal from '../../main/webapp/vue-app/components/WalletResetModal';
import {getWalletApp, initApp} from '../TestUtils.js';

describe('WalletApp.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  it('WalletApp data', () => {
    expect(app.vm.networkId).toBe(4452365);
    expect(app.vm.browserWalletExists).toBeFalsy();
    expect(app.vm.walletAddress).toBe('0xb460a021b66a1f421970b07262ed11d626b798ef');
    expect(app.vm.selectedTransactionHash).toBeNull();
    expect(app.vm.selectedAccount).toBeNull();
    expect(app.vm.fiatSymbol).toBe('$');
    expect(app.vm.isWalletEnabled).toBeTruthy();
    expect(app.vm.loading).toBeFalsy();
    expect(app.vm.useMetamask).toBeFalsy();
    expect(app.vm.isReadOnly).toBeTruthy();
    expect(app.vm.isSpaceAdministrator).toBeFalsy();
    expect(app.vm.seeAccountDetails).toBeFalsy();
    expect(app.vm.seeAccountDetailsPermanent).toBeFalsy();
    expect(app.vm.principalAccount).toBe('ether');
    expect(app.vm.fiatSymbol).toBe('$');
    expect(app.vm.error).toBeNull();
    expect(app.vm.showSettingsModal).toBeFalsy();
    expect(app.vm.showAddContractModal).toBeFalsy();
    expect(app.vm.overviewAccounts).toEqual(['ether']);
    expect(app.vm.overviewAccountsToDisplay).toEqual(['ether']);
    expect(Number(app.vm.gasPriceInEther)).toBe(0.000000008);

    expect(Number(app.vm.displayAccountsList)).toBeTruthy();
    expect(Number(app.vm.displayWalletResetOption)).toBeFalsy();
    expect(Number(app.vm.displayEtherBalanceTooLow)).toBeFalsy();
    expect(Number(app.vm.etherBalance)).toBe(100);
    expect(Number(app.vm.totalBalance)).toBe(100);
    expect(Number(app.vm.totalFiatBalance)).toBe(9361.729);
  });

  it('WalletApp visible components', () => {
    expect(app.findAll('#walletSetup')).toHaveLength(1);
    expect(app.findAll('#waletSummary')).toHaveLength(1);
    expect(app.findAll('#walletAccountsList')).toHaveLength(1);
    expect(app.findAll('#walletAppMenu')).toHaveLength(1);
    expect(app.findAll('#accountDetail')).toHaveLength(0);
  });

  it('WalletApp maximized class', () => {
    expect(app.find('#WalletApp').classes()).toContain('maximized');
    expect(app.find('#WalletApp').classes()).not.toContain('minimized');
  });

  it('WalletSetup data', () => {
    expect(app.vm.$refs.walletSetup.isReadOnly).toBeTruthy();
    expect(app.vm.$refs.walletSetup.displayWalletCreationToolbar).toBeTruthy();
    expect(app.vm.$refs.walletSetup.walletAddress).toBe('0xb460a021b66a1f421970b07262ed11d626b798ef');
    expect(app.vm.$refs.walletSetup.isSpace).toBeFalsy();
    expect(app.vm.$refs.walletSetup.loading).toBeFalsy();
    expect(app.vm.$refs.walletSetup.isAdministration).toBeFalsy();
    expect(app.vm.$refs.walletSetup.useMetamask).toBeFalsy();
    expect(app.vm.$refs.walletSetup.isSpaceAdministrator).toBeFalsy();
    expect(app.vm.$refs.walletSetup.autoGenerated).toBeFalsy();
    expect(app.vm.$refs.walletSetup.skipWalletPasswordSet).toBeFalsy();
    expect(app.vm.$refs.walletSetup.browserWalletExists).toBeFalsy();
    expect(app.vm.$refs.walletSetup.displayWalletSetup).toBeFalsy();
    expect(app.vm.$refs.walletSetup.browserWalletBackedUp).toBeFalsy();
    expect(app.vm.$refs.walletSetup.detectedMetamaskAccount).toBeNull();
    expect(app.vm.$refs.walletSetup.displayWalletNotExistingYet).toBeFalsy();
    expect(app.vm.$refs.walletSetup.displayWalletBackup).toBeFalsy();
    expect(app.vm.$refs.walletSetup.displayResetPassword).toBeFalsy();
  });

  it('WalletSetup visible components', () => {
    const walletSetupHTML = app.vm.$refs.walletSetup.$el.innerHTML;

    expect(walletSetupHTML).toContain('No private key was found in current browser');
    expect(walletSetupHTML).toContain('is displayed in readonly mode');

    expect(walletSetupHTML).not.toContain('Your wallet is not backed up yet');
    expect(walletSetupHTML).not.toContain('Your wallet is not secured yet');
    expect(walletSetupHTML).not.toContain('Space wallet');
    expect(walletSetupHTML).not.toContain('Space administrator');

    expect(app.findAll('#walletBrowserSetup')).toHaveLength(0);
    expect(app.findAll('#walletMetamaskSetup')).toHaveLength(0);
  });

  it('WalletAppMenu data', () => {
    expect(app.vm.$refs.walletAppMenu.isSpace).toBeFalsy();
    expect(app.vm.$refs.walletAppMenu.isSpaceAdministrator).toBeFalsy();
    expect(app.vm.$refs.walletAppMenu.walletAddress).toBe('0xb460a021b66a1f421970b07262ed11d626b798ef');
    expect(app.vm.$refs.walletAppMenu.isMaximized).toBeTruthy();
  });

  it('WalletAppMenu visible components', () => {
    const walletAppMenuHTML = app.vm.$refs.walletAppMenu.$el.innerHTML;

    expect(walletAppMenuHTML).toContain('Refresh wallet');
    expect(walletAppMenuHTML).toContain('title="Settings"');

    expect(walletAppMenuHTML).not.toContain('Open wallet application');
  });
});
