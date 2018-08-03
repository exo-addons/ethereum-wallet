var EATWToken = artifacts.require("./EATWToken.sol");

module.exports = (deployer, _, accounts) => {
  deployer.deploy(EATWToken, 500000, { gas: 300000 });
};
