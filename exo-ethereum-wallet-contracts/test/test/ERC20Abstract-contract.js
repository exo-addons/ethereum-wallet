var ERC20Abstract = artifacts.require("ERC20Abstract");

contract('ERC20Abstract', function(accounts) {
	
	var tokenInstance;
	
	

	  it('test _transfer', function() {
		    return ERC20Abstract.deployed().then(function(instance) {
		      tokenInstance = instance;
		      return tokenInstance.transfer.call(accounts[0],accounts[7], 999999999999999999999);
		    }).then(assert.fail).catch(function(error) {
		      assert(error.message.indexOf('revert') >= 0, 'error message must contain revert');
		      return tokenInstance.transfer.call(accounts[0],0x0, 5);
		    }).then(assert.fail).catch(function(error) {
		      assert(error.message.indexOf('revert') >= 0, 'error message must contain revert');
		      return tokenInstance.transfer.call(accounts[0],accounts[7], 0);
		    }).then(assert.fail).catch(function(error) {
		      assert(error.message.indexOf('revert') >= 0, 'error message must contain revert');
		      return tokenInstance.transfer(accounts[0],accounts[7],  10  );
		    }).then(function(receipt) {
		      assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
		      assert.equal(receipt.logs[0].event,'Transfer', 'should be the "Transfer" event');
		      assert.equal(receipt.logs[0].args._from, accounts[0], 'the account the tokens are transferred from is wrong');
		      assert.equal(receipt.logs[0].args._to, accounts[7], 'the account the tokens are transferred to is wrong');
		      assert.equal(receipt.logs[0].args._value, 5, 'the transfer amount is wrong');
		    });
		  });
	  
 	 
});




