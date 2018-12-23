import GasPriceChoice from '../../main/webapp/vue-app/components/GasPriceChoice';

import {getWalletApp, initApp} from '../TestUtils.js';

import {shallowMount} from '@vue/test-utils';

describe('GasPriceChoice.test.js', () => {
  let app;
  const walletApp = getWalletApp();

  beforeAll(() => {
    app = shallowMount(GasPriceChoice, {
      propsData: {
        estimatedFee: '5000',
        title: 'hello',
        disabled: false,
      },
      attachToDocument: true,
    });

    return initApp(walletApp);
  });

  it('test html', () => {
    expect(app.html()).toContain('5000');
    expect(app.html()).toContain('hello');
    expect(app.vm.disabled).toBe(false);
    expect(app.findAll('.mt-3')).toHaveLength(1);
    expect(app.findAll('.mt-2')).toHaveLength(0);
  });

  it('check choice 2', () => {
    const choice = '2';
    expect(app.vm.choice).toBe('1');
    app.setData({choice: choice});
    expect(app.vm.choice).toBe('2');
    expect(app.emitted().changed).toBeTruthy();
    expect(app.emitted().changed.length).toBe(1);
    expect(app.emitted().changed[0]).toEqual([8000000000]);
  });

  it('check choice 3', () => {
    const choice = '3';
    expect(app.vm.choice).toBe('2');
    app.setData({choice: choice});
    expect(app.vm.choice).toBe('3');
    expect(app.emitted().changed).toBeTruthy();
    expect(app.emitted().changed.length).toBe(2);
    expect(app.emitted().changed[0]).toEqual([8000000000]);
    expect(app.emitted().changed[1]).toEqual([15000000000]);
  });
});
