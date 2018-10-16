var ERTToken = artifacts.require("ERTToken");

contract('ERTToken', function(accounts) {
	
	var tokenInstance;
	
	it('initializes the contract with the correct values', function() {
	    return ERTToken.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.totalSupply();
	    }).then(function(totalSupply) {
	      assert.equal(totalSupply, 100000, 'has not the correct totalSupply');
	      return tokenInstance.name();
	    }).then(function(name) {
	      assert.equal(name, 'Curries', 'has not the correct name');
	      return tokenInstance.decimals();
	    }).then(function(decimals) {
	      assert.equal(decimals, 18, 'has not the correct decimals'); 
	      return tokenInstance.symbol();
	    }).then(function(symbol) {
	      assert.equal(symbol, 'C', 'has not the correct symbol');
	    });
	  })
	

  it("put 100000 ERTToken in the admin account", function() {
    return ERTToken.deployed().then(function(instance) {
      return instance.balanceOf(accounts[0]);
    }).then(function(adminBalance) {
      assert.equal(adminBalance.valueOf(), 100000, "100000 wasn't in the admin account");
    });
  });
	

  

  it('transfers 10 ERTToken to the account[1]', function() {
	    return ERTToken.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.transfer.call(accounts[1], 5, { from: accounts[0] });
	    }).then(function(success) {
	      assert.equal(success, true, 'Transaction failed');
	      return tokenInstance.transfer(accounts[1], 10, { from: accounts[0] });
	    }).then(function(receipt) {
	      assert.equal(receipt.logs.length, 2, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event,'ApprovedAccount', 'should be the "AppovedAccount" event');
	      assert.equal(receipt.logs[1].event,'Transfer', 'should be the "Transfer" event');
	      assert.equal(receipt.logs[1].args._from, accounts[0], 'the account the tokens are transferred from is wrong');
	      assert.equal(receipt.logs[1].args._to, accounts[1], 'the account the tokens are transferred to is wrong');
	      assert.equal(receipt.logs[1].args._value, 10, 'the transfer amount is wrong');
	      return tokenInstance.balanceOf(accounts[1]);
	    }).then(function(balance) {
	      assert.equal(balance.toNumber(), 10, 'token balance of receiver is wrong');
	      return tokenInstance.balanceOf(accounts[0]);
	    }).then(function(balance) {
	      assert.equal(balance.toNumber(), 100000-10, 'token balance of sender is wrong');
	    });
	  });
  
  
  
  it('approves tokens for delegated transfer', function() {
	    return ERTToken.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.approve.call(accounts[1], 5);
	    }).then(function(success) {
	      assert.equal(success, true, 'transaction failed');
	      return tokenInstance.approve(accounts[2], 5, { from: accounts[0] });
	    }).then(function(receipt) {
	      assert.equal(receipt.logs.length, 2, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event, 'ApprovedAccount', 'should be the "ApprovedAccount" event');
	      assert.equal(receipt.logs[1].event, 'Approval', 'should be the "Approval" event');
	      assert.equal(receipt.logs[1].args._owner, accounts[0], 'the account the tokens are authorized by is wrong');
	      assert.equal(receipt.logs[1].args._spender, accounts[2], 'the account the tokens are authorized to is wrong');
	      assert.equal(receipt.logs[1].args._value, 5, 'the transfer amount is wrong');
	      return tokenInstance.allowed(accounts[0], accounts[2]);
	    }).then(function(allowed) {
	      assert.equal(allowed.toNumber(), 5, 'stores the allowance for delegated trasnfer');
	    });
	  });
  
  
 /* it('handles delegated token transfers', function() {
	    return ERTToken.deployed().then(function(instance) {
	      tokenInstance = instance;
	      fromAccount = accounts[3];
	      toAccount = accounts[4];
	      spendingAccount = accounts[5];
	    
	      return tokenInstance.transfer(fromAccount, 100, { from: accounts[0] });
	    }).then(function(receipt) {
	      return tokenInstance.approve(spendingAccount, 10, { from: fromAccount });
	    }).then(function(receipt) {
	     
	      return tokenInstance.transferFrom.call(fromAccount, toAccount, 10, { from: spendingAccount });
	    }).then(function(success) {
	      assert.equal(success, true);
	      return tokenInstance.transferFrom(fromAccount, toAccount, 10, { from: spendingAccount });
	    }).then(function(receipt) {
	      assert.equal(receipt.logs.length, 2, 'number of emitted event is wrong');
	      assert.equal(receipt.logs[0].event, 'ApprovedAccount', 'should be the "ApprovedAccount" event');
	      assert.equal(receipt.logs[0].args._target, toAccount, 'the account the tokens are transferred to');
	      assert.equal(receipt.logs[1].event, 'Transfer', 'should be the "Transfer" event');
	      assert.equal(receipt.logs[1].args._from, fromAccount, 'logs the account the tokens are transferred from');
	      assert.equal(receipt.logs[1].args._to, toAccount, 'logs the account the tokens are transferred to');
	      assert.equal(receipt.logs[1].args._value, 10, 'logs the transferfrom amount');
	      return tokenInstance.balanceOf(fromAccount);
	    }).then(function(balance) {
	      assert.equal(balance.toNumber(), 90, 'deducts the amount from the sending account');
	      return tokenInstance.balanceOf(toAccount);
	    }).then(function(balance) {
	      assert.equal(balance.toNumber(), 10, 'adds the amount from the receiving account');
	      return tokenInstance.allowance(fromAccount, spendingAccount);
	    }).then(function(allowance) {
	      assert.equal(allowance.toNumber(), 0, 'deducts the amount from the allowance');
	    });
	  });
  
 */ 
  

 
	 
});
