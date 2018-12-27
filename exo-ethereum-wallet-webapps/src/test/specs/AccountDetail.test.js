import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import AccountDetail from '../../main/webapp/vue-app/components/AccountDetail';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('AccountDetail.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    isReadOnly: false,
    isDisplayOnly: false,
    networkId: 0,
    walletAddress: false,
    wallet: false,
    fiatSymbol: '$',
    selectedTransactionHash: false,
    contractDetails: {},
    isAdministration: false,
    refreshing: false,
    enableDelegation: false,
    error: false,
    fiatBalance: '0 $',
    balance: '',
  };

  it('AccountDetail default data', () => {
    console.log('-- Test AccountDetail default data');

    const accountDetail = mount(AccountDetail, {
      attachToDocument: true,
    });

    expectObjectValueEqual(accountDetail.vm, defaultAttributesValues, 'AccountDetail default data: ');
    expectCountElement(accountDetail, 'accountDetail', 0);
  });

  it('AccountDetail transaction-sent event', () => {
    console.log('-- Test transaction-sent event');

    const accountDetail = mount(AccountDetail, {
      attachToDocument: true,
    });

    const transaction = {
      from: global.walletAddress,
      to: global.walletAddress,
      value: 1,
    };
    accountDetail.vm.newTransactionPending(transaction, {});
    expect(accountDetail.emitted()['transaction-sent']).toBeTruthy();
  });

  it('AccountDetail - test send ether and refresh balance', (done) => {
    console.log('--- test send ether and refresh balance');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app).then(() => {
      const accountDetails = app.vm.accountsDetails[global.walletAddress];
      expect(accountDetails).not.toBeNull();
      app.vm.openAccountDetail(accountDetails);

      return app.vm.$nextTick(() => {
        try {
          expectCountElement(app, 'accountDetail', 1);

          const accountDetailCmp = app.vm.$refs.accountDetail;
          const initialBalance = accountDetailCmp.contractDetails.balance;
          return sendEther(global.walletAddress, global.walletAddresses[2], 3)
            .then(() => accountDetailCmp.refreshBalance())
            .then(() => {
              expect(Number(accountDetailCmp.contractDetails.balance) < Number(initialBalance) - 3).toBeTruthy();
              done();
            });
        } catch (e) {
          return done(e);
        }
      });
    });
  });

  it('AccountDetail - test open ether account details', (done) => {
    console.log('--- test open ether account details');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app).then(() => {
      const accountDetails = app.vm.accountsDetails[global.walletAddress];
      expect(accountDetails).not.toBeNull();
      app.vm.openAccountDetail(accountDetails);

      return app.vm.$nextTick(() => {
        try {
          expectCountElement(app, 'accountDetail', 1);

          const accountDetailCmp = app.vm.$refs.accountDetail;

          const expectedData = Object.assign({}, defaultAttributesValues);
          expectedData.isReadOnly = true;
          expectedData.networkId = global.testNetworkId;
          expectedData.walletAddress = global.walletAddress;
          expectedData.contractDetails = {
            address: global.walletAddress,
            isContract: false,
            symbol: 'ether',
            title: 'ether',
          };

          expectObjectValueEqual(accountDetailCmp, expectedData, 'AccountDetail - test open ether account details: ', ['icon', 'contract', 'retrievedAttributes', 'balance', 'etherBalance', 'fiatBalance'], true);

          expect(accountDetailCmp.contractDetails.balance > 0).toBeTruthy();
          expect(accountDetailCmp.fiatBalance).toBe(`${accountDetailCmp.contractDetails.balanceFiat} ${accountDetailCmp.fiatSymbol}`);

          app.vm.back();
          expectCountElement(app, 'accountDetail', 0);
        } catch (e) {
          return done(e);
        }
        done();
      });
    });
  });

  it('AccountDetail - test open token account details', (done) => {
    console.log('--- test open token account details');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app).then(() => {
      return app.vm.$nextTick(() => {
        try {
          const accountDetails = app.vm.accountsDetails[global.tokenAddress];
          expect(accountDetails).not.toBeNull();
          app.vm.openAccountDetail(accountDetails);

          return app.vm.$nextTick(() => {
            try {
              expectCountElement(app, 'accountDetail', 1);

              const accountDetailCmp = app.vm.$refs.accountDetail;

              const expectedData = Object.assign({}, defaultAttributesValues);
              expectedData.isReadOnly = true;
              expectedData.networkId = global.testNetworkId;
              expectedData.walletAddress = global.walletAddress;
              expectedData.contractDetails = {
                address: global.tokenAddress,
                isContract: true,
                isDefault: true,
                defaultContract: true,
                isApproved: true,
                isAdmin: true,
                isPaused: false,
                contractType: 1,
                contractTypeLabel: 'ERT Token',
                networkId: global.testNetworkId,
                name: global.tokenName,
                title: global.tokenName,
                symbol: global.tokenSymbol,
                decimals: global.tokenDecimals,
                totalSupply: global.tokenSupply,
                sellPrice: global.tokenSellPrice,
                owner: global.walletAddresses[0],
                isOwner: true,
                adminLevel: 5,
              };

              expectObjectValueEqual(accountDetailCmp, expectedData, 'AccountDetail - test open token account details', ['icon', 'contract', 'retrievedAttributes', 'balance', 'etherBalance'], true);

              expect(accountDetailCmp.contractDetails.balance > 0).toBeTruthy();
              expect(accountDetailCmp.contractDetails.etherBalance > 0).toBeTruthy();

              app.vm.back();
              expectCountElement(app, 'accountDetail', 0);
            } catch (e) {
              return done(e);
            }
            done();
          });
        } catch (e) {
          return done(e);
        }
      });
    });
  });

  it('AccountDetail - test send tokens and refresh balance', (done) => {
    console.log('--- test send tokens and refresh balance');

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    return initApp(app).then(() => {
      const accountDetails = app.vm.accountsDetails[global.tokenAddress];
      expect(accountDetails).not.toBeNull();
      app.vm.openAccountDetail(accountDetails);

      return app.vm.$nextTick(() => {
        try {
          expectCountElement(app, 'accountDetail', 1);

          const accountDetailCmp = app.vm.$refs.accountDetail;
          const initialBalance = accountDetailCmp.contractDetails.balance;
          return sendTokens(accountDetailCmp.contractDetails.contract, global.walletAddress, global.walletAddresses[2], 3)
            .then(() => accountDetailCmp.refreshBalance())
            .then(() => {
              expect(Number(accountDetailCmp.contractDetails.balance)).toBe(Number(initialBalance) - 3);
              done();
            });
        } catch (e) {
          return done(e);
        }
      });
    });
  });
});
