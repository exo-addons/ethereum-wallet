import {getWalletApp, initApp, expectObjectValueEqual} from '../TestUtils.js';

import WalletsettingsModal from '../../main/webapp/vue-app/components/WalletSettingsModal.vue';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('WalletsettingsModal.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    title: false,
    appLoading: false,
    isSpace: false,
    open: false,
    displayResetOption: false,
    fiatSymbol: false,
    overviewAccounts: [],
    principalAccountAddress: false,
    accountsDetails: {},
    loading: false,
    dialog: false,
    error: false,
    walletAddress: false,
    currencies: [
      {symbol: '$ (AUD)', text: 'Australia Dollar (AUD)', value: 'aud'},
      {symbol: 'R$', text: 'Brazil Real (R$)', value: 'brl'},
      {symbol: '$ (CAD)', text: 'Canadian dollar (CAD)', value: 'cad'},
      {symbol: 'CHF', text: 'Switzerland Franc (CHF)', value: 'chf'},
      {symbol: '$ (CLP)', text: 'Chile Peso (CLP)', value: 'clp'},
      {symbol: '¥ (CNY)', text: 'China Yuan Renminbi (CNY)', value: 'cny'},
      {symbol: 'Kč', text: 'Czech Republic Koruna (Kč)', value: 'czk'},
      {symbol: 'kr (DKK)', text: 'Denmark Krone (DKK)', value: 'dkk'},
      {symbol: '€', text: 'Euro Member Countries (€)', value: 'eur'},
      {symbol: '£', text: 'United Kingdom Pound (£)', value: 'gbp'},
      {symbol: '$ (HKD)', text: 'Hong Kong Dollar (HKD)', value: 'hkd'},
      {symbol: 'Ft', text: 'Hungary Forint (Ft)', value: 'huf'},
      {symbol: 'Rp', text: 'Indonesia Rupiah (Rp)', value: 'idr'},
      {symbol: 'INR', text: 'India Rupee (INR)', value: 'inr'},
      {symbol: '¥', text: 'Japan Yen (¥)', value: 'jpy'},
      {symbol: '₩', text: 'Korea (South) Won (₩)', value: 'krw'},
      {symbol: '$ (MXN)', text: 'Mexico Peso (MXN)', value: 'mxn'},
      {symbol: 'RM', text: 'Malaysia Ringgit (RM)', value: 'myr'},
      {symbol: 'kr (NOK)', text: 'Norway Krone (NOK)', value: 'nok'},
      {symbol: '$ (NZD)', text: 'New Zealand Dollar (NZD)', value: 'nzd'},
      {symbol: '₱', text: 'Philippines Piso (₱)', value: 'php'},
      {symbol: '₨', text: 'Pakistan Rupee (₨)', value: 'pkr'},
      {symbol: 'zł', text: 'Poland Zloty (zł)', value: 'pln'},
      {symbol: '$', text: 'United States Dollar ($)', value: 'usd'},
      {symbol: '₽', text: 'Russia Ruble (₽)', value: 'rub'},
      {symbol: 'kr (SEK)', text: 'Sweden Krona (SEK)', value: 'sek'},
      {symbol: '$ (SGD)', text: 'Singapore Dollar (SGD)', value: 'sgd'},
      {symbol: '฿ (THB)', text: 'Thailand Baht (THB)', value: 'thb'},
      {symbol: 'TRY', text: 'Turkey Lira (TRY)', value: 'try'},
      {symbol: 'NT$', text: 'Taiwan New Dollar (NT$)', value: 'twd'},
      {symbol: 'R (ZAR)', text: 'South Africa Rand (ZAR)', value: 'zar'},
    ],
    defaultGas: 0,
    accountType: 0,
    enableDelegation: true,
    autoGeneratedPassword: false,
    backedUp: false,
    defaulGasPriceFiat: 0,
    selectedOverviewAccounts: [],
    selectedPrincipalAccount: false,
    etherAccount: {text: 'Ether', value: 'ether', disabled: false},
    fiatAccount: {text: 'Fiat ($, €...)', value: 'fiat', disabled: false},
    accountsList: [],
    displayWalletResetOption: false,
    useMetamaskChoice: false,
  };

  it('WalletsettingsModal default data', () => {
    console.log('-- WalletsettingsModal default data');

    const walletsettingsModal = mount(WalletsettingsModal, {
      attachToDocument: true,
    });

    expectObjectValueEqual(walletsettingsModal.vm, defaultAttributesValues, 'WalletsettingsModal default data');
  });

  it('WalletsettingsModal --when save preferences', () => {
    console.log('-- WalletsettingsModal --when save preferences');
    const walletsettingsModal = mount(WalletsettingsModal, {
      attachToDocument: true,
    });
    expect(walletsettingsModal.emitted()['settings-changed']).toBeFalsy();
    walletsettingsModal.vm.savePreferences(); // emit = false why ? !!!!!!!!!!!!!!!!!!!!!!!!
  });

  it('WalletsettingsModal --when change Account Type', () => {
    console.log('-- WalletsettingsModal --when change Account Type');
    const walletsettingsModal = mount(WalletsettingsModal, {
      attachToDocument: true,
    });
    expect(walletsettingsModal.emitted()['settings-changed']).toBeFalsy();
    walletsettingsModal.vm.changeAccountType();
    expect(walletsettingsModal.emitted()['settings-changed']).toBeTruthy();
  });

  it('WalletsettingsModal - test when change currency to fiat', (done) => {
    console.log('--- WalletsettingsModal - test when change currency to fiat');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let contractDetails;
    return initApp(app)
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.walletAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);
        return app.vm.$nextTick();
      })
      .then(() => {
        const walletSettingsModal = app.vm.$refs.walletSettingsModal;
        expect(walletSettingsModal).toBeTruthy();

        walletSettingsModal.walletAddress = global.walletAddress;
        walletSettingsModal.open = true;
        walletSettingsModal.getOverviewAccountObject('fiat');

        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.principalAccountAddress = global.tokenAddress;
        expectedData.overviewAccounts = global.defaultWalletSettings.defaultOverviewAccounts;
        expectedData.fiatSymbol = '$';
        expectedData.walletAddress = global.walletAddress;
        expectedData.open = true;
        expectedData.fiatAccount = {text: 'Fiat ($, €...)', value: 'fiat', disabled: false};

        expectObjectValueEqual(walletSettingsModal, expectedData, 'WalletsettingsModal   --------', null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
