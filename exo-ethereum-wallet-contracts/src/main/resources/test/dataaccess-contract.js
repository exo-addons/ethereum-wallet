const ERTToken = artifacts.require("ERTToken");
var ERTTokenDataV1 = artifacts.require("ERTTokenDataV1");

const decimals = Math.pow(10, 18);
  
  contract('DataAccess', function(accounts) {
    let tokenInstance;
    
    
    it('test contract initialization attributes', function() {
      return ERTToken.deployed().then(instance => {
        tokenInstance = instance;
        
        return tokenInstance.getDataAddress(1);
      }).then(function(dataAddress) {
        assert.equal(dataAddress, ERTTokenDataV1.address, 'Token Implementation seems to have wrong data address');
        
        return tokenInstance.initialized();
      }).then(function(result) {
        assert.equal(result, true, 'Token Implementation data is not initialized');
        
        return tokenInstance.name();
      }).then(function(name) {
        assert.equal(name, 'Curries', 'has not the correct name');
        
        return tokenInstance.symbol();
      }).then(function(symbol) {
        assert.equal(symbol, 'C', 'has not the correct symbol');
        
        return tokenInstance.totalSupply();
      }).then(function(totalSupply) {
        assert.equal(totalSupply, 100000 * decimals, 'has not the correct totalSupply');
        
        return tokenInstance.decimals();
      }).then(function(decimals) {
        assert.equal(decimals, 18, 'has not the correct decimals');
        
        
        return tokenInstance.balance(accounts[1]);
      }).then(function(balance) {
        assert.equal(balance, 0, 'has not the correct balance');
        
        return tokenInstance.approve(accounts[1], 1 * decimals, {
          from : accounts[0]
        });
      }).then(receipt => {
        return tokenInstance.getAllowance(accounts[0], accounts[1]);
      }).then(allowed => {
        assert.equal(allowed.toNumber(), 1 * decimals,
          'stores the allowance for delegated trasnfer');
        
        return tokenInstance.isApprovedAccount(accounts[0]);
      }).then(function(ownerApproved) {
        assert.equal(true, ownerApproved, 'Contract owner has to be approved');
        
        return tokenInstance.getGasPriceInToken();
      }).then(function(gaspriceintoken) {
        assert.equal(gaspriceintoken.toNumber(), 50000000000000 , 'gas price in token is wrong');
        
        return tokenInstance.getGasPriceLimit();
      }).then(function(gaspricelimit) {
        assert.equal(gaspricelimit, 100000000000 , 'gas price limit is wrong');
        
        
        return tokenInstance.addAdmin(accounts[1], 1);
      }).then(receipt => {
        return tokenInstance.isAdmin(accounts[1], 1);
      }).then(result => {
        assert.equal(result , true, 'accounts[1] is not admin yet');
        
        return tokenInstance.isPaused();
      }).then(function(result) {
        assert.equal(result, false , 'ERC20 operation is paused');
      });
    })
    
    
    
    
    
    
    it('test setting Data', function() {
      return ERTToken.deployed().then(instance => {
        tokenInstance = instance;
        
        return tokenInstance.setName("ERC");
      }).then(receipt => {
        return tokenInstance.name();
      }).then(result => {
        assert.equal(result , "ERC", 'has not the correct name');
        
        return tokenInstance.setSymbol("R");
      }).then(receipt => {
        return tokenInstance.symbol();
      }).then(result => {
        assert.equal(result , "R", 'has not the correct symbol');
        
        return tokenInstance.setGasPriceLimit(2000);
      }).then(receipt => {
        return tokenInstance.getGasPriceLimit();
      }).then(result => {
        assert.equal(result , 2000, 'has not the correct gas price limit');
      });
    })
    
    
    
    

  
});



