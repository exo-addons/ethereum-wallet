var ERTToken = artifacts.require("ERTToken");
var TestERTTokenV2 = artifacts.require("TestERTTokenV2");
var ERTTokenV1 = artifacts.require("ERTTokenV1");
var ERTTokenDataV1 = artifacts.require("ERTTokenDataV1");
var TestERTTokenDataV2 = artifacts.require("TestERTTokenDataV2");
var TestERTTokenV3 = artifacts.require("TestERTTokenV3");

const decimals = Math.pow(10, 18);

contract('Proxy', function(accounts) {
   
  let tokenInstance;
  
  it('send ether to ERTToken ', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;  
          return tokenInstance.transfer(ERTToken.address, 5 * decimals, {from : accounts[0]});
        }).then(function(result) {
          return tokenInstance.balanceOf(ERTToken.address);
        }).then(function(result) {
          assert.equal(result, 5 * decimals, 'the balance of ERTToken is wrong ');
        });
  });
   
   it('when new implementation V2 was provided', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance;
         return tokenInstance.upgradeImplementation(1, TestERTTokenV2.address );
         }).then(assert.fail).catch(function(error) {
           assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the given implementation version is equal to the current one');
          return tokenInstance.upgradeImplementation(2, ERTTokenV1.address );
         }).then(assert.fail).catch(function(error) {
           assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the given implementation is equal to the current one');
           return tokenInstance.upgradeImplementation(2, TestERTTokenV2.address, {from: accounts[5]} );
         }).then(assert.fail).catch(function(error) {
           assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender was not the owner');
           return tokenInstance.implementationAddress.call();
         }).then(function(implementation) {
           assert.equal(implementation, ERTTokenV1.address, 'should return the current implementation');    
          return tokenInstance.upgradeImplementation(2, TestERTTokenV2.address);
         }).then(function(receipt) {
          assert(receipt.logs.length > 0, 'number of emitted event is wrong');
          const upgradedEvent = receipt.logs.find(log => log && log.event && log.event === 'Upgraded');
          assert.notEqual(upgradedEvent, null ,'should be the "Upgraded" event');
          assert.equal(receipt.logs[0].args.implementationVersion, 2, 'the implementation version is wrong');
          assert.equal(receipt.logs[0].args.implementationAddress, TestERTTokenV2.address,'the implementation address is wrong');
          return tokenInstance.implementationAddress.call();
         }).then(function(implementation) {
          assert.equal(implementation, TestERTTokenV2.address, 'should return the given implementation');
         });
   })
   
   it('check the balances of contracts after upgrading impl ', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;  
          return tokenInstance.balanceOf(ERTToken.address);
        }).then(function(result) {
          assert.equal(result, 5 * decimals, 'the balance of the ERTToken is changed');
          return tokenInstance.balanceOf(ERTTokenV1.address);
        }).then(function(result) {
          assert.equal(result, 0, 'the balance of ERTTokenV1 shouled be 0 ');
          return tokenInstance.balanceOf(TestERTTokenV2.address);
        }).then(function(result) {
          assert.equal(result, 0, 'the balance of TestERTokenV2 shouled be 0');
        });
  });
   
    it('Old implementation should be paused', function() {
     return ERTTokenV1.deployed()
       .then(instance => {
         tokenInstance = instance;
          return tokenInstance.paused.call();
         }).then(function(result) {
           assert.equal(result, true, 'old and useless implementation should be paused'); 
         });
   })
   
    it('new implementation should be not paused', function() {
     return TestERTTokenV2.deployed()
       .then(instance => {
         tokenInstance = instance;
          return tokenInstance.paused.call();
         }).then(function(result) {
           assert.equal(result, false, 'new implementation should be not paused'); 
         });
   })
   
   it('proxy should be not paused', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance;
          return tokenInstance.paused.call();
         }).then(function(result) {
           assert.equal(result, false, 'proxy should be  not paused'); 
         });
   })
   
   it('when new data version was added', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance; 
          return tokenInstance.upgradeData(2, TestERTTokenDataV2.address);
         }).then(function(receipt) {
          return tokenInstance.getDataAddress(2);
         }).then(function(data) {
          assert.equal(data, TestERTTokenDataV2.address, 'should return the new data address added');
          return tokenInstance.getDataAddress(1);
         }).then(function(data) {
          assert.equal(data, ERTTokenDataV1.address, 'should return the new data address added');
         });
   })
   
   it('when access to data from previous and new contracts', function() {
       return ERTToken.deployed()
         .then(instance => {
           tokenInstance = instance;  
             return tokenInstance.getDataAddress(1);
           }).then(function(dataAddress) {
             assert.equal(dataAddress, ERTTokenDataV1.address, 'Token Implementation seems to have wrong data address');
             return tokenInstance.getDataAddress(2);
           }).then(function(dataAddress) {
             assert.equal(dataAddress, TestERTTokenDataV2.address, 'Token Implementation seems to have wrong data address');
             return tokenInstance.name();
           }).then(function(name) {
             assert.equal(name, 'Curries', 'has not the correct name');
             return tokenInstance.totalSupply();
           }).then(function(totalSupply) {
             assert.equal(totalSupply.toNumber(), 100000 * decimals, 'has not the correct totalSupply');
             return tokenInstance.balanceOf(accounts[0]);
           }).then(balance => {
             assert.equal(balance.toNumber(), (100000-5) * decimals, 'Wrong balance of contract owner');
             return tokenInstance.isFrozen(accounts[5]);
           }).then(function(result) {
             assert.equal(result, false, 'accounts should be not freezen');
           });
   })
     
    it('test data write using new contract Imp ', function() {
   return ERTToken.deployed()
     .then(instance => {
       tokenInstance = instance;  
         return tokenInstance.freeze(accounts[5]);
       }).then(function(result) {
         return tokenInstance.isFrozen(accounts[5]);
       }).then(function(result) {
         assert.equal(result, true, 'accounts shouled be frozen ');
       });
 })
 
   it('should not make transfer token after freeze accounts ', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance;  
         return tokenInstance.transfer(accounts[5], 50 * decimals, {from : accounts[0]});
           }).then(receipt => {
           return tokenInstance.balanceOf(accounts[5]);
         }).then(balance => {
           assert.equal(balance.toNumber(), 50 * decimals, 'Wrong balance of contract owner');
           return tokenInstance.transfer(accounts[3], 20 * decimals, {from : accounts[5]});
         }).then(assert.fail).catch(function(error) {
           assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is frozen');
           return tokenInstance.unFreeze(accounts[5]);
         }).then(function(result) {
           return tokenInstance.isFrozen(accounts[5]);
         }).then(function(result) {
           assert.equal(result, false, 'accounts shouled be not frozen ');
           return tokenInstance.transfer(accounts[0], 20 * decimals, {from : accounts[5]});
         }).then(receipt => {
           return tokenInstance.balanceOf(accounts[5]);
         }).then(balance => {
           assert.equal(balance.toNumber(), 30 * decimals, 'Wrong balance of accounts[5], the sender is not Frozen so he made the transfer');
         });
   })
   
   it('Test Data write for OLD implementation ', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance;  
           return tokenInstance.setSellPrice(6);
         }).then(function(result) {
           return tokenInstance.getSellPrice();
         }).then(function(sellPrice) {
           assert.equal(sellPrice.toNumber(), 6, 'writing for old implementation is wrong should be the new setting ');
         });
   })
   
       it('when new implementation V3 was provided', function() {
     return ERTToken.deployed()
       .then(instance => {
         tokenInstance = instance;
           return tokenInstance.implementationAddress.call();
         }).then(function(implementation) {
           assert.equal(implementation, TestERTTokenV2.address, 'should return the current implementation');    
           return tokenInstance.upgradeImplementation(3, TestERTTokenV3.address);
         }).then(function(receipt) {
          return tokenInstance.implementationAddress.call();
         }).then(function(implementation) {
          assert.equal(implementation, TestERTTokenV3.address, 'should return the given implementation'); 
         });
      })
   
      
      it('access to data and use Imp 2 and 1', function() {
        return ERTToken.deployed()
          .then(instance => {
            tokenInstance = instance;
           
            return tokenInstance.totalSupply();
          }).then(function(totalSupply) {
            assert.equal(totalSupply, 100000 * decimals, 'has not the correct totalSupply');
            return tokenInstance.freeze(accounts[6]);
          }).then(function(result) {
            return tokenInstance.isFrozen(accounts[6]);
          }).then(function(result) {
            assert.equal(result, true, 'accounts shouled be frozen ');
            return tokenInstance.transfer(accounts[6], 50 * decimals, {from : accounts[0]});
          }).then(receipt => {
            return tokenInstance.unFreeze(accounts[6]);
          }).then(function(result) {
            return tokenInstance.transfer(accounts[0], 20 * decimals, {from : accounts[6]});
          }).then(receipt => {
            return tokenInstance.balanceOf(accounts[6]);
          }).then(balance => {
            assert.equal(balance.toNumber(), 30 * decimals, 'Wrong balance of accounts[6], the sender is not Frozen so he made the transfer');
        });
      }); 
   
   
   
});


