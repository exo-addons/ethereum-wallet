var Charitable = artifacts.require("Charitable");

contract('Charitable', function(accounts) {
	
	var tokenInstance;
	
	
  
  it("should set the value of minbalance ", function() {
	  return Charitable.deployed().then(function(instance) { 
	    tokenInstance = instance;
	    return tokenInstance.setMinBalance(20);
	  }).then(function( minBalanceForAccounts) {
	    assert.equal( minBalanceForAccounts, 20 * 0.001, "the value was not set.");
	  });
	});
  
  
  
  it ('test approveBuyerAccount ' , function(){
	  return Charitable.deployed().then(function(instance){
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
  
  
  

  
	 
  
  

  
  
 
  
  
});

