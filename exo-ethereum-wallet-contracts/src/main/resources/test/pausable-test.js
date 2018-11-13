var ERTToken = artifacts.require("ERTToken");

contract('Pausable', function(accounts) {

  it('test pause', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.isPaused();
      }).then(paused => {
        assert.equal(paused, false,
            'Contract seems to be unexpectedly paused');
        return tokenInstance.pause({
          from : accounts[0]
        });
      }).then(receipt => {
        assert.equal(receipt.logs.length, 1,
            'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event, 'ContractPaused',
            'should be the "ContractPaused" event');
        return tokenInstance.isPaused();
      }).then(paused => {
        assert.equal(paused, true,
            'Contract seems to be unexpectedly unPaused after using pause() method');
      });
  });

  it('test unPause ', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      return tokenInstance.isPaused();
    }).then(paused => {
      assert.equal(paused, true,
          'Contract seems to be unexpectedly unpaused');
      return tokenInstance.unPause();
    }).then(receipt => {
          assert.equal(receipt.logs.length, 1,
              'number of emitted event is wrong');
          assert.equal(receipt.logs[0].event, 'ContractUnPaused',
              'should be the "ContractUnPaused" event');

        return tokenInstance.isPaused();
    }).then(paused => {
      assert.equal(paused, false,
          'Contract seems to be unexpectedly paused after using unPause() method');
    });
  });
});
