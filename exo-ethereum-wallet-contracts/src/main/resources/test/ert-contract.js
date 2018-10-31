var ERTToken = artifacts.require("ERTToken");

const decimals = Math.pow(10, 18);

contract('ERTToken', function(accounts) {

  let tokenInstance;

  it('test contract initialization attributes', function() {
    return ERTToken.deployed().then(instance => {
      tokenInstance = instance;
      return tokenInstance.totalSupply();
    }).then(function(totalSupply) {
      assert.equal(totalSupply, 100000 * decimals, 'has not the correct totalSupply');
      return tokenInstance.name();
    }).then(function(name) {
      assert.equal(name, 'Curries', 'has not the correct name');
      return tokenInstance.symbol();
    }).then(function(symbol) {
      assert.equal(symbol, 'C', 'has not the correct symbol');
      return tokenInstance.decimals();
    }).then(function(decimals) {
      assert.equal(decimals, 18, 'has not the correct decimals');
      return tokenInstance.approvedAccount(accounts[0]);
    }).then(function(ownerApproved) {
      assert.equal(true, ownerApproved, 'Contract owner has to be approved');
    });
  })

  it('put 100000 * 10 ^ 18 ERTToken in the admin account', function() {
    return ERTToken.deployed().then(function(instance) {
      return instance.balanceOf(accounts[0]);
    }).then(
        function(adminBalance) {
          assert.equal(adminBalance.valueOf(), 100000 * decimals,
              "100000 * 10 ^ 18 wasn't in the admin account");
        });
  });

  it('transfer tokens', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      // Test transferring something larger than the sender's balance
      // balances[_from] >= _value
      return tokenInstance.transfer(accounts[1], 100001 * decimals);
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no transfer with exceeded tokens is allowed');

      // Test error require msg.sender != _to
      return tokenInstance.transfer(accounts[0], 99 * decimals, {from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no self transfer is allowed');  
     
      // Test error require value > 0
      return tokenInstance.transfer(accounts[5], -5 * decimals);
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no negative tokens transfer should be allowed');

      // Test error require to != 0x0
      return tokenInstance.transfer(0x0, 7 * decimals, {from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no transfer to 0x address is allowed');

      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      balance = balance.toNumber();
      assert.equal(balance, 100000 * decimals, 'Wrong balance of contract owner');

      return tokenInstance.transfer.call(accounts[1], 6 * decimals, {
        from : accounts[0]
      });
    }).then(success => {
      assert.equal(success, true, 'Transaction failed');
      return tokenInstance.transfer(accounts[1], 10 * decimals, {
        from : accounts[0]
      });
    }).then(receipt => {
      assert(receipt.logs.length >= 2,
          'number of emitted event is wrong');
      assert.equal(receipt.logs[0].event, 'ApprovedAccount',
          'should be the "AppovedAccount" event');
      assert.equal(receipt.logs[1].event, 'Transfer',
          'should be the "Transfer" event');
      assert.equal(receipt.logs[1].args._from, accounts[0],
          'the account the tokens are transferred from is wrong');
      assert.equal(receipt.logs[1].args._to, accounts[1],
          'the account the tokens are transferred to is wrong');
      assert.equal(receipt.logs[1].args._value, 10 * decimals,
          'the transfer amount is wrong');
      return tokenInstance.balanceOf(accounts[1]);
    }).then(balance => {
      assert.equal(balance.toNumber(), 10 * decimals,
          'token balance of receiver is wrong');
      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      assert.equal(balance.toNumber(), (100000 - 10) * decimals,
          'token balance of sender is wrong');
    });
  });

  it('approves tokens for delegated transfer', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
      
      // Test approving amount larger than balance balances[msg.sender] >= _value
      return tokenInstance.approve(accounts[1], 11111111111111 * decimals, {from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'error1 prime message must contain revert');  

      // Test require msg.sender != _spender
      return tokenInstance.approve(accounts[0], 11 * decimals, {from : accounts[0]});
    }).then(assert.fail).catch(function(error) {
      assert(error.message.indexOf('revert') >= 0, 'error2 prime message must contain revert');

      return tokenInstance.approve.call(accounts[1], 5 * decimals);
    }).then(function(success) {
      assert.equal(success, true, 'transaction failed');
      return tokenInstance.approve(accounts[2], 5 * decimals, {
        from : accounts[0]
      });
    }).then(receipt => {
      assert.equal(receipt.logs.length, 2,
        'number of emitted event is wrong');
      assert.equal(receipt.logs[0].event, 'ApprovedAccount',
        'should be the "ApprovedAccount" event');
      assert.equal(receipt.logs[1].event, 'Approval',
        'should be the "Approval" event');
      assert.equal(receipt.logs[1].args._owner, accounts[0],
        'the account the tokens are authorized by is wrong');
      assert.equal(receipt.logs[1].args._spender, accounts[2],
        'the account the tokens are authorized to is wrong');
      assert.equal(receipt.logs[1].args._value, 5 * decimals,
        'the transfer amount is wrong');
      return tokenInstance.allowance(accounts[0], accounts[2]);
    }).then(allowed => {
      assert.equal(allowed.toNumber(), 5 * decimals,
        'stores the allowance for delegated trasnfer');
    });
  });

  let fromAccount = accounts[3], toAccount = accounts[4], spendingAccount = accounts[5];
  let cannotTransferAgain = false;
  it('handles delegated token transfers', function() {
    return ERTToken.deployed().then(instance => {
      tokenInstance = instance;
      return tokenInstance.transfer(fromAccount, 100 * decimals, {
        from : accounts[0]
      });
    }).then(function(receipt) {
      // Reciever address should be approved first to be able to receive Tokens
      return tokenInstance.approveAccount(spendingAccount, {
        from : accounts[0]
      });
    }).then(function(receipt) {
      return tokenInstance.approve(spendingAccount, 10 * decimals, {
        from : fromAccount
      });
    }).then(function(receipt) {
      // We have to approve 'toAccount' first before sending him some tokens
      return tokenInstance.approveAccount(toAccount, {
        from : accounts[0]
      });
    }).then(function(receipt) {
      return tokenInstance.transferFrom(fromAccount, toAccount, 10 * decimals, {
        from : spendingAccount
      });
    }).then(function(success) {
      return tokenInstance.transferFrom(fromAccount, toAccount, 1 * decimals, {
        from : spendingAccount
      });
    }).catch(error => {
      cannotTransferAgain = true;
    }).then(receipt => {
      assert.equal(true, cannotTransferAgain, "Shouldn't be able to transfer again");
      return tokenInstance.balanceOf(fromAccount);
    }).then(balance => {
      assert.equal(balance.toNumber(), 90 * decimals,
        'deducts the amount from the sending account');
      return tokenInstance.balanceOf(spendingAccount);
    }).then(balance => {
      assert.equal(balance.toNumber(), 0,
        'spending account shouldn\'t have recieved tokens tokens');
      return tokenInstance.balanceOf(toAccount);
    }).then(balance => {
      assert.equal(balance.toNumber(), 10 * decimals,
        'adds the amount from the receiving account');
      return tokenInstance.allowance(fromAccount, spendingAccount);
    }).then(allowance => {
      assert.equal(allowance.toNumber(), 0,
        'deducts the amount from the allowance');
    });
  });
});
