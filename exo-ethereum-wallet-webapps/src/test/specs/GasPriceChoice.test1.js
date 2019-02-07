import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import GasPriceChoice from '../../main/webapp/vue-app/components/GasPriceChoice.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('GasPriceChoice.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    estimatedFee: false,
    title: false,
    disabled: false,
    choice: '1',
  };

  it('GasPriceChoice default data', () => {
    console.log('-- GasPriceChoice default data');

    const gasPriceChoice = mount(GasPriceChoice, {
      attachToDocument: true,
    });

    expectObjectValueEqual(gasPriceChoice.vm, defaultAttributesValues, 'GasPriceChoice default data');
  });

  it('GasPriceChoice test when check choice 2', () => {
    console.log('-- GasPriceChoice test when check choice 2');

    const gasPriceChoice = mount(GasPriceChoice, {
      attachToDocument: true,
    });

    gasPriceChoice.setData({choice: '2'});
    expect(gasPriceChoice.emitted()['changed']).toBeTruthy();
    expect(gasPriceChoice.emitted()['changed']).toEqual([[8000000000]]);
  });

  it('GasPriceChoice test when check choice 3', () => {
    console.log('-- GasPriceChoice test when check choice 3');

    const gasPriceChoice = mount(GasPriceChoice, {
      attachToDocument: true,
    });

    gasPriceChoice.setData({choice: '3'});
    expect(gasPriceChoice.emitted()['changed']).toBeTruthy();
    expect(gasPriceChoice.emitted()['changed']).toEqual([[15000000000]]);
  });
});
