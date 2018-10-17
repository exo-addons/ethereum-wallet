var ERTToken = artifacts.require("ERTToken");

contract('Owned', function(accounts) {
  
  
  let tokenInstance;
  
   
   
   it('should transfer the Ownership', function() {
        return ERTToken.deployed().then(function(instance) {
          tokenInstance = instance;
          return tokenInstance.transferOwnership(accounts[2], {from : accounts[0]});
        }).then(function(result) {
          assert.equal(result.logs.length, 1, 'number of emitted event is wrong');
          assert.equal(result.logs[0].event,'TransferOwnership', 'should be the "TransferOwnership" event');
          assert.equal(result.logs[0].args.newOwner, accounts[2], 'the new owner is wrong');
        });
      }) 
      
    
    
  
});

