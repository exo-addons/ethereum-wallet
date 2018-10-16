var FundCollection = artifacts.require("FundCollection");
var ERTToken = artifacts.require("ERTToken");


contract('FundCollection', function(accounts) {
	
	 var buyPrice = 100000; 

	
	  
	  
	  it('Buy tokens', function() {
		    return ERTToken.deployed().then(function(instance) {
		      tokenInstance = instance;
		      return FundCollection.deployed();
		    }).then(function(instance) {
		      tokenSaleInstance = instance;
		      return tokenSaleInstance.buy( { from: account[0], value: msg.value / buyPrice })
		    }).then(function(receipt) {
		      assert.equal(receipt.logs.length, 2, 'triggers number of event is wrong');
		      assert.equal(receipt.logs[0].event, 'Transfer', 'should be the "Transfer" event');
		      assert.equal(receipt.logs[1].event, 'FundsReceived', 'should be the "FundsReceived" event');
		      assert.equal(receipt.logs[0].args._from, account[0], 'the account that buy token is wrong');
		      //assert.equal(receipt.logs[0].args._to, , 'the account that will recieve token is wrong');
		      assert.equal(receipt.logs[0].args._value, account[0], 'the amount that will be baught is wrong');
		      assert.equal(receipt.logs[1].args.from, account[0], 'the account that buy token is wrong');
		      assert.equal(receipt.logs[1].args.amount, msg.value / buyPrice, 'the amount that will be baught is wrong');
		   
		      return tokenInstance.balanceOf(account[0]);
		    }).then(function(balance) {
		      assert.equal(balance.toNumber(), 100000 - msg.value / buyPrice);
		    
		      
		    });
		  });
	  
	  
	
});





