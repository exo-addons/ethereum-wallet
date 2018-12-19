module.exports = (config) => {
  if (global.server) {
    global.server.close();
  }
};
