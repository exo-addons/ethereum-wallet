import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendFundsModal from '../../main/webapp/vue-app/components/SendFundsModal';

import {mount} from '@vue/test-utils';

describe('SendFundsModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    accountsDetails: {},
    overviewAccounts: [],
    principalAccount: false,
    refreshIndex: 1,
    networkId: 0,
    walletAddress: false,
    icon: false,
    displayAllAccounts: false,
    disabled: false,
    addPendingToReceiver: false,
    noButton: false,
    selectedOption: false,
    error: false,
    dialog: false,
  };

  it('SendFundsModal default data', () => {
    console.log('-- Test SendFundsModal default data');

    const sendFundsModal = mount(SendFundsModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendFundsModal.vm, defaultAttributesValues);
    expectCountElement(sendFundsModal, 'sendFundsModal', 1);
  });
});
