const path = require('path');
const merge = require('webpack-merge');
const apiMocker = require('connect-api-mocker');

const webpackCommonConfig = require('./webpack.prod.js');

module.exports = merge(webpackCommonConfig, {
  output: {
    path: '/exo-server/webapps/exo-ethereum-wallet/',
    filename: 'js/[name].bundle.js'
  }
});