module.exports = config => {
  const ganache = require("ganache-cli");
  global.server = ganache.server();
  global.server.listen(8545);
};
