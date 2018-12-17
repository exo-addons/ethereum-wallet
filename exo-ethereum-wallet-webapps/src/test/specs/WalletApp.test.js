const WalletApp = require('../../main/webapp/vue-app/components/WalletApp');
const WalletBrowserSetup = require('../../main/webapp/vue-app/components/WalletBrowserSetup');
const WalletAppMenu = require('../../main/webapp/vue-app/components/WalletAppMenu');
const WalletSetup = require('../../main/webapp/vue-app/components/WalletSetup');
const WalletSummary = require('../../main/webapp/vue-app/components/WalletSummary');
const WalletAccountsList = require('../../main/webapp/vue-app/components/WalletAccountsList');
const AccountDetail = require('../../main/webapp/vue-app/components/AccountDetail');
const WalletSettingsModal = require('../../main/webapp/vue-app/components/WalletSettingsModal');
const AddContractModal = require('../../main/webapp/vue-app/components/AddContractModal');
const FIAT_CURRENCIES = require('../../main/webapp/vue-app/WalletConstants');
const {getContractsDetails, retrieveContractDetails} = require('../../main/webapp/vue-app/WalletToken');
const {initWeb3, initSettings, computeBalance, etherToFiat, gasToEther} = require('../../main/webapp/vue-app/WalletUtils');
const { shallowMount, mount, wrapper} = require('@vue/test-utils');
const Vue = require ('vue');

//beforeAll(() => {
//  if (!global.server) {
//    global.server = global.TestRPC.server();
//    global.server.listen(7545, function(err, blockchain) {});
//// console.log(global.server.provider.manager.state.accounts)
//  }
//});

//afterAll(() => {
//  if (global.server) {
//   global.server.close();    
//  }
//});



//function getAccountDetails(address) {
 // return {
  //  title : 'ether',
   // icon : 'fab fa-ethereum',
  //  symbol : 'ether',
  //  isContract : false,
  //  address : address,
   // balance : balanceDetails && balanceDetails.balance ? balanceDetails.balance : 0,
   // balanceFiat : balanceDetails && balanceDetails.balanceFiat ? balanceDetails.balanceFiat : 0
 // };
//}


describe('WalletApp.test.js', () => {
  let app;
  const overviewAccounts = [{
    title : 'ether',
    icon : 'fab fa-ethereum',
    symbol : 'ether',
    isContract : true,
  }];
  
  const walletAddress = '0x104E1124A0A64dc13b02effdD675b860c66F72d5';
  
  
  beforeEach(() => {
        
    app = shallowMount(WalletApp, {
      
      attachToDocument: true,
    });
    app.setData({ walletAddress : walletAddress , overviewAccounts : overviewAccounts})

  });
  
  

  it('wallet has minimized class by default', () => {
    expect(app.find('#WalletApp').classes()).toContain('minimized');
    expect(app.find('#WalletApp').classes()).not.toContain('maximized');
  });
  
  
  it('wallet app toolbar should exist only if wallet is enabled', () => {
    expect(app.find('.walletAppToolbar').exists()).toBe(false);
    app.vm.isWalletEnabled = true;
    expect(app.find('.walletAppToolbar').exists()).toBe(true);
  });
  
  
  it('contient les données par défaut', () => {
    expect(typeof WalletApp.data).toBe('function')
    const defaultData = WalletApp.data()
    expect(defaultData.isWalletEnabled).toBe(false)
    expect(defaultData.loading).toBe(true)
    expect(defaultData.useMetamask).toBe(false)
    expect(defaultData.isReadOnly).toBe(true)
    expect(defaultData.isSpaceAdministrator).toBe(false)
    expect(defaultData.seeAccountDetails).toBe(false)
    expect(defaultData.seeAccountDetailsPermanent).toBe(false)
    expect(defaultData.principalAccount).toBeNull()
    expect(defaultData.showSettingsModal).toBe(false)
    expect(defaultData.showAddContractModal).toBe(false)
    expect(defaultData.displayWalletSetup).toBe(false)
    expect(defaultData.displayWalletNotExistingYet).toBe(false)
    expect(defaultData.networkId).toBeNull()
    expect(defaultData.browserWalletExists).toBe(false)
    expect(defaultData.browserWalletBackedUp).toBe(true)
    expect(defaultData.walletAddress).toBeNull()
    expect(defaultData.selectedTransactionHash).toBeNull()
    expect(defaultData.selectedAccount).toBeNull()
    expect(defaultData.fiatSymbol).toBe('$')
    expect(defaultData.refreshIndex).toBe(1)
    expect(defaultData.errorMessage).toBeNull()
    expect(defaultData.overviewAccounts).toHaveLength(0)
    expect(defaultData.overviewAccountsToDisplay).toHaveLength(0)
  })
  

  it('init ', () => {
  expect(app.vm.init());
  expect(app.vm.loading).toBe(true);
  expect(app.vm.errorMessage).toBeNull();
  expect(app.vm.seeAccountDetails).toBe(false);
  expect(app.vm.walletAddress).toBeNull();
  });

 
 
 it('display Accounts List', () => {
   expect(app.vm.displayAccountsList).toBeTruthy();
 });
 
 
 it('Balance by default', () => {
   expect(app.vm.etherBalance).toBe(0);
   expect(app.vm.totalFiatBalance).toBe(0);
   expect(app.vm.totalBalance).toBe(0);
 });
  
 
 
//
// it('refresh token balance', () => {
//   expect(app.vm.refreshTokenBalance( getAccountDetails('0xdbd2da3d1dcd87eea6b94351e92e78058b5f9318')));
//  
// });

 
 
it('refresh token balance', () => {
expect(app.vm.refreshBalance());

});

 
 

// it('open account detail', () => {
//expect(app.vm.openAccountDetail( getAccountDetails('0x76291828321273608d29bfef00a73f4fcf7f0f3a'), hash).toBeTruthy());
// });
 
 

it('wallet app, menu', () => {
  expect(app.find('.wallet-app-menu').exists()).toBe(false);
 // app.vm.showSettingsModal: true
  
});


 
});









