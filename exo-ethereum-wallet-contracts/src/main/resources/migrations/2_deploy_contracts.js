var ERTToken = artifacts.require("./ERTToken.sol");
//var ERTTokenData = artifacts.require("./ERTTokenData.sol");

var ERTTokenV1 = artifacts.require("./ERTTokenV1.sol");
var ERTTokenDataV1 = artifacts.require("./ERTTokenDataV1.sol");

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
     .then(() => ERTTokenV1.deployed()) // Wait until ERTTokenV1 is deployed
     .then(ertTokenV1Instance => ertTokenV1Instance.initialize(ERTToken.address, 100000 * Math.pow(10, 18), "Curries", 18, "C"))
     // Change gas price limit to 100 Gwei because it's the the default gas price of truffle test network
     .then(() => ERTTokenV1.deployed()) // Wait until ERTTokenV1 is deployed
     .then(ertTokenV1Instance => ertTokenV1Instance.setGasPriceLimit(Number(web3.toWei("100", "Gwei"))))
     // Change ABI of Proxy by ABI of Token to access it methods
     .then(() => ERTToken.abi = ERTTokenV1.abi);
};