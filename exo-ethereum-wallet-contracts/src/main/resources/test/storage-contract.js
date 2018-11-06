const ERTToken = artifacts.require("ERTToken");

var TestERTTokenV2 = artifacts.require("TestERTTokenV2");
var TestERTTokenDataV2 = artifacts.require("TestERTTokenDataV2");
var ERTTokenDataV1 = artifacts.require("ERTTokenDataV1");
  
  
contract('TokenStorage', function(accounts) {
    let tokenInstance;
    

    it('when setting a data address', function() {
      return ERTToken.deployed().then(instance => {
          tokenInstance = instance; 
          
          return tokenInstance.setDataAddress(2, TestERTTokenDataV2.address, {from : accounts[5]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: msg.sender should be the owner or the proxy');
          
          return tokenInstance.setDataAddress(2, TestERTTokenDataV2.address);
            }).then(function(result1) {
              return tokenInstance.getDataAddress(2);
            }).then(function(dataAddress) {
              assert.equal(dataAddress, TestERTTokenDataV2.address, 'Data address of version 2 is wrong');
              
              return tokenInstance.getDataAddress(1);
            }).then(function(dataAddress) {
              assert.equal(dataAddress, ERTTokenDataV1.address, 'Data address of version 1 is wrong');
              
              return tokenInstance.getDataAddress(5);
            }).then(function(dataAddress) {
              assert.equal(dataAddress, 0x0, 'should address != 0x because data address not yet set');
               
            })
         }) 
    
    
    
    
    
    
});

  
  
