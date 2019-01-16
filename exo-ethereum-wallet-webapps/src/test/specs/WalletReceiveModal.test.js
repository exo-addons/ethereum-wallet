import {getWalletApp, initApp, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import WalletReceiveModal from '../../main/webapp/vue-app/components/WalletReceiveModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletReceiveModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    icon: false,
    walletAddress: false,
    dialog: false,
  };

  it('WalletReceiveModal default data', () => {
    console.log('-- WalletReceiveModal default data');

    const walletReceiveModal = mount(WalletReceiveModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletReceiveModal.vm, defaultAttributesValues, 'WalletReceiveModal default data');
  });

  it('WalletReceiveModal - test dialog ', (done) => {
    console.log('--- WalletReceiveModal - test dialog  ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, contractDetails, walletReceiveModal;
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
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.icon = false;
        expectedData.walletAddress = global.walletAddress;
        expectedData.dialog = true;
        expectObjectValueEqual(walletReceiveModal, expectedData, ' walletReceiveModal, test dialog', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
