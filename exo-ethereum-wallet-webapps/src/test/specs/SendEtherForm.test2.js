import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import SendEtherForm from '../../main/webapp/vue-app/components/SendEtherForm';

import {mount} from '@vue/test-utils';
import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendEtherForm.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    account: false,
    balance: 0,
    showQRCodeModal: false,
    storedPassword: false,
    transactionLabel: '',
    transactionMessage: '',
    transactionHash: false,
    walletPassword: '',
    walletPasswordShow: false,
    useMetamask: false,
    loading: false,
    recipient: false,
    amount: false,
    gasPrice: 0,
    error: false,
  };

  it('SendEtherForm default data', (done) => {
    console.log('-- Test SendEtherForm default data');

    try {
      const sendEtherForm = mount(SendEtherForm, {
        attachToDocument: true,
      });

      expectObjectValueEqual(sendEtherForm.vm, defaultAttributesValues, 'SendEtherForm default data');
      expectCountElement(sendEtherForm, 'sendEtherForm', 1);
      done();
    } catch (e) {
      done(e);
    }
  });

  it('SendEtherForm - test send Ether Form', (done) => {
    console.log('--- test send Ether');

    global.walletAddress = global.walletAddresses[0];

    let sendEtherModal, sendEtherForm, accountDetails;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        accountDetails = app.vm.accountsDetails[global.walletAddress];
        expect(accountDetails).not.toBeNull();
        app.vm.openAccountDetail(accountDetails);
        return app.vm.$nextTick();
      })
      .then(() => flushPromises())
      .then(() => {
        expectCountElement(app, 'accountDetail', 1);

        const accountDetailCmp = app.vm.$refs.accountDetail;
        sendEtherModal = accountDetailCmp.$refs.sendEtherModal;
        expect(sendEtherModal).not.toBeNull();
        sendEtherModal.dialog = true;

        return app.vm.$nextTick();
      })
      .then(() => flushPromises())
      .then(() => {
        sendEtherForm = sendEtherModal.$refs.sendEtherForm;
        expect(sendEtherForm).not.toBeNull();

        // Send empty form
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).not.toBeNull();

        // Send empty amount
        sendEtherForm.recipient = global.walletAddresses[3];
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).not.toBeNull();

        // Send empty amount
        sendEtherForm.amount = 'invalid number';
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).not.toBeNull();

        // Send amount > balance
        sendEtherForm.amount = 100000000000000000000000;
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).not.toBeNull();

        sendEtherForm.storedPassword = false;
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).toBeTruthy();
        sendEtherForm.walletPassword = 'fake password';
        sendEtherForm.sendEther();
        expect(sendEtherForm.error).toBeTruthy();
        sendEtherForm.storedPassword = true;

        // Send form
        sendEtherForm.amount = 1;
        const promiseResult = sendEtherForm.sendEther();
        expect(sendEtherForm.error).toBeNull();
        expect(promiseResult && promiseResult.then).toBeTruthy();
        return promiseResult;
      })
      .then(() => flushPromises())
      .then(() => {
        expect(sendEtherForm.transactionHash).not.toBeNull();
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
