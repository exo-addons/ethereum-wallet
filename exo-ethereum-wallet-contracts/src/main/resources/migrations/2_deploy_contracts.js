var ERTToken = artifacts.require("./ERTToken.sol");


module.exports =  function(deployer) {
 deployer.deploy(ERTToken, 100000 * Math.pow(10, 18), "Curries", 18, "C");
};









 