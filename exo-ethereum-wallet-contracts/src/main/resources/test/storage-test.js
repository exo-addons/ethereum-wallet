var ERTToken = artifacts.require("ERTToken");
var TestERTTokenV2 = artifacts.require("TestERTTokenV2");
var TestERTTokenDataV2 = artifacts.require("TestERTTokenDataV2");
var ERTTokenDataV1 = artifacts.require("ERTTokenDataV1");
var ERTTokenV1 = artifacts.require("ERTTokenV1");
  
  
contract('TokenStorage', function(accounts) {
    let tokenInstance;
    
    it('initialized token storage', function() {
      return ERTToken.deployed().then(instance => {
          tokenInstance = instance;
           return tokenInstance.implementationAddress.call();
          }).then(function(implementation) {
            assert.equal(implementation, ERTTokenV1.address, 'should return the current implementation');    
            return tokenInstance.owner.call();
          }).then(function(owner) {
            assert.equal(owner, accounts[0], 'should return the owner address of the contract');  
            return tokenInstance.proxy.call();
          }).then(function(proxy) {
            assert.equal(proxy, ERTToken.address, 'should return the proxy address'); 
            return tokenInstance.version.call();
          }).then(function(version) {
            assert.equal(version, 1 , 'should return the version of the iplementation'); 
          });
    })
});

  
  
