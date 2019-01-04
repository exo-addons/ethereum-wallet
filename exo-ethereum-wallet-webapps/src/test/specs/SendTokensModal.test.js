import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendTokensModal from '../../main/webapp/vue-app/components/SendTokensModal.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('SendTokensModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    account: false,
    contractDetails: {},
    noButton: false,
    useNavigation: false,
    isReadonly: false,
    open: false,
    contract: {},
    dialog: false,
    balance: false,
    etherBalance: false,
    disabled: true,
  };

  it('SendTokensModal default data', () => {
    console.log('-- SendTokensModal default data');

    const sendTokensModal = mount(SendTokensModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendTokensModal.vm, defaultAttributesValues, 'sendTokensModal default data');
  });

  it('SendTokensModal - test wallet browser enabled', (done) => {
    console.log('--- sendTokensModal - test wallet browser enabled');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, sendTokensModal;
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
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.account = global.walletAddress;
        expectedData.isReadonly = false;
        expectedData.open = true;
        expectedData.dialog = true;
        expectedData.disabled = false;
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
        expectObjectValueEqual(sendTokensModal, expectedData, 'SendTokensModal test wallet browser enabled', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
