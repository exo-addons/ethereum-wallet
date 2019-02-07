import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import WalletSummary from '../../main/webapp/vue-app/components/WalletSummary';

import {mount} from '@vue/test-utils';
import {hashCode} from '../../main/webapp/vue-app/WalletUtils.js';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletSummary.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    accountsDetails: {},
    networkId: 0,
    walletAddress: false,
    isMaximized: false,
    isSpace: false,
    isSpaceAdministrator: false,
    hideActions: false,
    overviewAccounts: [],
    principalAccount: false,
    etherBalance: 0,
    totalBalance: 0,
    totalFiatBalance: 0,
    fiatSymbol: '$',
    isReadOnly: false,
    updatePendingTransactionsIndex: 1,
    pendingTransactions: {},
    disableSendButton: true,
    pendingTransactionsCount: false,
    principalAccountDetails: false,
    overviewAccountsArray: [],
  };

  it('WalletSummary default data', () => {
    console.log('-- Test WalletSummary default data');

    const walletSummary = mount(WalletSummary, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletSummary.vm, defaultAttributesValues, 'walletSummary default data: ');
  });

  it('WalletSummary - wallet summary is enabled', (done) => {
    console.log('--- test wallet summary is enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => app.vm.$nextTick())
      .then(() => {
        const accountDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(accountDetails).not.toBeNull();
        app.vm.openAccountDetail(accountDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        expectCountElement(app, 'waletSummary', 1);

        const walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletSummary --test refresh Balance  of default contract event', () => {
    console.log('-- WalletSummary --test refresh Balance  of default contract event');
    const walletSummary = mount(WalletSummary, {
      attachToDocument: true,
    });
    expect(walletSummary.emitted()['refresh-balance']).toBeFalsy();
    walletSummary.vm.refreshBalance(global.defaultWalletSettings.defaultContractsToDisplay);
    expect(walletSummary.emitted()['refresh-balance']).toBeTruthy();
  });

  it('WalletSummary - test refresh Balance  of token contract event', (done) => {
    console.log('--- test refresh Balance  of token contract event');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, accountDetails;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => app.vm.$nextTick())
      .then(() => {
        accountDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(accountDetails).not.toBeNull();
        app.vm.openAccountDetail(accountDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const walletSummary = mount(WalletSummary, {
          attachToDocument: true,
        });
        expect(walletSummary.emitted()['refresh-token-balance']).toBeFalsy();
        walletSummary.vm.refreshBalance(accountDetails);
        expect(walletSummary.emitted()['refresh-token-balance']).toBeTruthy();
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
