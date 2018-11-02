const ERTToken = artifacts.require("ERTToken");
  
  contract('Burnable', function(accounts) {
    let tokenInstance;

    const decimals = Math.pow(10, 18);

 it('burn tokens', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      
      return tokenInstance.burn(100001 * decimals);
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no burn with value larger than the sender s balance');
      
      return tokenInstance.totalSupply();
    }).then(function(totalSupply) {
      assert.equal(totalSupply, 100000 * decimals, 'has not the correct totalSupply');
      
      
      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      assert.equal(balance.toNumber(), 100000 * decimals, 'Wrong balance of contract owner');
      
      return tokenInstance.burn(3 * decimals , {
        from : accounts[0]
      });
    }).then(receipt => {

      assert(receipt.logs.length = 1,
          'number of emitted event is wrong');
      assert.equal(receipt.logs[0].event, 'Burn',
          'should be the "Burn" event');
      assert.equal(receipt.logs[0].args.burner, accounts[0],
          'the account the tokens are burned from is wrong');
      assert.equal(receipt.logs[0].args.value, 3 * decimals,
          'the burned amount is wrong');
      
      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      assert.equal(balance.toNumber(), (100000-3) * decimals,
          'token balance of burner is wrong');
      
      
      return tokenInstance.totalSupply();
    }).then( totalSupplyB => {
      assert.equal(totalSupplyB.toNumber(), (100000-3) * decimals,
          'total supply is wrong');
     
    });
  }); 
    
    
 
});
  
  
  
  
 
     
     
   