import {getWalletApp, initApp} from '../TestUtils.js';

import SpacewalletApp from '../../main/webapp/vue-app/components/SpaceWalletApp.vue';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('SpaceWalletApp.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  it('Space Wallet App ', () => {
    console.log('-- Space Wallet App');

    const spacewalletApp = mount(SpacewalletApp, {
      attachToDocument: true,
    });

    expect(spacewalletApp.html()).toContain('WalletApp');
  });
});
