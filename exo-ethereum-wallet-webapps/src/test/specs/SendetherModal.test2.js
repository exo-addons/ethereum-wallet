import {getWalletApp, initApp, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import SendEtherModal from '../../main/webapp/vue-app/components/SendEtherModal';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendEtherModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    account: false,
    isReadonly: false,
    noButton: false,
    useNavigation: false,
    open: false,
    balance: false,
    recipient: false,
    dialog: false,
    disabled: true,
  };

  it('SendEtherModal default data', () => {
    console.log('-- Test SendEtherModal default data');

    const sendFundsModal = mount(SendEtherModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendFundsModal.vm, defaultAttributesValues, 'SendEtherModal default data');
  });

  it('sendEtherModal - sendEtherModal enabled   ', (done) => {
    console.log('--- sendEtherModal - sendEtherModal enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app)
      .then(() => flushPromises())
      .then(() => {
        const contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();
        expect(app.findAll('#accountDetail .sendEtherModal')).toHaveLength(1);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('sendEtherModal - test wallet browser enabled', (done) => {
    console.log('--- sendEtherModal - test wallet browser enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendEtherModal;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();
        return flushPromises();
      })
      .then(() => {
        sendEtherModal = accountDetailCmp.$refs.sendEtherModal;
        expect(sendEtherModal).toBeTruthy();
        sendEtherModal.open = true;
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.account = global.walletAddress;
        expectedData.isReadonly = false;
        expectedData.open = true;
        expectedData.dialog = true;
        expectedData.disabled = false;
        expectedData.useNavigation = true;
        expectedData.balance = contractDetails.balance;
        expectObjectValueEqual(sendEtherModal, expectedData, 'test wallet browser enabled', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
