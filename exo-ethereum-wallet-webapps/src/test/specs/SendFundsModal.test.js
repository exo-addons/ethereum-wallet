import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendFundsModal from '../../main/webapp/vue-app/components/SendFundsModal';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendFundsModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    accountsDetails: {},
    overviewAccounts: [],
    principalAccount: false,
    refreshIndex: 1,
    networkId: 0,
    walletAddress: false,
    icon: false,
    displayAllAccounts: false,
    disabled: false,
    addPendingToReceiver: false,
    noButton: false,
    selectedOption: false,
    error: false,
    dialog: false,
  };

  it('SendFundsModal default data', () => {
    console.log('-- Test SendFundsModal default data');

    const sendFundsModal = mount(SendFundsModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendFundsModal.vm, defaultAttributesValues, 'SendFundsModal default data');
    expectCountElement(sendFundsModal, 'sendFundsModal', 1);
  });

  it('SendFundsModal - Send Funds Modal Enabled', (done) => {
    console.log('--- SendFundsModal - Send Funds Modal Enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.walletAddress = global.walletAddress;
        expectedData.overviewAccounts = global.defaultWalletSettings.defaultOverviewAccounts;
        expectedData.disabled = true;
        expectedData.refreshIndex = 7;
        expectedData.networkId = 4452364;
        expectedData.principalAccount = global.defaultWalletSettings.defaultPrincipalAccount;

        const sendFundsModal = walletSummary.$refs.sendFundsModal;
        expect(sendFundsModal).toBeTruthy();
        expectObjectValueEqual(sendFundsModal, expectedData, 'Send Funds Modal Enabled', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('SendFundsModal - test prepareSendForm', (done) => {
    console.log('--- sendFundsModal - test prepareSendForm  ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, contractDetails, sendFundsModal;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
        return flushPromises();
      })

      .then(() => {
        sendFundsModal = walletSummary.$refs.sendFundsModal;
        expect(sendFundsModal).toBeTruthy();
        sendFundsModal.open = true;
        expect(sendFundsModal.selectedOption).toBeNull();
        sendFundsModal.prepareSendForm(global.walletAddresses[0], null, null, 'Curries', null, true);
        expect(sendFundsModal.error).toBeNull();
        sendFundsModal.success();
        expect(sendFundsModal.error).toBeNull();

        return flushPromises();
      })

      .then(() => {
        expect(sendFundsModal.selectedOption).not.toBeNull();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.walletAddress = global.walletAddress;
        expectedData.overviewAccounts = global.defaultWalletSettings.defaultOverviewAccounts;
        expectedData.disabled = false;
        expectedData.dialog = true;
        expectedData.refreshIndex = 10;
        expectedData.networkId = 4452364;
        expectedData.principalAccount = global.defaultWalletSettings.defaultPrincipalAccount;
        expectedData.selectedOption = {};

        expectObjectValueEqual(sendFundsModal, expectedData, 'SendFundsModal  test prepareSendForm', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
