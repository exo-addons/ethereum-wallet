var ERTToken = artifacts.require("./ERTToken.sol");


module.exports =  function(deployer) {
 deployer.deploy(ERTToken, 100000, "Curries", 18, "C");
};









 