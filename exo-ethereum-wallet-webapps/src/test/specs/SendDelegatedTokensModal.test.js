import {getWalletApp, initApp, getTransactions, expectObjectValueEqual, initiateBrowserWallet, sendTokens, approveTokens, approveAccount} from '../TestUtils.js';

import SendDelegatedTokensModal from '../../main/webapp/vue-app/components/SendDelegatedTokensModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendDelegatedTokensModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    walletAddress: false,
    contractDetails: {},
    noButton: false,
    isReadonly: false,
    useNavigation: false,
    open: false,
    hasDelegatedTokens: true,
    showQRCodeModal: false,
    storedPassword: false,
    walletPassword: '',
    walletPasswordShow: false,
    useMetamask: false,
    loading: false,
    from: false,
    recipient: false,
    amount: false,
    gasPrice: 0,
    dialog: false,
    warning: false,
    error: false,
    etherBalance: false,
    disabled: true,
  };

  it('SendDelegatedTokensModal default data', () => {
    console.log('-- SendDelegateTokensModal default data');

    const senddelegatedTokensModal = mount(SendDelegatedTokensModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(senddelegatedTokensModal.vm, defaultAttributesValues, 'SendDelegatedTokensModal default data');
  });

  it('SendDelegatedTokensModal - test send delegation disabled', (done) => {
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
        expect(app.findAll('#accountDetail .sendDelegatedTokenModal')).toHaveLength(0);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('sendDelegateTokensModal - test send delegation enabled', (done) => {
    console.log('--- sendDelegatedTokensModal - test send delegation enabled');

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
        expect(app.findAll('#accountDetail .sendDelegatedTokenModal')).toHaveLength(1);

        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.isReadonly = true;
        expectedData.useNavigation = true;
        expectedData.etherBalance = contractDetails.etherBalance;
        expectedData.walletAddress = global.walletAddress;

        expectedData.contractDetails = {
          address: global.tokenAddress,
          isContract: true,
          isDefault: true,
          defaultContract: true,
          isApproved: true,
          isAdmin: true,
          isPaused: false,
          contractType: 1,
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

        const sendDelegatedTokensModal = accountDetailCmp.$refs.sendDelegatedTokensModal;
        expect(sendDelegatedTokensModal).toBeTruthy();
        expectObjectValueEqual(sendDelegatedTokensModal, expectedData, 'sendDelegatedTokensModal test send delegation enabled default data', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('sendDelegatedTokensModal - test wallet browser enabled', (done) => {
    console.log('--- sendDelegatedTokensModal - test wallet browser enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendDelegatedTokensModal;
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
        sendDelegatedTokensModal = accountDetailCmp.$refs.sendDelegatedTokensModal;
        expect(sendDelegatedTokensModal).toBeTruthy();
        sendDelegatedTokensModal.open = true;
        return flushPromises();
      })
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.isReadonly = false;
        expectedData.open = true;
        expectedData.walletAddress = global.walletAddress;
        expectedData.dialog = true;
        expectedData.disabled = false;
        expectedData.storedPassword = true;
        expectedData.gasPrice = global.defaultWalletSettings.minGasPrice;
        expectedData.useNavigation = true;
        expectedData.etherBalance = contractDetails.etherBalance;
        expectedData.contractDetails = {
          address: global.tokenAddress,
          isContract: true,
          isDefault: true,
          defaultContract: true,
          isApproved: true,
          isAdmin: true,
          isPaused: false,
          contractType: 1,
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

        expectObjectValueEqual(sendDelegatedTokensModal, expectedData, ' sendDelegatedTokensModal test wallet browser enabled', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('SendDelegatedTokensModal - test send delegated token', (done) => {
    console.log('--- SendDelegateTokensModal -  test send delegated token');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];
    global.defaultWalletSettings.enableDelegation = true;

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, senddelegatedTokensModal, initialBalance;
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
          senddelegatedTokensModal = accountDetailCmp.$refs.sendDelegatedTokensModal;
          expect(senddelegatedTokensModal).toBeTruthy();
          senddelegatedTokensModal.open = true;
          return flushPromises();
        })

        .then(() => {
          sendTokens(contractDetails.contract, global.walletAddress, global.walletAddresses[7], 20);
        })

        .then(() => contractDetails.contract.methods.balanceOf(global.walletAddresses[7]).call())
        .then((balance) => {
          initialBalance = Number(balance);
        })

        .then(() => {
          approveTokens(contractDetails.contract, global.walletAddresses[7], global.walletAddresses[0], 5);
        })
        .then(() => {
          approveAccount(contractDetails.contract, global.walletAddress, global.walletAddresses[9]);
        })
        .then(() => contractDetails.contract.methods.allowance(global.walletAddresses[7], global.walletAddresses[0]).call())
        .then((allowance) => {
          expect(Number(allowance)).toEqual(5 * Math.pow(10, global.tokenDecimals));
          return flushPromises();
        })

        .then(() => contractDetails.contract.methods.balanceOf(global.walletAddresses[9]).call())
        .then((balance) => {
          expect(Number(balance)).toEqual(0);
        })

        .then(() => contractDetails.contract.methods.isApprovedAccount(global.walletAddresses[2]).call())
        .then((approved) => {
          expect(approved).toBeFalsy();
        })

        .then(() => {
          expect(senddelegatedTokensModal.buttonDisabled).toBeFalsy();

          senddelegatedTokensModal.sendTokens();
          expect(senddelegatedTokensModal.error).toBeTruthy();

          senddelegatedTokensModal.from = global.walletAddresses[7];

          //when recipient not approved
          senddelegatedTokensModal.recipient = 'invalid address';
          senddelegatedTokensModal.sendTokens();
          expect(senddelegatedTokensModal.error).toBeTruthy();
          senddelegatedTokensModal.recipient = global.walletAddresses[2];
          senddelegatedTokensModal.amount = 'abc';
          senddelegatedTokensModal.sendTokens();
          expect(senddelegatedTokensModal.error).toBeTruthy();
          senddelegatedTokensModal.amount = 3;
          senddelegatedTokensModal.storedPassword = false;
          senddelegatedTokensModal.sendTokens();
          expect(senddelegatedTokensModal.error).toBeTruthy();
          senddelegatedTokensModal.walletPassword = 'fake password';
          senddelegatedTokensModal.sendTokens();
          expect(senddelegatedTokensModal.error).toBeTruthy();
          senddelegatedTokensModal.storedPassword = true;
          senddelegatedTokensModal.sendTokens();
        })

        //when allowance < amount
        .then(() => {
          senddelegatedTokensModal.from = global.walletAddresses[7];
          senddelegatedTokensModal.recipient = global.walletAddresses[9];
          senddelegatedTokensModal.amount = 10;
          senddelegatedTokensModal.storedPassword = true;
          senddelegatedTokensModal.sendTokens();
        })

        //when allowance < sum of amount to sent
        .then(() => {
          senddelegatedTokensModal.from = global.walletAddresses[7];
          senddelegatedTokensModal.recipient = global.walletAddresses[9];
          senddelegatedTokensModal.amount = 3;
          senddelegatedTokensModal.storedPassword = true;
          senddelegatedTokensModal.sendTokens();
        })

        .then(() => {
          senddelegatedTokensModal.from = global.walletAddresses[7];
          senddelegatedTokensModal.recipient = global.walletAddresses[9];
          senddelegatedTokensModal.amount = 4;
          senddelegatedTokensModal.storedPassword = true;
          return senddelegatedTokensModal.sendTokens();
        })

        .then(() => contractDetails.contract.methods.allowance(global.walletAddresses[7], global.walletAddresses[0]).call())
        .then(() => contractDetails.contract.methods.balanceOf(global.walletAddresses[9]).call())
        .then((balance) => {
          expect(Number(balance)).toEqual(4 * Math.pow(10, global.tokenDecimals));
          return flushPromises();
        })

        .then(() => contractDetails.contract.methods.balanceOf(global.walletAddresses[7]).call())
        .then((balance) => {
          expect(Number(balance)).toEqual(initialBalance - 4 * Math.pow(10, global.tokenDecimals));
          return flushPromises();
        })

        .then(() => {
          const senderTransactions = getTransactions(senddelegatedTokensModal.from);
          const receiverTransactions = getTransactions(senddelegatedTokensModal.recipient);
          expect(senderTransactions).toBeTruthy();
          expect(receiverTransactions).toBeTruthy();
          expect(senderTransactions.length > 0).toBeTruthy();
          expect(receiverTransactions.length > 0).toBeTruthy();
          done();
        })
        .catch((e) => {
          done(e);
        })
    );
  });
});
