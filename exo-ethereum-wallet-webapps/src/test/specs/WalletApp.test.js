import {getWalletApp, initApp, expectCountElement, expectHasClass, expectObjectValueEqual} from '../TestUtils.js';

describe('WalletApp.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
      networkId: 4452365,
      browserWalletExists: false,
      walletAddress: '0xb460a021b66a1f421970b07262ed11d626b798ef',
      selectedTransactionHash: false,
      selectedAccount: false,
      fiatSymbol: '$',
      isWalletEnabled: true,
      loading: false,
      useMetamask: false,
      isReadOnly: true,
      isSpaceAdministrator: false,
      seeAccountDetails: false,
      seeAccountDetailsPermanent: false,
      principalAccount: 'ether',
      error: false,
      showSettingsModal: false,
      overviewAccounts: ['ether'],
      overviewAccountsToDisplay: ['ether'],
      gasPriceInEther: "0.000000008",
      displayAccountsList: true,
      displayWalletResetOption: false,
      displayEtherBalanceTooLow: false,
      etherBalance: 100,
      totalBalance: 100,
      totalFiatBalance: 9361.729,
  };

  it('WalletApp default data', () => {
    expectObjectValueEqual(app.vm, defaultAttributesValues);
  });

  it('WalletApp visible components', () => {
    expectCountElement(app, 'walletSetup', 1);
    expectCountElement(app, 'waletSummary', 1);
    expectCountElement(app, 'walletAccountsList', 1);
    expectCountElement(app, 'walletAppMenu', 1);
    expectCountElement(app, 'accountDetail', 0);
  });

  it('WalletApp maximized class', () => {
    expectHasClass(app, 'WalletApp', 'maximized');
  });

});
