module.exports = () => {
  require('./constants.js');
  const ganache = require('ganache-cli');
  global.server = ganache.server({
    network_id: 4452365,
    gasPrice: '0x77359400', // 2 GWei
    gasLimit: '0x989680', // 10 000 000
    accounts: global.WALLET_ACCOUNTS,
  });
  global.adminAccount = '0x2d232d448FB0B5b370D3abAD2681399e2002aE2A';
  global.server.listen(8545);
};
