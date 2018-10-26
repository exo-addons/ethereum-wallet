var ERTToken = artifacts.require("./ERTToken.sol");
var OwnedUpgradeabilityProxy = artifacts.require("./OwnedUpgradeabilityProxy.sol");

module.exports =  function(deployer) {
 deployer.deploy(ERTToken, 100000 * Math.pow(10, 18), "Curries", 18, "C");
 deployer.deploy(OwnedUpgradeabilityProxy);


};









 