import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import QRCode from '../../main/webapp/vue-app/components/QRCode.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('QRCode.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    netId: false,
    information: false,
    amount: 0,
    from: false,
    to: false,
    isContract: false,
    functionName: false,
    functionPayable: false,
    argsNames: [],
    argsTypes: [],
    argsValues: [],
  };

  it('QRCode default data', () => {
    console.log('-- QRCode default data');

    const qrCode = mount(QRCode, {
      attachToDocument: true,
    });

    expectObjectValueEqual(qrCode.vm, defaultAttributesValues, 'QRCode default data');
  });

  it('QRCode - test generating QR code when want to receive funds ', (done) => {
    console.log('--- QRCode - test generating QR code when want to receive funds   ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let walletSummary, contractDetails, walletReceiveModal, qrCode;
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
        walletReceiveModal = walletSummary.$refs.walletReceiveModal;
        expect(walletReceiveModal).toBeTruthy();
        walletReceiveModal.dialog = true;
        return flushPromises();
      })

      .then(() => {
        qrCode = walletReceiveModal.$refs.qrCode;
        expect(qrCode).toBeTruthy();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.information = 'You can send this Wallet address or QR code to other users to send you ether or tokens';
        expectedData.to = global.walletAddress;
        expectObjectValueEqual(qrCode, expectedData, ' QRCode,  test generating QR code when want to receive funds', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('QRCodeModal - Test generating QRCode to delgate tokens', (done) => {
    console.log('--- QRCodeModal - Test generating QRCode to delgate tokens ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, delegateTokensModal, qrCodeModal, qrCode;
    return (
      initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
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
          delegateTokensModal = accountDetailCmp.$refs.delegateTokensModal;
          expect(delegateTokensModal).toBeTruthy();
          delegateTokensModal.open = true;
          delegateTokensModal.showQRCodeModal = true;

          delegateTokensModal.recipient = global.walletAddresses[2];
          delegateTokensModal.amount = 4;

          return flushPromises();
        })

        .then(() => {
          qrCodeModal = delegateTokensModal.$refs.qrCodeModal;
          expect(qrCodeModal).toBeTruthy();
          qrCodeModal.open = true;
          return flushPromises();
        })

        .then(() => {
          qrCodeModal.to = global.walletAddresses[2];
          qrCodeModal.amount = 4;
          return flushPromises();
        })

        .then(() => {
          qrCode = qrCodeModal.$refs.qrCode;
          expect(qrCode).toBeTruthy();
          return flushPromises();
        })

        .then(() => {
          expect(qrCode.information).toBe('You can scan this QR code by using a different application that supports QR code transaction generation to delegate tokens');
          expect(qrCode.to).toBe(global.walletAddresses[2]);
          expect(qrCode.amount).toBe(4);
          expect(qrCode.isContract).toBeTruthy();
          expect(qrCode.functionName).toBe('approve');
          expect(qrCode.argsNames).toEqual(['_spender', '_value']);
          expect(qrCode.argsTypes).toEqual(['address', 'uint256']);
        })

        .then(() => done())
        .catch((e) => done(e))
    );
  });

  it('QRCodeModal - Test generating QRCode to send tokens', (done) => {
    console.log('--- QRCodeModal - Test generating QRCode to send tokens ');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal, sendTokensForm, qrCodeModal, qrCode;
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
        return flushPromises();
      })
      .then(() => {
        qrCode = qrCodeModal.$refs.qrCode;
        expect(qrCode).toBeTruthy();
        return flushPromises();
      })

      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.information = 'You can scan this QR code by using a different application that supports QR code transaction generation to send tokens';
        expectedData.to = global.walletAddresses[3];
        expectedData.from = global.walletAddresses[0];
        expectedData.amount = 2;
        expectedData.isContract = true;
        expectedData.functionName = 'transfer';
        expectedData.argsNames = ['_to', '_value'];
        expectedData.argsTypes = ['address', 'uint256'];
        expectedData.argsValues = [global.walletAddresses[3], 2];
        expectObjectValueEqual(qrCode, expectedData, ' QRCode, Test generating QRCode to send tokens', null, true);
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
