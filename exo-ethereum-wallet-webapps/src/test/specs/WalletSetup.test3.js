import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import WalletSetup from '../../main/webapp/vue-app/components/WalletSetup';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletSetup.test.js', () => {
  const app = getWalletApp();
  let walletSetup;

  beforeAll(() => {
    return initApp(app).then(() => (walletSetup = app.vm.$refs.walletSetup));
  });

  const defaultAttributesValues = {
    isReadOnly: true,
    displayWalletCreationToolbar: true,
    walletAddress: global.defaultWalletAddress,
    isSpace: false,
    isSpaceAdministrator: false,
    loading: false,
    isAdministration: false,
    browserWalletPasswordAutoGenerated: false,
    browserWalletExists: false,
    displayWalletSetup: false,
    browserWalletBackedUp: false,
    detectedMetamaskAccount: false,
    displayWalletNotExistingYet: false,
    displayWalletBackup: false,
    displayResetPassword: false,
  };

  it('WalletSetup default data', () => {
    console.log('-- Test WalletSetup default data');

    const walletSetup = mount(WalletSetup, {
      attachToDocument: true,
    });
    expectObjectValueEqual(
      walletSetup.vm,
      {
        isReadOnly: false,
        displayWalletCreationToolbar: false,
        walletAddress: false,
        isSpace: false,
        isSpaceAdministrator: false,
        loading: false,
        isAdministration: false,
        browserWalletPasswordAutoGenerated: false,
        browserWalletExists: false,
        displayWalletSetup: false,
        browserWalletBackedUp: false,
        detectedMetamaskAccount: false,
        displayWalletNotExistingYet: false,
        displayWalletBackup: false,
        displayResetPassword: false,
      },
      'WalletSetup default data'
    );
  });

  it('WalletSetup data', () => {
    expectObjectValueEqual(walletSetup, defaultAttributesValues, 'WalletSetup data');
  });

  it('WalletSetup refresh', () => {
    const walletSetup = mount(WalletSetup, {
      attachToDocument: true,
    });
    walletSetup.vm.refresh();
    expect(walletSetup.emitted().refresh).toBeTruthy();
  });

  it('WalletSetup visible components', () => {
    const walletSetupHTML = walletSetup.$el.innerHTML;

    expect(walletSetupHTML).toContain('No private key was found in current browser');
    expect(walletSetupHTML).toContain('is displayed in readonly mode');

    expect(walletSetupHTML).not.toContain('Your wallet is not backed up yet');
    expect(walletSetupHTML).not.toContain('Your wallet is not secured yet');
    expect(walletSetupHTML).not.toContain('Space wallet');
    expect(walletSetupHTML).not.toContain('Space administrator');

    expectCountElement(app, 'walletBrowserSetup', 0);
    expectCountElement(app, 'walletMetamaskSetup', 0);
  });

  it('WalletSetup display Wallet Setup actions', (done) => {
    walletSetup.displayWalletSetupActions();
    return app.vm.$nextTick().then(() => {
      try {
        expectCountElement(app, 'walletBrowserSetup', 1);
      } catch (e) {
        return done(e);
      }
      done();
    });
  });

  it('WalletSetup display Wallet backup and set password warnings actions', (done) => {
    console.log('-- Test WalletSetup display Wallet backup and set password warnings actions');

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;

        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.isReadOnly = false;
        expectedData.displayWalletCreationToolbar = false;
        expectedData.browserWalletPasswordAutoGenerated = true;
        expectedData.browserWalletExists = true;
        expectedData.displayResetPassword = true;
        expectedData.browserWalletBackedUp = true;
        expectedData.displayWalletBackup = false;
        expectObjectValueEqual(walletSetup, expectedData, 'WalletSetup display Wallet backup and set password warnings actions');

        // Simulate no backup was made
        walletSetup.browserWalletBackedUp = false;
        return flushPromises();
      })
      .then(() => {
        expectCountElement(app, 'walletResetPasswordWarning', 1);
        expectCountElement(app, 'walletBackupWarning', 1);

        walletSetup.hideBackupMessage();

        return flushPromises();
      })
      .then(() => app.vm.$nextTick())
      .then(() => {
        expectCountElement(app, 'walletResetPasswordWarning', 1);
        expectCountElement(app, 'walletBackupWarning', 0);

        window.walletSettings.userPreferences.browserWalletPasswordAutoGenerated = false;
        walletSetup.refreshAutoGenerated();

        return flushPromises();
      })
      .then(() => {
        expectCountElement(app, 'walletResetPasswordWarning', 0);
        expectCountElement(app, 'walletBackupWarning', 0);
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
