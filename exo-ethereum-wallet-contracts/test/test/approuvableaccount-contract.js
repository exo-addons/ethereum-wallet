var ApprouvableAccount = artifacts.require("ApprouvableAccount");

contract('ApprouvableAccount', function(accounts) {
	
	var tokenInstance;
	
	
  it ('test approveAccount ' , function(){
	  return ApprouvableAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.approveAccount.call(accounts[5]);
	  }).then(function(success){
		  assert.equal(success, true, 'test approveAccount failed');
		  return tokenInstance.approveAccount.call(accounts[5]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'ApprovedAccount', 'should be the "AppovedAccount" event');
	      assert.equal(receipt.logs[0].args.target, accounts[5], 'the approved account is wrong');
	  })
  });
  
  
  
  it ('test disapproveAccount ' , function(){
	  return ApprouvableAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.disapproveAccount.call(accounts[5]);
	  }).then(function(success){
		  assert.equal(success, false, 'test disapproveAccount failed');
		  return tokenInstance.approveAccount.call(accounts[5]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'DisapprovedAccount', 'should be the "DisappovedAccount" event');
	      assert.equal(receipt.logs[0].args.target, accounts[5], 'the disapproved account is wrong');
	  })
  }) ;
  
  
});






