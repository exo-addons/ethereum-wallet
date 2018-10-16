var FreezableAccount = artifacts.require("FreezableAccount");

contract('FreezableAccount', function(accounts) {
	
	var tokenInstance;
	
	
  it ('test freezeAccount ' , function(){
	  return FreezableAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.freezeAccount.call(accounts[8]);
	  }).then(function(success){
		  assert.equal(success, true, 'test freezeAccount failed');
		  return tokenInstance.freezeAccountt.call(accounts[8]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'FrozenAccount', 'should be the "FrozenAccount" event');
	      assert.equal(receipt.logs[0].args._target, accounts[8], 'the frozen account is wrong');
	  })
  });
  
  
  
  it ('test unFrozenAccount ' , function(){
	  return FreezableAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.unFrozenAccount.call(accounts[8]);
	  }).then(function(success){
		  assert.equal(success, false, 'test unfrozenAccount failed');
		  return tokenInstance.unFrozenAccountt.call(accounts[8]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'UnFrozenAccount', 'should be the "UnFrozenAccount" event');
	      assert.equal(receipt.logs[0].args._target, accounts[8], 'the frozen account is wrong');
	  })
  });
  
  
  
});


