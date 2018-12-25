import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import SendEtherModal from '../../main/webapp/vue-app/components/SendEtherModal';

import {mount} from '@vue/test-utils';

describe('SendEtherModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    account: false,
    isReadonly: false,
    noButton: false,
    useNavigation: false,
    open: false,
    balance: 0,
    recipient: 0,
    dialog: false,
    disabled: true,
  };

  it('SendEtherModal default data', () => {
    console.log('-- Test SendEtherModal default data');

    const sendFundsModal = mount(SendEtherModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(sendFundsModal.vm, defaultAttributesValues);
  });
});
