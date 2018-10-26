var OwnedUpgradeabilityProxy = artifacts.require("OwnedUpgradeabilityProxy");
var ERTToken = artifacts.require("ERTToken");


contract('OwnedUpgradeabilityProxy', function(accounts) {

  let proxy;

  it('has an owner', function() {
    return OwnedUpgradeabilityProxy.deployed().then(instance => {
        proxy = instance;
      return proxy.proxyOwner();
    }).then(function(proxyOwner) {
      assert.equal(proxyOwner, accounts[0] , 'has not the correct proxyowner');
      
    });
  })


  it('transfers the ownership', function() {
    return OwnedUpgradeabilityProxy.deployed().then(instance => {
        proxy = instance;
      return proxy.transferProxyOwnership(accounts[1],{from : accounts[0]});
    }).then(function(receipt) {
        assert.equal(receipt.logs.length, 1);
        assert.equal(receipt.logs[0].event, 'ProxyOwnershipTransferred');
        assert.equal(receipt.logs[0].args.previousOwner, accounts[0]);
        assert.equal(receipt.logs[0].args.newOwner, accounts[1]);
        return proxy.transferProxyOwnership(accounts[0],{from : accounts[2]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is the token owner');
      
      return proxy.transferProxyOwnership(accounts[2],{from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the owner');
      
      return proxy.transferProxyOwnership(0x0,{from : accounts[1]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the new proposed owner is the zero address');
      
      
    }); 
  })
  
  
  
  
  it('when no initial implementation was provided', function() {
    return OwnedUpgradeabilityProxy.deployed().then(instance => {
        proxy = instance;
      return proxy.implementation();
    }).then(function(result) {
        assert.equal(result, 0x0 , 'zero address should be returned');
    })
  })
  
  
       it('when an implementation was provided', function() {
    return OwnedUpgradeabilityProxy.deployed().then(instance => {
        proxy = instance; 
       return proxy.upgradeTo(ERTToken.address , {from : accounts[1]});
    }).then(function(result) {
        return proxy.implementation();
          }).then(function(implementation) {
              assert.equal(implementation, ERTToken.address, 'should return the given implementation');   
   
              return proxy.upgradeTo(ERTToken.address , {from : accounts[1]});
          }).then(assert.fail).catch(function(error) {
            assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the given implementation is equal to the current one');
            
            
            return proxy.upgradeTo(ERTToken.address , {from : accounts[2]});
          }).then(assert.fail).catch(function(error) {
            assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the proxy owner');
            
            return proxy.upgradeTo(0x0 , {from : accounts[2]});
          }).then(assert.fail).catch(function(error) {
            assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the new implementation is the zero address'); 
          }); 
  })
  
  
  
    it('upgrade and call', function() {
    return OwnedUpgradeabilityProxy.deployed().then(instance => {
        proxy = instance; 
        return proxy.upgradeToAndCall(ERTToken.address , ERTToken.abi , {from : accounts[1]});
    }).then(function(result) {
        return proxy.implementation();
          }).then(function(implementation) {
              assert.equal(implementation, ERTToken.address , 'should upgrades to the given implementation');   
     
       
          }); 
  })


})



     

         
          






