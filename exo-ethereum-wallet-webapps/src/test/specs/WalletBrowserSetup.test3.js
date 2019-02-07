import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import WalletBrowserSetup from '../../main/webapp/vue-app/components/WalletBrowserSetup';

import {mount} from '@vue/test-utils';
import {hashCode} from '../../main/webapp/vue-app/WalletUtils.js';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletBrowserSetup.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    isSpace: false,
    isSpaceAdministrator: false,
    isAdministration: false,
    loading: false,
    walletAddress: global.walletAddresses[1],
    loadingWalletBrowser: false,
  };

  it('WalletBrowserSetup default data', () => {
    console.log('-- Test WalletBrowserSetup default data');

    const walletBrowserSetup = mount(WalletBrowserSetup, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletBrowserSetup.vm, defaultAttributesValues, 'walletBrowserSetup default data: ');
  });

  it('WalletBrowserSetup test when creating new wallet Address', (done) => {
    console.log('-- Test WalletBrowserSetup test when creating new wallet Address');

    let walletSetup, walletBrowserSetup;
    return initApp(app)
      .then(() => app.vm.$nextTick())
      .then(() => {
        walletSetup = app.vm.$refs.walletSetup;
        expect(walletSetup).toBeTruthy();

        walletSetup.displayWalletSetup = true;

        walletBrowserSetup = walletSetup.$refs.walletBrowserSetup;
        expect(walletBrowserSetup).toBeTruthy();

        walletBrowserSetup.createWallet();

        expect(walletBrowserSetup.loadingWalletBrowser).toBeTruthy();

        return flushPromises();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletBrowserSetup test when switchToMetamask event', () => {
    console.log('-- WalletBrowserSetup test when switchToMetamask event');

    const walletBrowserSetup = mount(WalletBrowserSetup, {
      attachToDocument: true,
    });

    walletBrowserSetup.vm.switchToMetamask();
    expect(walletBrowserSetup.emitted()['configured']).toBeTruthy();
  });
});
