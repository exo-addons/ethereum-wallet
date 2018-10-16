var Owned = artifacts.require("Owned");

contract('Owned', function(accounts) {
	
	

	/* it("sets an owner", async () => {
		    const owner = await Owned.new();
		    assert.equal(await owner.owner.call(), msg.sender);
		  }); */
	 
	 
	 
	 it('test owner', function() {
		    return Owned.deployed().then(function(instance) {
		      return owner.call();
		    }).then(function(result) {
		      assert.equal(result, msg.sender, 'has not the correct owner');
		    });
		  }) 
		  
	  
	  
	
});





 

 
