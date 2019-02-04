const path = require('path');
const merge = require('webpack-merge');
const apiMocker = require('connect-api-mocker');

const webpackCommonConfig = require('./webpack.common.js');

module.exports = merge(webpackCommonConfig, {
  module: {
    rules: [
      {
        test: /.(ttf|otf|eot|svg|woff(2)?)(\?[a-z0-9]+)?$/,
        use: {
          loader: "file-loader",
          options: {
            name: "[name].[ext]",
            context: 'css',
            outputPath: "fonts/"
          }
        }
      }
    ]
  },
  entry: {
    wallet: './src/main/webapp/vue-app/wallet-dev.js',
    walletAPI: './src/main/webapp/vue-app/walletAPI.js',
    spaceWallet: './src/main/webapp/vue-app/spaceWallet-dev.js',
    walletAdmin: './src/main/webapp/vue-app/walletAdmin-dev.js',
    rewardApp: './src/main/webapp/vue-app/rewardApp.js'
  },
  output: {
    path: '/exo-server/webapps/exo-ethereum-wallet/',
    filename: 'js/[name].bundle.js'
  },
  devServer: {
    contentBase: path.join(__dirname, 'src/main/webapp'),
    before: function(app) {
      app.use('/portal/rest', apiMocker({
        target: 'src/main/webapp/js/mock',
        nextOnNotFound: true
      }));
    },
    port: 3000
  },
  devtool: 'inline-source-map'
});