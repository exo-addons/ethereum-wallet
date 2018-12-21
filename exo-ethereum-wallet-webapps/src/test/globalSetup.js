module.exports = () => {
  require('./constants.js');
  const ganache = require('ganache-cli');
  global.server = ganache.server({
    network_id: 4452365,
    gasPrice: '0x77359400', // 2 GWei
    gasLimit: '0x989680', // 10 000 000
    accounts: global.WALLET_ACCOUNTS, // Accounts to initialize
  });
  global.server.listen(8545);
};
