var ERTToken = artifacts.require("ERTToken");

contract('Mintable', function(accounts) {
  
  
  let tokenInstance;
  
  
  it("should minted token ", function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.balanceOf(accounts[0]);
    }).then(
        function(adminebalance) {
          assert.equal(adminebalance.valueOf(), 100000,
              "the ammount is wrong it shouled be 100000");
          
          return tokenInstance.balanceOf(accounts[1]);
        }).then(
            function(balance) {
              assert.equal(balance.valueOf(), 0,
                  "it shouled be 0");
              
              
              return tokenInstance.mintToken(accounts[1], 50, {from : accounts[0]});
            }).then(
                function(result) {
                  assert.equal(result.logs.length, 2,
                  'number of emitted event is wrong');
                  assert.equal(result.logs[0].event, 'Transfer',
                  'should be the "Transfer" event');
                  assert.equal(result.logs[0].args._from, 0,
                  'the account the tokens are transferred from is wrong');
                  assert.equal(result.logs[0].args._to, accounts[0],
                  'the account the tokens are transferred to is wrong');
                  assert.equal(result.logs[0].args._value, 50,
                  'the transfer amount is wrong');
                  assert.equal(result.logs[1].event, 'Transfer',
                  'should be the "Transfer" event');
                  assert.equal(result.logs[1].args._from, accounts[0],
                  'the account the tokens are transferred from is wrong');
                  assert.equal(result.logs[1].args._to, accounts[1],
                  'the account the tokens are transferred to is wrong');
                  assert.equal(result.logs[1].args._value, 50,
                  'the transfer amount is wrong');
           
                  
                  return tokenInstance.balanceOf(accounts[0]);
                }).then(
                    function(adminebalance) {
                      assert.equal(adminebalance.valueOf(), 100000,
                          "the ammount is wrong it shouled be 100000");
                      
                      return tokenInstance.balanceOf(accounts[1]);
                    }).then(
                        function(balance) {
                          assert.equal(balance.valueOf(), 50,
                              "it shouled be 50");
                          
                         
                            return tokenInstance.totalSupply();
                          }).then(function(totalSupply) {
                            assert.equal(totalSupply, 100050, 'has not the correct totalSupply');  
              
       
        });
  });

  
});
