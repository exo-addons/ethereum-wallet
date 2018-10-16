var ApprouvableBuyerAccount = artifacts.require("ApprouvableBuyerAccount");

contract('ApprouvableBuyerAccount', function(accounts) {
	
	var tokenInstance;
	
	
  it ('test approveBuyerAccount ' , function(){
	  return ApprouvableBuyerAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.approveBuyerAccount.call(accounts[6]);
	  }).then(function(success){
		  assert.equal(success, true, 'test approveBuyerAccount failed');
		  return tokenInstance.approveBuyerAccount.call(accounts[6]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'ApprovedBuyerAccount', 'should be the "AppovedBuyerAccount" event');
	      assert.equal(receipt.logs[0].args.target, accounts[6], 'the approved buyer account is wrong');
	  })
  });
  
  
	
  it ('test disapproveBuyerAccount ' , function(){
	  return ApprouvableBuyerAccount.deployed().then(function(instance){
		  tokenInstance = instance;
		  return tokenInstance.disapproveBuyerAccount.call(accounts[6]);
	  }).then(function(success){
		  assert.equal(success, false, 'test disapproveBuyerAccount failed');
		  return tokenInstance.disapproveBuyerAccount.call(accounts[6]);
	  }).then(function(receipt){
		  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'DisapprovedBuyerAccount', 'should be the "DisAppovedBuyerAccount" event');
	      assert.equal(receipt.logs[0].args._target, accounts[6], 'the disapproved buyer account is wrong');
	  })
  });
  
  
  
  

  
  
  
 
  
  
});

