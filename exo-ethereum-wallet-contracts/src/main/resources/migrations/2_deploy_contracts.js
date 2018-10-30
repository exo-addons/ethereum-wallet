var ERTToken = artifacts.require("./ERTToken.sol");
var ERTTokenData = artifacts.require("./ERTTokenData.sol");

var ERTTokenV1 = artifacts.require("./ERTTokenV1.sol");
var ERTTokenDataV1 = artifacts.require("./ERTTokenDataV1.sol");

module.exports =  function(deployer) {
  return deployer.deploy(ERTTokenDataV1)
   .then(() => deployer.deploy(ERTTokenData, ERTTokenDataV1.address))
   .then(() => deployer.deploy(ERTTokenV1, ERTTokenData.address, 100000 * Math.pow(10, 18), "Curries", 18, "C"))
   .then(() => deployer.deploy(ERTToken, ERTTokenV1.address))
   .then(() => ERTToken.abi = ERTTokenV1.abi);
};