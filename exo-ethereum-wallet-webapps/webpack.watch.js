const path = require('path');
const merge = require('webpack-merge');
const apiMocker = require('connect-api-mocker');

const webpackProductionConfig = require('./webpack.prod.js');

module.exports = merge(webpackProductionConfig, {
  output: {
    path: '/exo-server/webapps/exo-ethereum-wallet/',
    filename: 'js/[name].bundle.js'
  }
});