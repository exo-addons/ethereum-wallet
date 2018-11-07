var ERTToken = artifacts.require("ERTToken");
var TestERTTokenV2 = artifacts.require("TestERTTokenV2");
var ERTTokenV1 = artifacts.require("ERTTokenV1");

contract('Proxy', function(accounts) {
   
  let tokenInstance;
  
   it('when new implementation was provided', function() {
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
          assert(receipt.logs.length = 1, 'number of emitted event is wrong');
          assert.equal(receipt.logs[0].event, 'Upgraded', 'should be the "Upgraded" event');
          assert.equal(receipt.logs[0].args.implementationVersion, 2, 'the implementation version is wrong');
          assert.equal(receipt.logs[0].args.implementationAddress, TestERTTokenV2.address,'the implementation address is wrong');
          return tokenInstance.implementationAddress.call();
         }).then(function(implementation) {
          assert.equal(implementation, TestERTTokenV2.address, 'should return the given implementation');
         });
   })
   
   
   
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
   
   
});


