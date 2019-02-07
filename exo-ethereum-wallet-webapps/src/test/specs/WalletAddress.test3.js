import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther, approveTokens, approveAccount} from '../TestUtils.js';

import WalletAddress from '../../main/webapp/vue-app/components/WalletAddress.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletAddress.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    value: '',
    allowCopy: true,
  };

  it('WalletAddress default data', () => {
    console.log('-- WalletAddress default data');

    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletAddress.vm, defaultAttributesValues, 'WalletAddress default data');
  });

  it('WalletAddress - test copy address ', (done) => {
    console.log('--- WalletAddress - test copy address  ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, contractDetails, walletReceiveModal, walletAddress;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
        expect(app.findAll('#waletSummary .walletReceiveModal')).toHaveLength(1);
        return flushPromises();
      })

      .then(() => {
        walletReceiveModal = walletSummary.$refs.walletReceiveModal;
        expect(walletReceiveModal).toBeTruthy();
        walletReceiveModal.dialog = true;
        return flushPromises();
      })

      .then(() => {
        walletAddress = walletReceiveModal.$refs.walletAddress;
        expect(walletAddress).toBeTruthy();
        walletAddress.copyToClipboard();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.value = '0x627306090abab3a6e1400e9345bc60c78a8bef57';
        expectedData.allowCopy = true;
        expectObjectValueEqual(walletAddress, expectedData, ' walletAddress, test copy address', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
