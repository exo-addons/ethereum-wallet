var ERTToken = artifacts.require("ERTToken");

contract('Pausablee', function(accounts) {

  it('test pause ', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.pause({
        from : accounts[0]
      });
    }).then(
        function(receipt) {
          assert.equal(receipt.logs.length, 1,
              'number of emitted event is wrong');
          assert.equal(receipt.logs[0].event, 'Pause',
              'should be the "Pause" event');

        })
  });

  it('test unpause ', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.unPause();
    }).then(
        function(receipt) {
          assert.equal(receipt.logs.length, 1,
              'number of emitted event is wrong');
          assert.equal(receipt.logs[0].event, 'UnPause',
              'should be the "UnPause" event');

        })
  });

});
