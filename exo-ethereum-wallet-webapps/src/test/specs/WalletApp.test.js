import {getWalletApp, initApp, expectCountElement, expectHasClass, expectObjectValueEqual, getEtherAccountDetails, initiateBrowserWallet} from '../TestUtils.js';

describe('WalletApp.test.js', () => {
  let app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  beforeEach(() => {
    if(global.reinitApp) {
      app = getWalletApp();
      return initApp(app);
    }
  });

  afterEach(() => {
    global.walletAddress = global.defaultWalletAddress;
  });

  const defaultAttributesValues = {
    isWalletEnabled: true,
    isReadOnly: true,
    displayAccountsList: true,
    networkId: global.testNetworkId,
    browserWalletExists: false,
    walletAddress: global.walletAddress,
    selectedTransactionHash: false,
    selectedAccount: false,
    fiatSymbol: '$',
    loading: false,
    useMetamask: false,
    isSpaceAdministrator: false,
    seeAccountDetails: false,
    seeAccountDetailsPermanent: false,
    principalAccount: 'ether',
    error: false,
    showSettingsModal: false,
    overviewAccounts: ['ether'],
    overviewAccountsToDisplay: ['ether'],
    gasPriceInEther: '0.000000008',
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

  it('WalletApp - test change account associated adress', async () => {
    // This test will change application status, so it has to be reinitialized
    global.reinitApp = true;

    // Fake address to retrive right Data from mocked fetch method
    global.walletAddress = '0x0000a021b66a1f421970b072600011d626b798ef';

    const expectedData = Object.assign({}, defaultAttributesValues);
    expectedData.accountsDetails = {}
    expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, '0', '0');
    expectedData.etherBalance = 0;
    expectedData.totalBalance = 0;
    expectedData.totalFiatBalance = 0;
    expectedData.walletAddress = global.walletAddress;

    await initApp(app);
    expectObjectValueEqual(app.vm, expectedData);
  });

  it('WalletApp - test account having a browser wallet with generated password', async () => {
    // This test will change application status, so it has to be reinitialized
    global.reinitApp = true;

    global.walletAddress = global.walletAddresses[0];
    // Not space, generated and not backed up wallet
    await initiateBrowserWallet(0, 'testpassword', false, true, false);
    app = getWalletApp();
    await initApp(app);

    const expectedData = Object.assign({}, defaultAttributesValues);
    expectedData.accountsDetails = {}
    expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, String(expectedData.etherBalance), String(expectedData.totalFiatBalance));
    expectedData.walletAddress = global.walletAddress;
    expectedData.isReadOnly = false;
    expectedData.browserWalletExists = true;
    expectedData.displayWalletResetOption = true;

    expectObjectValueEqual(app.vm, expectedData);
  });
});
