// TODO: Wait for https://github.com/facebook/jest/issues/5164 to delete 'runInBand' option for jest
// and add contract deployment here with ES6

module.exports = () => {
  const fs = require('fs');
  try {
    // File used to cache Token contract address between tests
    if (fs.existsSync('target/contractAddress.txt')) {
      fs.unlinkSync('target/contractAddress.txt');
    }
    // File used to cache Token contract ABI between tests
    if (fs.existsSync('target/contractAbi')) {
      fs.unlinkSync('target/contractAbi');
    }
    // File used to cache Token contract BIN between tests
    if (fs.existsSync('target/contractBin')) {
      fs.unlinkSync('target/contractBin');
    }
    // Blockchain db directory
    if (fs.existsSync('target/ganache-data')) {
      const removeFolderRecursive = (dirPath) => {
        if (fs.existsSync(dirPath)) {
          fs.readdirSync(dirPath).forEach((filePath, index) => {
            filePath = `${dirPath}/${filePath}`;
            if (fs.lstatSync(filePath).isDirectory()) {
              removeFolderRecursive(filePath);
            } else {
              fs.unlinkSync(filePath);
            }
          });
          fs.rmdirSync(dirPath);
        }
      };
      removeFolderRecursive('target/ganache-data');
    }
    if (!fs.existsSync('target/ganache-data')) {
      fs.mkdirSync('target/ganache-data');
    }
  } catch (e) {
    console.error('Error deleting files', e);
  }

  require('./constants.js');
  const ganache = require('ganache-cli');
  try {
    global.server = ganache.server({
      host: 'localhost',
      network_id: 4452364,
      debug: true,
      gasPrice: '0x77359400', // 2 GWei
      gasLimit: '0x989680', // 10 000 000
      accounts: global.WALLET_ACCOUNTS, // Accounts to initialize
      db_path: 'target/ganache-data',
    });
    global.server.listen(8545, (err) => {
      if (err) {
        console.error(err);
      }
    });
  } catch (e) {
    console.error('Error initializing ganache server', e);
    throw e;
  }
};
