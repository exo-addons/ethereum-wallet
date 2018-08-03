var Migrations = artifacts.require("./Migrations.sol");

module.exports = function(deployer, _, accounts) {
  deployer.deploy(Migrations, { gas: 300000 });
};
