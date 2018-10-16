var SafeMath = artifacts.require("SafeMath");

contract('SafeMath', function(accounts) {
	
	var tokenInstance;
	
	it('test add', function() {
	    return SafeMath.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.safeAdd(1,2);
	    }).then(function(result) {
	      assert.equal(result, 3, 'has not the correct value');
	    });
	  })
	  
	  
	  it('test substract', function() {
	    return SafeMath.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.safeSubtract(4,2);
	    }).then(function(result) {
	      assert.equal(result, 2, 'has not the correct value');
	    });
	  })
	  
	  
	  it('test Multiplicity', function() {
	    return SafeMath.deployed().then(function(instance) {
	      tokenInstance = instance;
	      return tokenInstance.safeMult(4,2);
	    }).then(function(result) {
	      assert.equal(result, 8, 'has not the correct value');
	    });
	  })
	  
	  
	
});
