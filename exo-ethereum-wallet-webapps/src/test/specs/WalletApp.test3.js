import {getWalletApp, initApp, expectCountElement, expectHasClass, expectObjectValueEqual, getEtherAccountDetails, getTokenAccountDetails, initiateBrowserWallet, sendTokens, approveAccount} from '../TestUtils.js';

jest.setTimeout(30000);

describe('WalletApp.test.js', () => {
  let app;

  beforeAll(() => {
    global.walletAddress = global.defaultWalletAddress;
    app = getWalletApp();
    return initApp(app);
  });

  beforeEach(() => {
    global.defaultWalletSettings.isWalletEnabled = true;
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
    console.log('--- Test default data');
    expectObjectValueEqual(app.vm, defaultAttributesValues, 'WalletApp default data');
  });

  it('WalletApp visible components', () => {
    console.log('--- Test visible components');
    expectCountElement(app, 'walletEnabledContent', 1);
    expectCountElement(app, 'walletDisabledContent', 0);
    expectCountElement(app, 'walletSetup', 1);
    expectCountElement(app, 'waletSummary', 1);
    expectCountElement(app, 'walletAccountsList', 1);
    expectCountElement(app, 'walletAppMenu', 1);
    expectCountElement(app, 'accountDetail', 0);
  });

  it('WalletApp maximized class', () => {
    expectHasClass(app, 'WalletApp', 'maximized');
  });

  it('WalletApp - test account having a browser wallet with generated password', () => {
    console.log('--- Test account having a browser wallet with generated password');

    global.walletAddress = global.walletAddresses[1];
    const app = getWalletApp();

    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);
        expectedData.accountsDetails = {};
        expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, String(expectedData.etherBalance), String(expectedData.totalFiatBalance));
        expectedData.walletAddress = global.walletAddress;
        expectedData.isReadOnly = false;
        expectedData.browserWalletExists = true;
        expectedData.displayWalletResetOption = true;

        expectObjectValueEqual(app.vm, expectedData, 'WalletApp - test account having a browser wallet with generated password: ');
      });
  });

  it('WalletApp - test change newly created account associated address', () => {
    console.log('--- Test change newly created account associated address');

    // Last address having 0 ether in its account
    global.walletAddress = global.walletAddresses[global.walletAddresses.length - 1];

    const app = getWalletApp();

    const expectedData = Object.assign({}, defaultAttributesValues);
    expectedData.accountsDetails = {};
    expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, '0', '0');
    expectedData.etherBalance = 0;
    expectedData.totalBalance = 0;
    expectedData.totalFiatBalance = 0;
    expectedData.walletAddress = global.walletAddress;

    return initApp(app)
      .then(() => {
        expectObjectValueEqual(app.vm, expectedData, 'WalletApp - test change newly created account associated address');

        // Disable ReadOnly mode to be able to display displayEtherBalanceTooLow warning
        console.log('--- Test displayEtherBalanceTooLow');
        return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false);
      })
      .then(() => initApp(app))
      .then(() => {
        expectedData.isReadOnly = false;
        expectedData.browserWalletExists = true;
        expectedData.displayWalletResetOption = true;
        expectedData.displayEtherBalanceTooLow = true;

        expectObjectValueEqual(app.vm, expectedData, 'WalletApp - test change newly created account associated address: ');
      });
  });

  it('WalletApp - test open token account details', (done) => {
    console.log('--- test open token account details');

    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app)
      .then(() => {
        const accountDetails = app.vm.accountsDetails[global.tokenAddress];
        app.vm.openAccountDetail(accountDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        expectCountElement(app, 'accountDetail', 1);
        app.vm.back();
        expectCountElement(app, 'accountDetail', 0);
        done();
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('WalletApp - test when disabled wallet', () => {
    console.log('--- Test when disabled wallet');

    global.defaultWalletSettings.isWalletEnabled = false;

    return initApp(app).then(() => {
      expectObjectValueEqual(app.vm, {isWalletEnabled: false}, 'WalletApp - test open token account details', null, true);
      expectCountElement(app, 'walletDisabledContent', 1);
      expectCountElement(app, 'walletEnabledContent', 0);
    });
  });

  it('WalletApp - test display token account', () => {
    console.log('--- Test display token account');

    // Last address having 0 ether in its account
    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();

    const expectedData = Object.assign({}, defaultAttributesValues);
    expectedData.walletAddress = global.walletAddresses[0];
    expectedData.accountsDetails = {};
    expectedData.accountsDetails[global.walletAddress] = getEtherAccountDetails(global.walletAddress, '0', '0');
    // Can't test on ether balance since this account is used to create contract
    delete expectedData.etherBalance;
    delete expectedData.totalBalance;
    delete expectedData.totalFiatBalance;
    delete expectedData.accountsDetails[global.walletAddress].balance;
    delete expectedData.accountsDetails[global.walletAddress].balanceFiat;

    expectedData.principalAccount = global.tokenAddress;
    expectedData.overviewAccounts = [global.tokenAddress, 'ether'];
    expectedData.overviewAccountsToDisplay = [global.tokenAddress, 'ether'];
    expectedData.accountsDetails[global.tokenAddress] = getTokenAccountDetails();

    return initApp(app).then(() => {
      expectObjectValueEqual(app.vm, expectedData, 'WalletApp - test display token account: ', ['etherBalance', 'totalBalance', 'totalFiatBalance', 'balance', 'balanceFiat'], true);
    });
  });

  it('WalletApp - test refresh token balance', () => {
    console.log('--- test refresh token balance');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails, initialBalance;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        initialBalance = contractDetails.balance;
        return approveAccount(contractDetails.contract, global.walletAddress, global.walletAddresses[2]);
      })
      .then(() => {
        return sendTokens(contractDetails.contract, global.walletAddress, global.walletAddresses[2], 3);
      })
      .then(() => app.vm.refreshTokenBalance(contractDetails))
      .then(() => {
        expect(Number(contractDetails.balance)).toBe(Number(initialBalance) - 3);
      });
  });
});
