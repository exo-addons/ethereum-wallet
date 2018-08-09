const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const config = merge(webpackCommonConfig, {
  mode: 'production',
  module: {
    rules: [
      {
        test: /.(ttf|otf|eot|svg|woff(2)?)(\?[a-z0-9]+)?$/,
        use: {
          loader: "file-loader",
          options: {
            name: "/exo-ethereum-wallet/fonts/[name].[ext]",
            emitFile: false
          }
        }
      }
    ]
  },
  entry: {
    wallet: './src/main/webapp/vue-app/wallet.js',
    spaceWallet: './src/main/webapp/vue-app/spaceWallet.js',
    walletAdmin: './src/main/webapp/vue-app/walletAdmin.js'
  },
  output: {
    path: path.join(__dirname, 'target/exo-ethereum-wallet/'),
    filename: 'js/[name].bundle.js'
  },
  externals: {
    vue: 'Vue',
    jquery: '$',
    vuetify: 'Vuetify',
    web3: 'Web3',
    TruffleContract: 'TruffleContract'
  }
});

module.exports = config;