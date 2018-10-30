var ERTToken = artifacts.require("./ERTToken.sol");
var ERTTokenProxy = artifacts.require("./ERTTokenProxy.sol");

module.exports =  function(deployer) {
 deployer.deploy(ERTToken, 100000 * Math.pow(10, 18), "Curries", 18, "C");
 deployer.deploy(ERTTokenProxy);
};