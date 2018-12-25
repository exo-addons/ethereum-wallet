import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendEtherForm from '../../main/webapp/vue-app/components/SendEtherForm';

import {mount} from '@vue/test-utils';
import {hashCode} from '../../main/webapp/vue-app/WalletUtils.js';

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

  it('SendEtherForm default data', () => {
    console.log('-- Test SendEtherForm default data');

    const sendEtherForm = mount(SendEtherForm, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendEtherForm.vm, defaultAttributesValues);
    expectCountElement(sendEtherForm, 'sendEtherForm', 1);
  });

  it('SendEtherModal - test send tokens and refresh balance', (done) => {
    console.log('--- test send tokens and refresh balance');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => {
        const accountDetails = app.vm.accountsDetails[global.walletAddress];
        expect(accountDetails).not.toBeNull();
        app.vm.openAccountDetail(accountDetails);

        return app.vm.$nextTick(() => {
          try {
            expectCountElement(app, 'accountDetail', 1);

            const accountDetailCmp = app.vm.$refs.accountDetail;
            const sendEtherModal = accountDetailCmp.$refs.sendEtherModal;
            expect(sendEtherModal).not.toBeNull();
            sendEtherModal.dialog = true;

            return app.vm.$nextTick(() => {
              const sendEtherForm = sendEtherModal.$refs.sendEtherForm;
              expect(sendEtherForm).not.toBeNull();

              console.log(sendEtherForm.account);
              console.log(window.walletSettings.userP);
              console.log(hashCode('testpassword'));

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

              // Send form
              sendEtherForm.amount = 1;
              const promiseResult = sendEtherForm.sendEther();
              console.warn(sendEtherForm.error);
              expect(sendEtherForm.error).toBeNull();
              expect(promiseResult && promiseResult.then).toBeTruthy();
              return promiseResult
                .then(() => {
                  expect(sendEtherForm.transactionHash).not.toBeNull();
                  done();
                })
                .catch((e) => {
                  done(e);
                });
            });
          } catch (e) {
            done(e);
          }
        });
      });
  });
});
