var EIP20 = artifacts.require("./EIP20.sol");

module.exports = (deployer, _, accounts) => {
  deployer.deploy(EIP20, 100000, 'E At W', 2, 'E@W');
};
