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
    dialog: false,
    walletPrivateKey: '',
    walletPrivateKeyShow: false,
    error: false,
    loading: false,
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
        walletImportKeyModal.dialog = true;
        walletImportKeyModal.walletPrivateKey = '9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949';
        walletImportKeyModal.importWallet();
        expect(walletImportKeyModal.error).toBeNull();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.walletAddress = global.walletAddress;
        expectedData.loading = true;
        expectedData.dialog = true;
        expectedData.walletPrivateKey = '9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949';
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
        walletImportKeyModal.dialog = true;
        walletImportKeyModal.walletPrivateKey = '0x9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949';
        walletImportKeyModal.importWallet();
        expect(walletImportKeyModal.error).toBeNull();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.walletAddress = global.walletAddress;
        expectedData.dialog = true;
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
        walletImportKeyModal.dialog = true;
        walletImportKeyModal.walletPrivateKey = 'test';
        walletImportKeyModal.importWallet();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.walletAddress = global.walletAddress;
        expectedData.dialog = true;
        expectObjectValueEqual(walletImportKeyModal, expectedData, 'when no existing private key', null, true);
      })

      .then(() => app.vm.$nextTick())
      .then(() => done())
      .catch((e) => done(e));
  });
});
