import {getWalletApp, initApp, expectObjectValueEqual} from '../TestUtils.js';

import WalletImportKeyModal from '../../main/webapp/vue-app/components/WalletImportKeyModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletImportKeyModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    isSpace: false,
    walletAddress: false,
    error: false,
  };

  it('WalletImportKeyModal default data', () => {
    console.log('-- WalletImportKeyModal default data');

    const walletImportKeyModal = mount(WalletImportKeyModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletImportKeyModal.vm, defaultAttributesValues, 'walletImportKeyModal default data');
  });

  it('WalletImportKeyModal -test when you want to restore existing wallet private key (without 0x) ', (done) => {
    console.log('--WalletImportKeyModal test when you want to restore existing wallet private key (without 0x)');

    let walletSetup, walletBrowserSetup, walletImportKeyModal;

    return initApp(app)
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        walletSetup.displayWalletSetup = true;

        walletBrowserSetup = walletSetup.$refs.walletBrowserSetup;
        expect(walletBrowserSetup).toBeTruthy();

        expect(app.findAll('#walletBrowserSetup .walletImportKeyModal')).toHaveLength(1);

        walletImportKeyModal = walletBrowserSetup.$refs.walletImportKeyModal;
        expect(walletImportKeyModal).toBeTruthy();
      })
      .then(() => {
        walletImportKeyModal.walletPrivateKey = '9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949';
        walletImportKeyModal.importWallet();
        return flushPromises();
      })
      .then(() => {
        expect(walletImportKeyModal.error).toBeFalsy();

        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.walletAddress = global.walletAddress;
        expectObjectValueEqual(walletImportKeyModal, expectedData, 'Restore existing wallet private key (without 0x) ', null, true);
      })

      .then(() => app.vm.$nextTick())

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletImportKeyModal test when you want to restore existing wallet private key (with 0x) ', (done) => {
    console.log('--WalletImportKeyModal test when you want to restore existing wallet private key (with 0x) ');

    let walletSetup, walletBrowserSetup, walletImportKeyModal;

    return initApp(app)
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        walletSetup.displayWalletSetup = true;

        walletBrowserSetup = walletSetup.$refs.walletBrowserSetup;
        expect(walletBrowserSetup).toBeTruthy();

        walletImportKeyModal = walletBrowserSetup.$refs.walletImportKeyModal;
        expect(walletImportKeyModal).toBeTruthy();
      })
      .then(() => {
        walletImportKeyModal.walletPrivateKey = '0x9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949';
        walletImportKeyModal.importWallet();
        expect(walletImportKeyModal.error).toBeFalsy();
        return flushPromises();
      })
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.walletAddress = global.walletAddress;
        expectObjectValueEqual(walletImportKeyModal, expectedData, 'restore existing wallet private key (with 0x)', null, true);
      })

      .then(() => app.vm.$nextTick())
      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletImportKeyModal no existing  private key ', (done) => {
    console.log('--WalletImportKeyModal no existing private key ');

    let walletSetup, walletBrowserSetup, walletImportKeyModal;

    return initApp(app)
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        walletSetup.displayWalletSetup = true;

        walletBrowserSetup = walletSetup.$refs.walletBrowserSetup;
        expect(walletBrowserSetup).toBeTruthy();

        walletImportKeyModal = walletBrowserSetup.$refs.walletImportKeyModal;
        expect(walletImportKeyModal).toBeTruthy();
      })

      .then(() => {
        walletImportKeyModal.walletPrivateKey = 'test';
        expect(walletImportKeyModal.$refs.form.validate()).toBeFalsy();
      })
      .then(() => app.vm.$nextTick())
      .then(() => done())
      .catch((e) => done(e));
  });
});
