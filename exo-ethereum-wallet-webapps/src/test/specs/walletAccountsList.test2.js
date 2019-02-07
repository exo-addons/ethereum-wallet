import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther, approveTokens, approveAccount} from '../TestUtils.js';

import WalletAccountList from '../../main/webapp/vue-app/components/WalletAccountsList.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletAccountList.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    isReadOnly: false,
    accountsDetails: {},
    overviewAccounts: [],
    fiatSymbol: false,
    principalAccount: false,
    walletAddress: false,
    networkId: 0,
    sendEtherModal: false,
    sendTokenModal: false,
    delegateTokenModal: false,
    sendDelegatedTokenModal: false,
    enableDelegation: false,
    selectedItem: false,
    accountsList: [],
  };

  it('WalletAccountList default data', () => {
    console.log('-- WalletAccountList default data');

    const walletAccountList = mount(WalletAccountList, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletAccountList.vm, defaultAttributesValues, 'walletAccountList default data');
  });

  it('WalletAccountList - test wallet Accounts List is enabled ', (done) => {
    console.log('--- WalletAccountList - test wallet Accounts List is enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails, WalletAccountList;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        expect(app.findAll('#WalletApp #walletAccountsList')).toHaveLength(1);
        return flushPromises();
      })

      .then(() => {
        WalletAccountList = app.vm.$refs.walletAccountsList;
        expect(WalletAccountList).toBeTruthy();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletAccountList when send Ether test transaction-sent event', () => {
    console.log('-- Test transaction-sent event');

    const walletAccountList = mount(WalletAccountList, {
      attachToDocument: true,
    });

    const transaction = {
      from: global.walletAddress,
      to: global.walletAddress,
      value: 1,
    };
    expect(walletAccountList.emitted()['transaction-sent']).toBeFalsy();
    walletAccountList.vm.addSendEtherTransaction(transaction);
    expect(walletAccountList.emitted()['transaction-sent']).toBeTruthy();
  });

  it('WalletAccountList when send Token test transaction-sent event', () => {
    console.log('-- Test transaction-sent event');

    const walletAccountList = mount(WalletAccountList, {
      attachToDocument: true,
    });

    const transaction = {
      from: global.walletAddress,
      to: global.walletAddress,
      value: 1,
    };
    expect(walletAccountList.emitted()['transaction-sent']).toBeFalsy();
    walletAccountList.vm.addSendTokenTransaction(transaction, {});
    expect(walletAccountList.emitted()['transaction-sent']).toBeTruthy();
  });
});
