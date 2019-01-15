import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther, approveTokens, approveAccount, saveTransaction} from '../TestUtils.js';

import TransactionList from '../../main/webapp/vue-app/components/TransactionsList.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('TransactionList.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    networkId: 0,
    fiatSymbol: false,
    account: false,
    contractDetails: {},
    error: false,
    displayFullTransaction: false,
    selectedTransactionHash: false,
    refreshIndex: 1,
    loading: false,
    transactionsLimit: 10,
    transactionsPerPage: 10,
    limitReached: false,
    transactions: {},
    sortedTransactions: {},
  };

  it('TransactionList default data', () => {
    console.log('-- TransactionList default data');
    const transactionList = mount(TransactionList, {
      attachToDocument: true,
    });
    expectObjectValueEqual(transactionList.vm, defaultAttributesValues, 'TransactioList default data');
  });

  it('TransactionList - test transactionList enabled', (done) => {
    console.log('--- TransactionList - test transactionList enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app)
      .then(() => flushPromises())
      .then(() => {
        const contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();
        expect(app.findAll('#accountDetail .transactionsList')).toHaveLength(1);

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.networkId = 4452364;
        expectedData.fiatSymbol = '$';
        expectedData.account = global.walletAddress;
        expectedData.loading = true;

        const transactionList = accountDetailCmp.$refs.transactionsList;
        expect(transactionList).toBeTruthy();
        expectObjectValueEqual(transactionList, expectedData, 'transactionsList test transactions List enabled default data', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('send tokens see transaction', (done) => {
    console.log('send Tokens see transaction');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal, sendTokensForm, transactionList;
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
        transactionList = accountDetailCmp.$refs.transactionsList;
        expect(transactionList).toBeTruthy();
        transactionList.init();
        return flushPromises();
      })

      .then(() => {
        expect(transactionList.transactions).toEqual({});
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
        return flushPromises();
      })

      .then(() => {
        sendTokensForm.recipient = global.walletAddresses[2];
        sendTokensForm.amount = 2;
        sendTokensForm.storedPassword = true;
        const initialBalance = contractDetails.balance;
        return sendTokensForm.sendTokens();
      })

      .then(() => {
        expect(transactionList.transactions).not.toEqual({});
        return flushPromises();
      })

      .then(() => done())
      .catch((e) => done(e));
  });

  it('when sending the second transaction test sorted transaction', (done) => {
    console.log('sorted transaction');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal, sendTokensForm, transactionList;
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
        transactionList = accountDetailCmp.$refs.transactionsList;
        expect(transactionList).toBeTruthy();
        transactionList.init();
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
        return flushPromises();
      })

      .then(() => {
        sendTokensForm.recipient = global.walletAddresses[5];
        sendTokensForm.amount = 5;
        sendTokensForm.storedPassword = true;
        const initialBalance = contractDetails.balance;
        return sendTokensForm.sendTokens();
      })

      .then(() => {
        expect(transactionList.transactions).not.toEqual({});
        expect(transactionList.transactions['0x42309b1a8931d5a6bf7943e7887ca0023539f0f55657fe35b33b1c3603e4b65b']).not.toEqual({});
        expect(transactionList.transactions['0xbc921d43dc2e640ec8071e00b1c9fced8bc56644b943367b77c17c0e3d088b69']).not.toEqual({});
        return flushPromises();
      })

      .then(() => done())
      .catch((e) => done(e));
  });
});
