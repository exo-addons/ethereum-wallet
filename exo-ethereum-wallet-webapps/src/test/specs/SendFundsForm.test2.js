import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import SendFundsForm from '../../main/webapp/vue-app/components/SendFundsForm';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendFundsForm.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    networkId: 0,
    walletAddress: false,
    selectedAccount: {},
    formName: false,
    receiver: false,
    receiverType: false,
    notificationId: false,
    amount: false,
  };

  it('SendFundsForm default data', () => {
    console.log('-- Test SendFundsForm default data');

    const sendFundsForm = mount(SendFundsForm, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendFundsForm.vm, defaultAttributesValues, 'SendFundsForm default data: ');
    expectCountElement(sendFundsForm, 'sendFundsForm', 1);
  });

  it('SendFundsForm - test send token through send funds Form', (done) => {
    console.log('--- sendFundsForm - test send Token through send funds Form ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, contractDetails, sendFundsModal, sendFundsForm, sendTokensForm;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
        return flushPromises();
      })
      .then(() => {
        sendFundsModal = walletSummary.$refs.sendFundsModal;
        expect(sendFundsModal).toBeTruthy();
        expect(sendFundsModal.selectedOption).toBeNull();
        sendFundsModal.prepareSendForm('testuser', 'user', 7, 'Curries', null, true);
        expect(sendFundsModal.error).toBeNull();

        return flushPromises();
      })

      .then(() => {
        sendFundsForm = sendFundsModal.$refs.sendFundsForm;
        expect(sendFundsForm).toBeTruthy();

        sendFundsForm.prepareSendForm('testuser', 'user', 7, 'Curries', null, true);

        sendTokensForm = sendFundsForm.$refs.sendTokensForm;
        expect(sendTokensForm).toBeTruthy();

        return flushPromises();
      })

      .then(() => {
        expect(sendFundsModal.selectedOption).not.toBeNull();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.walletAddress = global.walletAddress;
        expectedData.networkId = 4452365;
        expectedData.formName = 'token';

        expectedData.receiver = 'testuser';
        expectedData.receiverType = 'user';
        expectedData.amount = 7;

        expectObjectValueEqual(sendFundsForm, expectedData, 'SendFundsModal test send token through send funds Form', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('SendFundsForm - test send ether through send funds Form', (done) => {
    console.log('--- sendFundsForm - test send ether through send funds Form');

    global.walletAddress = global.walletAddresses[0];
    const app = getWalletApp();
    let walletSummary, contractDetails, sendFundsModal, sendFundsForm, sendEtherForm;
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
        expect(sendFundsModal.selectedOption).toBeNull();
        sendFundsModal.prepareSendForm('testuser', 'user', 5, null, null, true);
        expect(sendFundsModal.error).toBeNull();

        return flushPromises();
      })

      .then(() => {
        sendFundsForm = sendFundsModal.$refs.sendFundsForm;
        expect(sendFundsForm).toBeTruthy();

        sendFundsForm.prepareSendForm('testuser', 'user', 5, null, null, true);

        sendEtherForm = sendFundsForm.$refs.sendEtherForm;
        expect(sendEtherForm).toBeTruthy();

        return flushPromises();
      })

      .then(() => {
        expect(sendFundsModal.selectedOption).not.toBeNull();

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.walletAddress = global.walletAddress;
        expectedData.networkId = 4452365;
        expectedData.formName = 'ether';
        expectedData.amount = 5;
        expectedData.receiver = 'testuser';
        expectedData.receiverType = 'user';

        expectObjectValueEqual(sendFundsForm, expectedData, 'SendFundsModal test send ether through send funds Form', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('SendFundsForm transaction-pending event', () => {
    console.log('-- Test transaction-pending event');

    const sendFundsForm = mount(SendFundsForm, {
      attachToDocument: true,
    });

    const transaction = {
      from: global.walletAddress,
      to: global.walletAddress,
      value: 1,
    };
    sendFundsForm.vm.addPendingTransaction(transaction, {});
    expect(sendFundsForm.emitted()['pending']).toBeTruthy();
  });
});
