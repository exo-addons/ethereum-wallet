


var Pausable = artifacts.require("Pausable");

contract('Pausablee', function(accounts) {
	
	it ('test pause ' , function(){
		  return Pausable.deployed().then(function(instance){
			  tokenInstance = instance;
			  return tokenInstance.pause();
		  }).then(function(success){
			  assert.equal(success, true, 'test pause failed');
			  return tokenInstance.pause();
		  }).then(function(receipt){
			  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
		      assert.equal(receipt.logs[0].event,'Pause', 'should be the "Pause" event');
		      
		  })
	  });
	
	
	
	
	
	it ('test unpause ' , function(){
		  return Pausable.deployed().then(function(instance){
			  tokenInstance = instance;
			  return tokenInstance.unPause();
		  }).then(function(success){
			  assert.equal(success, false, 'test unpause failed');
			  return tokenInstance.unPause();
		  }).then(function(receipt){
			  assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
		      assert.equal(receipt.logs[0].event,'UnPause', 'should be the "UnPause" event');
		      
		  })
	  });
	


	
});



