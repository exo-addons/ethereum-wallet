
//beforeAll(() => {
//  if (!global.server) {
//    global.server = global.TestRPC.server();
//    global.server.listen(7545);
//    //console.log("--------------- server", global.server);
//  }
//});
//
//afterAll(() => {
//  if (global.server) {
//    global.server.close();    
//  }
//});


it ('test getaccounts', () => {
  return window.testWeb3.eth.getAccounts().then(accounts => {
    console.log("******** accounts ***********", accounts);
  });
});



//it ('test getaccounts1', () => {
//  return window.testWeb3.eth.accounts[0].then(acc => {
//  console.log("***********************************************************************",acc )
//  });
//});