const ERTToken = artifacts.require("ERTToken");
  
  contract('Admin', function(accounts) {
    let tokenInstance;
    
    
    it ('test addAdmin ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.addAdmin(accounts[6], 7 , {
          from : accounts[1]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the level <= 0 or >= 6');  
        return tokenInstance.addAdmin(accounts[6], 1 , {
          from : accounts[1]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the owner');  
        return tokenInstance.addAdmin(accounts[5], 5, {
            from : accounts[0]
        });
      }).then(receipt => {
        assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event,'AddedAdmin', 'should be the "AddedAdmin" event');
        assert.equal(receipt.logs[0].args.target, accounts[5], 'the added admin account is wrong');
        assert.equal(receipt.logs[0].args.level, 5, 'the level of admin is wrong');
        return tokenInstance.isAdmin(accounts[5], 5);
      }).then(admin => {
        assert.equal(admin, true, 'Account is not admin');
      }) 
    })
    
    
    it ('test removeAdmin ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.removeAdmin(accounts[0],  {
          from : accounts[0]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the target is the token owner');    
        return tokenInstance.removeAdmin(accounts[5],  {
            from : accounts[0]});
      }).then(receipt => {
        assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event,'RemovedAdmin', 'should be the "RemovedAdmin" event');
        assert.equal(receipt.logs[0].args.target, accounts[5], 'accounts[5] is again an admin ');
        return tokenInstance.isAdmin(accounts[5], 5);
      }).then(notAdmin => {
        assert.equal(notAdmin, false, 'Account is admin');
      }) 
    })
  
  

  
  
});

