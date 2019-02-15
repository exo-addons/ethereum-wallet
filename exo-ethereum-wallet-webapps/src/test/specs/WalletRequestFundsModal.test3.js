import {getWalletApp, initApp, expectObjectValueEqual} from '../TestUtils.js';

import WalletRequestFundsModal from '../../main/webapp/vue-app/components/WalletRequestFundsModal';

import {mount} from '@vue/test-utils';
import {hashCode} from '../../main/webapp/vue-app/WalletUtils.js';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletRequestFundsModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    icon: false,
    walletAddress: false,
    accountsDetails: {},
    overviewAccounts: [],
    principalAccount: false,
    selectedOption: false,
    recipient: false,
    amount: false,
    error: false,
    requestMessage: '',
    loading: false,
    dialog: false,
  };

  it('WalletRequestFundsModal default data', () => {
    console.log('-- Test WalletRequestFundsModal default data');

    const walletRequestFundsModal = mount(WalletRequestFundsModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletRequestFundsModal.vm, defaultAttributesValues, 'WalletRequestFundsModal default data: ');
  });

  it('WalletRequestFundsModal - test WalletRequestFundsModal enabled', (done) => {
    console.log('--- WalletRequestFundsModal - test WalletRequestFundsModal enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails, walletSummary, walletRequestFundsModal;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
      })

      .then(() => {
        walletRequestFundsModal = walletSummary.$refs.walletRequestFundsModal;
        expect(walletRequestFundsModal).toBeTruthy();
        expect(walletRequestFundsModal.error).toBeNull();
      })

      .then(() => {
        walletRequestFundsModal.requestFunds();
        expect(walletRequestFundsModal.error).not.toBeNull();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletRequestFundsModal - test send Request', (done) => {
    console.log('--- WalletRequestFundsModal - test send Request');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails, walletSummary, walletRequestFundsModal;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        walletSummary = app.vm.$refs.walletSummary;
        expect(walletSummary).toBeTruthy();
      })

      .then(() => {
        walletRequestFundsModal = walletSummary.$refs.walletRequestFundsModal;
        expect(walletRequestFundsModal).toBeTruthy();
        walletRequestFundsModal.dialog = true;
        expect(walletRequestFundsModal.error).toBeNull();
      })

      .then(() => {
        walletRequestFundsModal.recipient = global.walletAddresses[5];
        walletRequestFundsModal.amount = 4;
        walletRequestFundsModal.requestFunds();
        expect(walletRequestFundsModal.error).toBeNull();
      })

      .then(() => {
        walletRequestFundsModal.recipient = global.walletAddresses[4444444];
        walletRequestFundsModal.requestFunds();
        expect(walletRequestFundsModal.error).not.toBeNull();
        return flushPromises();
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
