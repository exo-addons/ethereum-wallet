import {getWalletApp, initApp, expectObjectValueEqual} from '../TestUtils.js';

describe('WalletAppMenu.test.js', () => {
  const app = getWalletApp();
  let walletAppMenu;

  beforeAll(() => {
    return initApp(app).then(() => (walletAppMenu = app.vm.$refs.walletAppMenu));
  });

  const defaultAttributesValues = {
    isSpace: false,
    isSpaceAdministrator: false,
    walletAddress: global.defaultWalletAddress,
    isMaximized: true,
  };

  it('WalletAppMenu data', () => {
    expectObjectValueEqual(walletAppMenu, defaultAttributesValues);
  });

  it('WalletAppMenu visible components', () => {
    const walletAppMenuHTML = walletAppMenu.$el.innerHTML;

    expect(walletAppMenuHTML).toContain('Refresh wallet');
    expect(walletAppMenuHTML).toContain('title="Settings"');

    expect(walletAppMenuHTML).not.toContain('Open wallet application');
  });
});
