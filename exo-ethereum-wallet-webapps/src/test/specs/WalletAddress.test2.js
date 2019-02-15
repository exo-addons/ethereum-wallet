import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual} from '../TestUtils.js';

import WalletAddress from '../../main/webapp/vue-app/components/WalletAddress.vue';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('WalletAddress.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    name: false,
    value: false,
    allowCopy: true,
    allowEdit: true,
    isEditing: false,
    loading: false,
    isAdmin: false,
    addressEtherscanLink: false,
    labelDetail: {},
    labelDetailToEdit: {},
  };

  it('WalletAddress default data', () => {
    console.log('-- WalletAddress default data');

    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletAddress.vm, defaultAttributesValues, 'WalletAddress default data');
  });

  it('WalletAddress test copy', () => {
    console.log('-- WalletAddress test copy');

    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    walletAddress.vm.value = '0x627306090abab3a6e1400e9345bc60c78a8bef57';

    expect(walletAddress.vm.allowCopy).toBeTruthy();

    walletAddress.find('#copy').trigger('click');
  });

  it('WalletAddress test save when allow Edit', () => {
    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    walletAddress.vm.allowEdit = false;
    walletAddress.vm.name = 'test';
    walletAddress.vm.value = '0x627306090abab3a6e1400e9345bc60c78a8bef57';
    walletAddress.vm.save();
    expect(walletAddress.vm.loading).toBeFalsy();
    walletAddress.vm.allowEdit = true;
    walletAddress.vm.save();
    expect(walletAddress.vm.loading).toBeTruthy();
  });

  it('WalletAddress test reset option', () => {
    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    walletAddress.vm.name = 'test';
    walletAddress.vm.value = '0x627306090abab3a6e1400e9345bc60c78a8bef57';
    walletAddress.vm.reset();
    expect(walletAddress.vm.labelDetail).toEqual({address: '0x627306090abab3a6e1400e9345bc60c78a8bef57'});
  });

  it('WalletAddress test edit or save', () => {
    const walletAddress = mount(WalletAddress, {
      attachToDocument: true,
    });

    walletAddress.vm.isAdmin = true;
    walletAddress.vm.name = 'nermine';
    walletAddress.vm.value = '0x627306090abab3a6e1400e9345bc60c78a8bef57';
    walletAddress.vm.allowEdit = false;
    expectCountElement(walletAddress, 'walletAddressEdit', 0);
    walletAddress.vm.allowEdit = true;
    expectCountElement(walletAddress, 'walletAddressEdit', 1);
    walletAddress.find('.walletAddressEdit').trigger('click');
    expect(walletAddress.vm.isEditing).toBeTruthy();

    //  console.warn('name',walletAddress.vm.name);
    //  console.warn('value',walletAddress.vm.value);
    //  console.warn('allowCopy',walletAddress.vm.allowCopy);
    //  console.warn('allowEdit',walletAddress.vm.allowEdit);
    //  console.warn('isediting',walletAddress.vm.isEditing);
    //  console.warn('loading',walletAddress.vm.loading);
    //  console.warn('isAdmin',walletAddress.vm.isAdmin);
    //  console.warn('addressEtherscanLink',walletAddress.vm.addressEtherscanLink);
    //  console.warn('labelDetail',walletAddress.vm.labelDetail);
    //  console.warn('labelDetailToEdit',walletAddress.vm.labelDetailToEdit);
  });
});
