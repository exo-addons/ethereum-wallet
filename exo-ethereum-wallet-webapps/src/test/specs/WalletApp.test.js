import {getWalletApp, initApp, expectCountElement, expectHasClass, expectObjectValueEqual, getEtherAccountDetails} from '../TestUtils.js';

describe('WalletApp.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  afterEach(() => {
    global.walletAddress = global.defaultWalletAddress;
  });

  const defaultAttributesValues = {
    networkId: global.testNetworkId,
    browserWalletExists: false,
    walletAddress: global.walletAddress,
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
    gasPriceInEther: '0.000000008',
    displayAccountsList: true,
    displayWalletResetOption: false,
    displayEtherBalanceTooLow: false,
    etherBalance: 100,
    totalBalance: 100,
    totalFiatBalance: 9361.729,
    accountsDetails: {},
  };

  defaultAttributesValues.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, '100', '9361.729');

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

  it('WalletApp - test change account associated adress', () => {
    // Fake address to retrive right Data from mocked fetch method
    global.walletAddress = '0x0000a021b66a1f421970b072600011d626b798ef';

    const expectedData = Object.assign({}, defaultAttributesValues);
    expectedData.accountsDetails = {}
    expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, '0', '0');
    expectedData.etherBalance = 0;
    expectedData.totalBalance = 0;
    expectedData.totalFiatBalance = 0;
    expectedData.walletAddress = global.walletAddress;

    return initApp(app).then(() => {
      expectObjectValueEqual(app.vm, expectedData);
    });
  });
});
