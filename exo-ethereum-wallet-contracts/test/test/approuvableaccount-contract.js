const ERTToken = artifacts.require("ERTToken");


  
  contract('ApprouvableAccount', function(accounts) {
    let tokenInstance;
    
    
    it ('test approveAccount ' , function(){
      return ERTToken.deployed().then(function(instance){
        tokenInstance = instance;
        return tokenInstance.approveAccount(accounts[7],  {
            from : accounts[0]});
      }).then(receipt => {
        //console.log(receipt);
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
  
  

  
  
});



