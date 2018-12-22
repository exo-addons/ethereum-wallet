import Vue from 'vue';

import {getWalletApp, initApp, expectObjectValueEqual, expectCountElement} from '../TestUtils.js';

describe('WalletAppMenu.test.js', () => {
  const app = getWalletApp();
  let walletAppMenu;

  beforeAll(() => {
    return initApp(app).then(() => (walletAppMenu = app.vm.$refs.walletAppMenu));
  });

  const defaultAttributesValues = {
    isSpace: false,
    isSpaceAdministrator: false,
    isMaximized: true,
  };

  it('WalletAppMenu data', () => {
    console.log('-- Test WalletAppMenu data');

    expectObjectValueEqual(walletAppMenu, defaultAttributesValues);
  });

  it('WalletAppMenu visible components', () => {
    console.log('-- Test visible components');

    const walletAppMenuHTML = walletAppMenu.$el.innerHTML;

    expect(walletAppMenuHTML).toContain('Refresh wallet');
    expect(walletAppMenuHTML).toContain('title="Settings"');

    expect(walletAppMenuHTML).not.toContain('Open wallet application');

    expectCountElement(app, 'walletAppMenuRefreshButton', 1);
    expectCountElement(app, 'walletAppMenuSettingsButton', 1);
    expectCountElement(app, 'walletAppMenuMaximizeButton', 0);
  });

  it('WalletAppMenu visible components when isSpace and no space administrator', () => {
    console.log('-- Test visible components when isSpace and no space administrator');

    // Space wallet address
    global.walletAddress = global.walletAddresses[2];

    const app = getWalletApp(true);
    let walletAppMenu;
    return initApp(app).then(() => {
      walletAppMenu = app.vm.$refs.walletAppMenu;

      expectCountElement(app, 'walletAppMenuRefreshButton', 1);
      expectCountElement(app, 'walletAppMenuSettingsButton', 0);
      expectCountElement(app, 'walletAppMenuMaximizeButton', 0);

      const expectedData = Object.assign({}, defaultAttributesValues);
      expectedData.isSpace = true;
      expectObjectValueEqual(walletAppMenu, expectedData);
    });
  });

  it('WalletAppMenu visible components when isSpace and is space administrator', () => {
    console.log('-- Test visible components when isSpace and is space administrator');

    // Space wallet address
    global.walletAddress = global.walletAddresses[2];
    global.defaultWalletSettings.isSpaceAdministrator = true;

    const app = getWalletApp(true);
    let walletAppMenu;
    return initApp(app).then(() => {
      walletAppMenu = app.vm.$refs.walletAppMenu;

      expectCountElement(app, 'walletAppMenuRefreshButton', 1);
      expectCountElement(app, 'walletAppMenuSettingsButton', 1);
      expectCountElement(app, 'walletAppMenuMaximizeButton', 0);

      const expectedData = Object.assign({}, defaultAttributesValues);
      expectedData.isSpace = true;
      expectedData.isSpaceAdministrator = true;
      expectObjectValueEqual(walletAppMenu, expectedData);
    });
  });

  it('WalletAppMenu visible components when not maximized', () => {
    console.log('-- Test visible components when not maximized');

    Vue.prototype.isMaximized = false;

    // Space wallet address
    global.walletAddress = global.walletAddresses[2];
    global.defaultWalletSettings.isSpaceAdministrator = true;

    const app = getWalletApp(true);
    let walletAppMenu;
    return initApp(app).then(() => {
      walletAppMenu = app.vm.$refs.walletAppMenu;

      expectCountElement(app, 'walletAppMenuRefreshButton', 1);
      expectCountElement(app, 'walletAppMenuSettingsButton', 0);
      expectCountElement(app, 'walletAppMenuMaximizeButton', 1);

      const expectedData = Object.assign({}, defaultAttributesValues);
      expectedData.isSpace = true;
      expectedData.isSpaceAdministrator = true;
      expectedData.isMaximized = false;
      expectObjectValueEqual(walletAppMenu, expectedData);
    });
  });
});
