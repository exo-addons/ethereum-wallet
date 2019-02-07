import {getWalletApp, expectCountElement, initApp} from '../TestUtils.js';

import SpaceWalletApp from '../../main/webapp/vue-app/components/SpaceWalletApp.vue';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('SpaceWalletApp.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  it('Space Wallet App ', () => {
    console.log('-- Space Wallet App');

    const spaceWalletApp = mount(SpaceWalletApp, {
      attachToDocument: true,
    });

    expect(spaceWalletApp.html()).toContain('WalletApp');
  });
});
