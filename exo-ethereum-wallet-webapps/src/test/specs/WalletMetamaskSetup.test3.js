import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import WalletMetamaskSetup from '../../main/webapp/vue-app/components/WalletMetamaskSetup.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('WalletMetamaskSetup.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    isSpace: false,
    isSpaceAdministrator: false,
    loading: false,
    isAdministration: false,
    walletAddress: false,
    addressAssociationDialog: false,
    installInstructionDialog: false,
    networkLabel: false,
    principalContractAdminAddress: global.walletAddresses[0],
    sameConfiguredNetwork: true,
    associatedWalletAddress: global.walletAddress,
    detectedMetamaskAccount: false,
    currentAccountAlreadyInUse: false,
    metamaskEnabled: false,
    metamaskConnected: false,
    displayNotSameNetworkWarning: false,
    isPrincipalContractAdmin: false,
    displaySpaceAccountAssociationHelp: false,
    displayUserAccountAssociationHelp: false,
    displayAddressAssociationBox: false,
    displayUserAccountChangeHelp: false,
    displayAccountHelpActions: false,
    displaySpaceMetamaskEnableHelp: false,
    newAddressDetected: false,
  };

  it('WalletMetamaskSetup default data', () => {
    console.log('-- WalletMetamaskSetup default data');

    const walletMetamaskSetup = mount(WalletMetamaskSetup, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletMetamaskSetup.vm, defaultAttributesValues, 'walletMetamaskSetup default data');
  });

  it('WalletMetamaskSetup test when disableMetasmaskUsage event', () => {
    console.log('-- WalletMetamaskSetup test when disableMetasmaskUsage event');

    const walletMetamaskSetup = mount(WalletMetamaskSetup, {
      attachToDocument: true,
    });

    walletMetamaskSetup.vm.disableMetamaskUsage();
    expect(walletMetamaskSetup.emitted()['refresh']).toBeTruthy();
  });
});
