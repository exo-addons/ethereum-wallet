import {getWalletApp, initApp, getTransactions, approveAccount, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import DelegateTokensModal from '../../main/webapp/vue-app/components/DelegateTokensModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('DelegateTokensModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    contractDetails: {},
    noButton: false,
    isReadonly: false,
    useNavigation: false,
    open: false,
    showQRCodeModal: false,
    storedPassword: false,
    walletPassword: false,
    walletPasswordShow: false,
    useMetamask: false,
    recipient: false,
    loading: false,
    amount: false,
    gasPrice: false,
    dialog: false,
    warning: false,
    error: false,
    balance: false,
    etherBalance: false,
    disabled: true,
    buttonDisabled: true,
  };

  it('DelegateTokensModal default data', () => {
    console.log('-- DelegateTokensModal default data');

    const delegateTokensModal = mount(DelegateTokensModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(delegateTokensModal.vm, defaultAttributesValues, 'DelegateTokensModal default data');
  });

  it('DelegateTokensModal - test delegation disabled', (done) => {
    console.log('--- DelegateTokensModal - test delegation disabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = false;

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
        expect(app.findAll('#accountDetail .delegateTokenModal')).toHaveLength(0);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('DelegateTokensModal - test delegation enabled', (done) => {
    console.log('--- DelegateTokensModal - test delegation enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let contractDetails;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();
        expect(app.findAll('#accountDetail .delegateTokenModal')).toHaveLength(1);

        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.isReadonly = true;
        expectedData.useNavigation = true;
        expectedData.balance = contractDetails.balance;
        expectedData.etherBalance = contractDetails.etherBalance;
        expectedData.contractDetails = {
          address: global.tokenAddress,
          isContract: true,
          isDefault: true,
          defaultContract: true,
          isApproved: true,
          isAdmin: true,
          isPaused: false,
          contractType: 2,
          networkId: global.testNetworkId,
          name: global.tokenName,
          title: global.tokenName,
          symbol: global.tokenSymbol,
          decimals: global.tokenDecimals,
          totalSupply: global.tokenSupply,
          sellPrice: global.tokenSellPrice,
          owner: global.walletAddresses[0],
          isOwner: true,
          adminLevel: 5,
        };

        const delegateTokensModal = accountDetailCmp.$refs.delegateTokensModal;
        expect(delegateTokensModal).toBeTruthy();
        expectObjectValueEqual(delegateTokensModal, expectedData, 'DelegateTokensModal test delegation enabled default data', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('DelegateTokensModal - test wallet browser enabled', (done) => {
    console.log('--- DelegateTokensModal - test wallet browser enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, delegateTokensModal;
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
        delegateTokensModal = accountDetailCmp.$refs.delegateTokensModal;
        expect(delegateTokensModal).toBeTruthy();
        delegateTokensModal.open = true;
        return flushPromises();
      })
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.isReadonly = false;
        expectedData.open = true;
        expectedData.dialog = true;
        expectedData.disabled = false;
        expectedData.storedPassword = true;
        expectedData.gasPrice = global.defaultWalletSettings.minGasPrice;
        expectedData.useNavigation = true;
        expectedData.balance = contractDetails.balance;
        expectedData.etherBalance = contractDetails.etherBalance;
        expectedData.contractDetails = {
          address: global.tokenAddress,
          isContract: true,
          isDefault: true,
          defaultContract: true,
          isApproved: true,
          isAdmin: true,
          isPaused: false,
          contractType: 2,
          contractTypeLabel: 'ERT Token',
          networkId: global.testNetworkId,
          name: global.tokenName,
          title: global.tokenName,
          symbol: global.tokenSymbol,
          decimals: global.tokenDecimals,
          totalSupply: global.tokenSupply,
          sellPrice: global.tokenSellPrice,
          owner: global.walletAddresses[0],
          isOwner: true,
          adminLevel: 5,
        };

        expectObjectValueEqual(delegateTokensModal, expectedData, 'DelegateTokensModal test wallet browser enabled', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('DelegateTokensModal - test form data', (done) => {
    console.log('--- DelegateTokensModal - test form data');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, delegateTokensModal;
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
        delegateTokensModal = accountDetailCmp.$refs.delegateTokensModal;
        expect(delegateTokensModal).toBeTruthy();
        delegateTokensModal.open = true;

        return flushPromises();
      })
      .then(() => {
        return approveAccount(contractDetails.contract, global.walletAddress, global.walletAddresses[9]);
      })
      .then(() => {
        return approveAccount(contractDetails.contract, global.walletAddress, global.walletAddresses[2]);
      })
      .then(() => {
        delegateTokensModal.amount = '999999999999999999999999';

        return flushPromises();
      })
      .then(() => {
        expect(delegateTokensModal.error).toBeTruthy();
        expect(delegateTokensModal.buttonDisabled).toBeTruthy();
        delegateTokensModal.amount = '0';

        return flushPromises();
      })
      .then(() => {
        expect(delegateTokensModal.buttonDisabled).toBeTruthy();
        delegateTokensModal.amount = '1';

        return flushPromises();
      })
      .then(() => {
        expect(delegateTokensModal.buttonDisabled).toBeTruthy();
        delegateTokensModal.recipient = 'invalid address';

        return flushPromises();
      })
      .then(() => {
        expect(delegateTokensModal.buttonDisabled).toBeFalsy();

        delegateTokensModal.sendTokens();
        expect(delegateTokensModal.error).toBeTruthy();

        delegateTokensModal.recipient = global.walletAddresses[2];
        delegateTokensModal.amount = 'abc';
        delegateTokensModal.sendTokens();
        expect(delegateTokensModal.error).toBeTruthy();
        delegateTokensModal.amount = 1;

        delegateTokensModal.storedPassword = false;
        delegateTokensModal.sendTokens();
        expect(delegateTokensModal.error).toBeTruthy();
        delegateTokensModal.walletPassword = 'fake password';
        delegateTokensModal.sendTokens();
        expect(delegateTokensModal.error).toBeTruthy();
        delegateTokensModal.storedPassword = true;

        const initialBalance = contractDetails.balance;
        contractDetails.balance = 0;
        delegateTokensModal.sendTokens();

        expect(delegateTokensModal.error).toBeTruthy();
        contractDetails.balance = initialBalance;
        return contractDetails.contract.methods.balanceOf(global.walletAddress).call();
      })
      .then((balance) => {
        expect(balance > 1 * Math.pow(10, global.tokenDecimals)).toBeTruthy();
      })
      .then(() => {
        if (delegateTokensModal.error) {
          done.fail(delegateTokensModal.error);
        }
        return delegateTokensModal.sendTokens();
      })
      .then(() => {
        if (delegateTokensModal.error) {
          done.fail(delegateTokensModal.error);
        }
        return contractDetails.contract.methods.allowance(global.walletAddress, delegateTokensModal.recipient).call();
      })
      .then((allowance) => {
        expect(Number(allowance)).toEqual(Math.pow(10, global.tokenDecimals));
        return flushPromises();
      })
      .then(() => {
        const senderTransactions = getTransactions(global.walletAddress);
        const receiverTransactions = getTransactions(delegateTokensModal.recipient);
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
