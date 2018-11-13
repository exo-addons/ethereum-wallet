const ERTToken = artifacts.require("ERTToken");
  
  contract('ApprouvableAccount', function(accounts) {
    let tokenInstance;
    
    
    it ('test approveAccount ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.approveAccount(accounts[4],  {
          from : accounts[1]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the sender is not the owner');  
        
        return tokenInstance.approveAccount(accounts[7],  {
            from : accounts[0]
        });
      }).then(receipt => {
        assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event,'ApprovedAccount', 'should be the "AppovedAccount" event');
        assert.equal(receipt.logs[0].args.target, accounts[7], 'the approved account is wrong');
        return tokenInstance.isApprovedAccount(accounts[7]);
      }).then(approved => {
        assert.equal(approved, true, 'Account is not approved');
      }) 
    })
 
    
    
    it ('test disapproveAccount ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.disapproveAccount(accounts[0],  {
          from : accounts[0]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: when the target is the token owner');  
        return tokenInstance.disapproveAccount(accounts[7],  {
            from : accounts[0]});
      }).then(receipt => {
        assert.equal(receipt.logs.length, 1, 'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event,'DisapprovedAccount', 'should be the "DisappovedAccount" event');
        assert.equal(receipt.logs[0].args.target, accounts[7], 'the disapproved account is wrong');
        return tokenInstance.isApprovedAccount(accounts[7]);
      }).then(approved => {
        assert.equal(approved, false, 'Account is not approved');
      }) 
    })
  
    
    
    it ('test approveAccount by admin level 5 when the onlyAdmin(1) ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.addAdmin(accounts[1], 5, {from : accounts[0]});
      }).then(receipt => {
        return tokenInstance.approveAccount(accounts[7], {from : accounts[1]});
      }).then(receipt => {
        return tokenInstance.isApprovedAccount(accounts[7]);
      }).then(approved => {
        assert.equal(approved, true, 'Account is not approved by admin with level 5');
      }) 
    })
 
    
    
    
  
  
});



