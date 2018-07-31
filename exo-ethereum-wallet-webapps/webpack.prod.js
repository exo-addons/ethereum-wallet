const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  entry: {
    wallet: './src/main/webapp/vue-app/main.js'
  },
  output: {
    path: path.join(__dirname, 'target/exo-ethereum-wallet/'),
    filename: 'js/[name].bundle.js'
  },
  externals: {
    vue: 'Vue',
    jquery: 'jQuery',
    vuetify: 'Vuetify',
    web3: 'Web3',
    TruffleContract: 'TruffleContract'
  }
});

module.exports = config;