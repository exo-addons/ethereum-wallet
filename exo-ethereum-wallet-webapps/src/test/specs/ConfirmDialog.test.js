import {getWalletApp, initApp, getTransactions, expectCountElement, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import ConfirmDialog from '../../main/webapp/vue-app/components/ConfirmDialog';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

describe('ConfirmDialog.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    loading: false,
    title: false,
    message: false,
    okLabel: 'ok',
    cancelLabel: 'Cancel',
    dialog: false,
  };

  it('ConfirmDialog default data', () => {
    console.log('-- Test ConfirmDialog default data');

    const confirmDialog = mount(ConfirmDialog, {
      attachToDocument: true,
    });

    expectObjectValueEqual(confirmDialog.vm, defaultAttributesValues, 'confirmDialog default data: ');
  });

  it('ConfirmDialog ok event', () => {
    console.log('-- Test ok event');

    const confirmDialog = mount(ConfirmDialog, {
      attachToDocument: true,
    });

    confirmDialog.vm.ok();
    expect(confirmDialog.emitted()['ok']).toBeTruthy();
  });

  it('ConfirmDialog close event', () => {
    console.log('-- Test close event');

    const confirmDialog = mount(ConfirmDialog, {
      attachToDocument: true,
    });

    confirmDialog.vm.close();
    expect(confirmDialog.emitted()['closed']).toBeTruthy();
  });

  it('ConfirmDialog test open dialog', () => {
    console.log('-- Test open dialog');

    const confirmDialog = mount(ConfirmDialog, {
      attachToDocument: true,
    });

    confirmDialog.vm.open();
    expect(confirmDialog.vm.dialog).toBeTruthy();
  });
});
