var ERTToken = artifacts.require("ERTToken");

contract('Owned', function(accounts) {
  
  let tokenInstance;
  it('should transfer the Ownership', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.transferOwnership(accounts[2],{from : accounts[1]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the owner');
      return tokenInstance.transferOwnership(accounts[2], {from : accounts[0]});
    }).then(function(result) {
      assert.equal(result.logs.length, 1, 'number of emitted event is wrong');
      assert.equal(result.logs[0].event,'TransferOwnership', 'should be the "TransferOwnership" event');
      assert.equal(result.logs[0].args.newOwner, accounts[2], 'the new owner is wrong');
    });
  })

  it('should set the address of the proxy', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.setProxy(ERTToken.address,{from : accounts[1]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the owner');
      return tokenInstance.setProxy(0x0, {from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the new proposed proxy is the zero address');
    });
  })
});










