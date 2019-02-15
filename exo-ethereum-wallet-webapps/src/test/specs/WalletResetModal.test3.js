import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import WalletResetModal from '../../main/webapp/vue-app/components/WalletResetModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletResetModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    displayRememberMe: false,
    buttonLabel: false,
    dialog: false,
    loading: false,
    backedUp: false,
    error: false,
    walletPassword: '',
    walletPasswordShow: false,
    newWalletPassword: '',
    newWalletPasswordShow: false,
    autoGeneratedPassword: false,
    browserWalletDecrypted: false,
    rememberPasswordStored: false,
    rememberPasswordToChange: false,
    rememberPassword: false,
  };

  it('WalletResetModal default data', () => {
    console.log('-- WalletResetModal default data');

    const walletResetModal = mount(WalletResetModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletResetModal.vm, defaultAttributesValues, 'walletResetModal default data');
    // walletResetModal.vm.resetWallet();
  });

  it('WalletResetModal  test WalletResetModal disabled when no wallet is generated', (done) => {
    console.log('--WalletResetModal test WalletResetModal disabled');

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /*not generated */ false, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => app.vm.$nextTick())
      .then(() => {
        const walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        expect(app.findAll('#walletSetup .walletResetModal')).toHaveLength(0);

        return app.vm.$nextTick();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletResetModal  test WalletResetModal enabled when wallet is generated', (done) => {
    console.log('--WalletResetModal test WalletResetModal enabled');

    global.walletAddress = global.walletAddresses[0];

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /*generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => app.vm.$nextTick())
      .then(() => {
        const walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        expect(app.findAll('#walletSetup .walletResetModal')).toHaveLength(1);

        return app.vm.$nextTick();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletResetModal test set password warning', (done) => {
    global.walletAddress = global.walletAddresses[1];
    let walletResetModal, walletSetup;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))

      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        expectCountElement(app, 'walletResetPasswordWarning', 1);

        window.walletSettings.userPreferences.browserWalletPasswordAutoGenerated = false;
        walletSetup.refreshAutoGenerated();

        return flushPromises();
      })

      .then(() => {
        expectCountElement(app, 'walletResetPasswordWarning', 0);
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletResetModal test when you set your wallet password (wallet not backedup', (done) => {
    global.walletAddress = global.walletAddresses[1];
    let walletResetModal, walletSetup;

    return initApp(app)
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        expectCountElement(app, 'walletResetPasswordWarning', 1);
        return flushPromises();
      })

      .then(() => {
        walletResetModal = walletSetup.$refs.walletResetModal;
        expect(walletResetModal).toBeTruthy();

        expectCountElement(app, 'backupWarningWhenSetPassword', 1);
      })
      .then(() => flushPromises())

      .then(() => {
        expect(walletResetModal.error).toBeNull();

        walletResetModal.rememberPasswordToChange = true;

        walletResetModal.newWalletPassword = 'testpassword';

        walletResetModal.changeRememberMe();

        walletResetModal.resetWallet(); // !!!!!!!!

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.buttonLabel = 'Set a password';
        expectedData.dialog = true;
        expectedData.rememberPasswordStored = true;
        expectedData.autoGeneratedPassword = true;
        expectedData.loading = true;
        expectedData.rememberPasswordToChange = true;
        expectedData.newWalletPassword = 'testpassword';
        expectObjectValueEqual(walletResetModal, expectedData, 'set your wallet password default data');
        return app.vm.$nextTick();
      })
      .then(() => flushPromises())
      .then(() => {
        walletResetModal.dialog = true;

        walletResetModal.newWalletPassword = 'testpassword1';

        walletResetModal.WalletPassword = 'testpassword';

        expect(walletResetModal.error).toBeNull();

        walletResetModal.resetWallet();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.buttonLabel = 'Set a password';
        expectedData.dialog = true;
        expectedData.loading = true;
        expectedData.rememberPasswordToChange = true;

        expectedData.rememberPasswordStored = true;
        expectedData.autoGeneratedPassword = true;
        expectedData.newWalletPassword = 'testpassword1';
        expectObjectValueEqual(walletResetModal, expectedData, 'reset your wallet password default data');
        return app.vm.$nextTick();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletResetModal the password is wrong', (done) => {
    global.walletAddress = global.walletAddresses[1];
    let walletResetModal;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* backedup */ true)
      .then(() => initApp(app))

      .then(() => app.vm.$nextTick())
      .then(() => {
        const walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        walletResetModal = walletSetup.$refs.walletResetModal;
        expect(walletResetModal).toBeTruthy();

        expect(walletResetModal.error).toBeNull();
      })
      .then(() => flushPromises())

      .then(() => {
        walletResetModal.dialog = true;
        walletResetModal.newWalletPassword = 'fake password';
        expect(walletResetModal.error).toBeNull();

        walletResetModal.changeRememberMe();

        walletResetModal.rememberPasswordToChange = true;

        walletResetModal.resetWallet();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.buttonLabel = 'Set a password';
        expectedData.dialog = true;
        expectedData.rememberPasswordStored = true;
        expectedData.autoGeneratedPassword = true;
        expectedData.backedUp = true;

        expectedData.loading = true;

        expectedData.rememberPasswordToChange = true;

        expectedData.newWalletPassword = 'fake password';
        expectObjectValueEqual(walletResetModal, expectedData, 'set your wallet password default data');
        return app.vm.$nextTick();
      })

      .then(() => flushPromises())
      .then(() => {
        walletResetModal.dialog = true;

        walletResetModal.newWalletPassword = 'fake password1';

        walletResetModal.WalletPassword = 'fake password';

        expect(walletResetModal.error).toBeNull();

        walletResetModal.resetWallet();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.buttonLabel = 'Set a password';
        expectedData.dialog = true;
        expectedData.loading = true;
        expectedData.rememberPasswordToChange = true;

        expectedData.rememberPasswordStored = true;
        expectedData.autoGeneratedPassword = true;
        expectedData.backedUp = true;
        expectedData.newWalletPassword = 'fake password1';
        expectObjectValueEqual(walletResetModal, expectedData, 'reset your wallet password default data');
        return app.vm.$nextTick();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('walletResetModal - test dialog when remember password  to change is false', () => {
    global.walletAddress = global.walletAddresses[1];
    const app = getWalletApp();

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => {
        const walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        const walletResetModal = walletSetup.$refs.walletResetModal;
        expect(walletResetModal).toBeTruthy();

        walletResetModal.dialog = true;

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.buttonLabel = 'Set a password';
        expectedData.dialog = true;
        expectedData.rememberPasswordStored = true;
        expectedData.autoGeneratedPassword = true;
        expectObjectValueEqual(walletResetModal, expectedData, 'reset your wallet password default data');
        return app.vm.$nextTick();
      });
  });
});
