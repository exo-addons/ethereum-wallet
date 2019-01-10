import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendTokensForm from '../../main/webapp/vue-app/components/SendTokensForm';

import {mount} from '@vue/test-utils';
import {hashCode} from '../../main/webapp/vue-app/WalletUtils.js';

import flushPromises from 'flush-promises';

describe('SendTokensForm.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    account: false,
    contractDetails: {},
    loading: false,
    showQRCodeModal: false,
    storedPassword: false,
    transactionLabel: '',
    transactionMessage: '',
    walletPassword: '',
    walletPasswordShow: false,
    useMetamask: false,
    recipient: false,
    isApprovedRecipient: true,
    canSendToken: true,
    amount: false,
    gasPrice: 0,
    estimatedGas: 0,
    fiatSymbol: false,
    warning: false,
    information: false,
    error: false,
    transactionFeeString: '',
    sellPriceInWei: 0,
    transactionFeeInWei: 0,
    transactionFeeEther: 0,
    transactionFeeFiat: 0,
    transactionFeeToken: 0,
  };

  it('SendTokensForm default data', () => {
    console.log('-- Test SendTokensForm default data');

    const sendTokensForm = mount(SendTokensForm, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendTokensForm.vm, defaultAttributesValues, 'SendTokensForm default data: ');
    expectCountElement(sendTokensForm, 'sendTokenForm', 1);
  });

  it('sendTokensForm - test send form data', (done) => {
    console.log('--- sendTokensForm - test send form data');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal, sendTokensForm, initialBalance;
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
        expect(sendTokensForm.transactionFeeString).toBe('');
        expect(sendTokensForm.transactionFeeInWei).toBe(0);
        expect(sendTokensForm.transactionFeeInWei).toBe(0);
        expect(sendTokensForm.transactionFeeEther).toBe(0);
        expect(sendTokensForm.transactionFeeFiat).toBe(0);
        expect(sendTokensForm.estimatedGas).toBe(0);

        return flushPromises();
      })

      .then(() => {
        expect(sendTokensForm.error).toBeNull();
        sendTokensForm.amount = 'invalid ammount';
        return flushPromises();
      })

      .then(() => {
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.amount = 999999999999999999999999;
        return flushPromises();
      })

      .then(() => {
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.recipient = global.walletAddresses[0];
        return flushPromises();
      })

      .then(() => contractDetails.contract.methods.balanceOf(global.walletAddress).call())
      .then((balance) => {
        initialBalance = Number(balance);
      })
      .then(() => {
        expect(sendTokensForm.error).toBeTruthy();

        sendTokensForm.recipient = 0x1111111111111111111111111111111111111111111111111111111111111111;
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.recipient = global.walletAddresses[3];

        sendTokensForm.amount = '2a'; // in tribe pass
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.amount = 35;

        sendTokensForm.storedPassword = false;
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.walletPassword = 'fake password';
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.storedPassword = true;

        sendTokensForm.canSendToken = false;
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.canSendToken = true;

        sendTokensForm.contractDetails.isPaused = true;
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        sendTokensForm.contractDetails.isPaused = false;

        const initialBalance = contractDetails.balance;
        contractDetails.balance = 0;
        sendTokensForm.sendTokens();
        expect(sendTokensForm.error).toBeTruthy();
        contractDetails.balance = initialBalance;
        return sendTokensForm.sendTokens();
      })

      .then(() => contractDetails.contract.methods.balanceOf(sendTokensForm.recipient).call())
      .then((balance) => {
        expect(Number(balance)).toEqual(35 * Math.pow(10, global.tokenDecimals));
        return flushPromises();
      })
      .then(() => contractDetails.contract.methods.balanceOf(global.walletAddress).call())
      .then((balance) => {
        expect(Number(balance)).toEqual(initialBalance - 35 * Math.pow(10, global.tokenDecimals));
        return flushPromises();
      })

      .then(() => {
        expect(sendTokensForm.transactionFeeString).not.toBe('');
        expect(sendTokensForm.transactionFeeInWei).not.toBe(0);
        expect(sendTokensForm.transactionFeeEther).not.toBe(0);
        expect(sendTokensForm.transactionFeeFiat).not.toBe(0);
        expect(sendTokensForm.estimatedGas).not.toBe(0);
      })

      .then(() => {
        const senderTransactions = getTransactions(global.walletAddress);
        const receiverTransactions = getTransactions(sendTokensForm.recipient);
        expect(senderTransactions).toBeTruthy();
        expect(receiverTransactions).toBeTruthy();
        expect(senderTransactions.length > 0).toBeTruthy();
        expect(receiverTransactions.length > 0).toBeTruthy();
        done();
      })
      .catch((e) => {
        done(e);
      });
  });
});
