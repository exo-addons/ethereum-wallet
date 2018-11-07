var ERTToken = artifacts.require("./ERTToken.sol");
var TestERTToken = artifacts.require("../test/TestERTToken.sol");
var ERTTokenV1 = artifacts.require("./ERTTokenV1.sol");
var ERTTokenDataV1 = artifacts.require("./ERTTokenDataV1.sol");
var TestERTTokenV2 = artifacts.require("./test/TestERTTokenV2.sol");
var TestERTTokenDataV2 = artifacts.require("./test/TestERTTokenDataV2.sol");

module.exports =  function(deployer) {
  // Deployment of tokens starting by ERTTokenDataV1: ERC20 Token data
  return deployer.deploy(ERTTokenDataV1)
     // Deployment of ERTTokenV1: ERC20 Token implementation (with proxy address = 0x because it's not yet deployed)
     .then(() => deployer.deploy(ERTTokenV1, ERTTokenDataV1.address, 0))
     // Deployment of ERTToken: proxy contract
     .then(() => deployer.deploy(ERTToken, ERTTokenV1.address, ERTTokenDataV1.address))
     // transfer ownership of ERTTokenDataV1 to ERTToken and ERTTokenV1 to be able to make changes on data
     .then(() => ERTTokenDataV1.deployed())
     .then(ertTokenDataV1Instance => ertTokenDataV1Instance.transferDataOwnership(ERTToken.address, ERTTokenV1.address))
     // Initialize Token Data with initial values
     .then(() => ERTTokenV1.deployed())
     .then(ertTokenV1Instance => ertTokenV1Instance.initialize(ERTToken.address, 100000 * Math.pow(10, 18), "Curries", 18, "C"))
     // Change ABI of Proxy by ABI of Token to access it methods
     .then(() => ERTToken.abi = TestERTToken.abi)
    //deployment of TestERTTokenDataV2 
    .then(() => deployer.deploy(TestERTTokenDataV2))
    //Deployment of TestERTTokenV2 (with proxy address)
    .then(() => deployer.deploy(TestERTTokenV2, ERTTokenDataV1.address ,TestERTTokenDataV2.address, ERTToken.address));
};