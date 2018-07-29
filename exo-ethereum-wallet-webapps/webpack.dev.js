const path = require('path');
const merge = require('webpack-merge');
const apiMocker = require('connect-api-mocker');

const webpackCommonConfig = require('./webpack.common.js');

// change the server path to your server location path
const exoServerPath = '';

module.exports = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, `${exoServerPath}webapps/exo-ethereum-wallet/`),
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