import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import QRCodeModal from '../../main/webapp/vue-app/components/QRCodeModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('QRCodeModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    title: false,
    information: false,
    open: false,
    amount: 0,
    from: false,
    to: false,
    isContract: false,
    functionName: false,
    functionPayable: false,
    argsNames: [],
    argsTypes: [],
    argsValues: [],
    dialog: false,
    netId: null,
  };

  it('QRCodeModal default data', () => {
    console.log('-- QRCodeModal default data');

    const qrCodeModal = mount(QRCodeModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(qrCodeModal.vm, defaultAttributesValues, 'QRCodeModal default data');
  });

  it('QRCodeModal - Test generating QRCode to send tokens', (done) => {
    console.log('--- QRCodeModal - Test generating QRCode to send tokens ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal, sendTokensForm, qrCodeModal;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();
        return flushPromises();
      })

      .then(() => {
        sendTokensModal = accountDetailCmp.$refs.sendTokensModal;
        expect(sendTokensModal).toBeTruthy();
        sendTokensModal.open = true;
        return flushPromises();
      })

      .then(() => {
        sendTokensForm = sendTokensModal.$refs.sendTokensForm;
        expect(sendTokensForm).toBeTruthy();
        sendTokensForm.showQRCodeModal = true;

        sendTokensForm.recipient = global.walletAddresses[3];
        sendTokensForm.amount = 2;

        return flushPromises();
      })

      .then(() => {
        qrCodeModal = sendTokensForm.$refs.qrCodeModal;
        expect(qrCodeModal).toBeTruthy();
        qrCodeModal.open = true;
        return flushPromises();
      })

      .then(() => {
        qrCodeModal.to = global.walletAddresses[3];
        qrCodeModal.amount = 2;
        qrCodeModal.netId = 1234;
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.title = 'Send Tokens QR Code';
        expectedData.information = 'You can scan this QR code by using a different application that supports QR code transaction generation to send tokens';
        expectedData.to = global.walletAddresses[3];
        expectedData.from = global.walletAddresses[0];
        expectedData.amount = 2;
        expectedData.isContract = true;
        expectedData.open = true;
        expectedData.dialog = true;
        expectedData.functionName = 'transfer';
        expectedData.argsNames = ['_to', '_value'];
        expectedData.argsTypes = ['address', 'uint256'];
        expectedData.argsValues = [global.walletAddresses[3], 2];
        expectedData.netId = 1234;
        expectObjectValueEqual(qrCodeModal, expectedData, ' QRCode, Test generating QRCode to delgate tokens', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
