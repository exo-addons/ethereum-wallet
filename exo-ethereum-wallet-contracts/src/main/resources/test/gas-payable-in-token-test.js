var ERTToken = artifacts.require("ERTToken");

const decimals = Math.pow(10, 18);

function deleteTokenDecimals(number) {
  // A workaround for big numbers that exceeds Number.MAX_SAFE_INTEGER (2 ^ 53 - 1)
  return (number / 10000000000) / (decimals / 10000000000);
}

function addTokenDecimals(number) {
  return number * decimals;
}

contract('FundCollection', function(accounts) {
  let initialOwnerEtherBalance = 0;
  let initialOwnerTokenBalance = 0;

  let initialSenderEtherBalance = 0;
  let initialSenderTokenBalance = 0;

  let ownerUsedGas = 0;
  let senderUsedGas = 0;
  let gasPrice = 0;

  let tokenPriceInGas = 0;

  const tokensToTransferFromOwner = addTokenDecimals(1000);
  const allowedEtherDelta = web3.toWei(0.0001, 'ether');

  it('Pay token transfer gas in tokens', function() {
    return ERTToken.deployed().then(function(instance) {
      tokenInstance = instance;
    }).then(() => {
      // Check accounts[1] to be able to send him tokens
      return tokenInstance.tokenPriceInGas();
    }).then(tokenPrice => {
      tokenPriceInGas = tokenPrice;
    }).then(() => {
      // Check accounts[1] to be able to send him tokens
      return tokenInstance.approveAccount(accounts[1], {from: accounts[0]});
    }).then(() => {
      // Check accounts[2] to be able to send him tokens
      return tokenInstance.approveAccount(accounts[2], {from: accounts[0]});
    }).then(() => {
      // Check contract balance is 0
      return web3.eth.getBalance(tokenInstance.address);
    }).then(balance => {
      assert.equal(balance, 0, 'Contract balance should be empty');
    }).then(() => {
      // Send 1 ether to contract to be able to refund users
      return web3.eth.sendTransaction({
        from : accounts[0],
        to: tokenInstance.address,
        value : web3.toWei(1,"ether")
      });
    }).then(() => {
      // Check contract balance is received by contract and not miner
      return web3.eth.getBalance(tokenInstance.address);
    }).then(balance => {
      assert.equal(String(balance), String(web3.toWei(1,"ether")), 'Contract balance should be 1 ether');
    }).then(() => {
      // Begin transfer and refund methods test for owner
      return web3.eth.getBalance(accounts[0]);
    }).then(balance => {
      initialOwnerEtherBalance = balance.toNumber();
    }).then(() => {
      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      initialOwnerTokenBalance = deleteTokenDecimals(balance.toNumber());
    }).then(() => {
      return tokenInstance.transfer(accounts[1], tokensToTransferFromOwner, {from: accounts[0]});
    }).then(receipt => {
      assert.equal(receipt && receipt.receipt && receipt.receipt.status, true, "Transaction failure");
      ownerUsedGas = receipt.receipt.gasUsed;
      gasPrice = receipt.receipt.gasPrice;
      // TODO test with gasUsed + gasPrice + tokenExchangeRate
    }).then(() => {
      return web3.eth.getBalance(accounts[0]);
    }).then(balance => {
      balance = balance.toNumber();
      const etherBalanceDiff = initialOwnerEtherBalance - balance;
      assert.equal(etherBalanceDiff < allowedEtherDelta, true, `ether balance shouldn't change a lot for sender even owner, diff: ${etherBalanceDiff}, usedGas: ${ownerUsedGas}`);
      assert.equal(etherBalanceDiff >= 0, true, `shouldn't add ether to the balance of sender, diff: ${etherBalanceDiff}, usedGas: ${ownerUsedGas}`);
    }).then(() => {
      return tokenInstance.balanceOf(accounts[0]);
    }).then(balance => {
      balance = deleteTokenDecimals(balance.toNumber());
      assert.equal(initialOwnerTokenBalance - balance,
          deleteTokenDecimals(tokensToTransferFromOwner),
          "token balance shouldn't change when sender is owner");
      // End transfer and refund methods test for owner
    }).then(() => {
      // Begin transfer and refund methods test for regular user
      return web3.eth.getBalance(accounts[1]);
    }).then(balance => {
      initialSenderEtherBalance = balance.toNumber();
    }).then(() => {
      return tokenInstance.balanceOf(accounts[1]);
    }).then(balance => {
      balance = balance.toNumber();
      assert.equal(balance,
          tokensToTransferFromOwner,
          `accounts[1] should have received ${tokensToTransferFromOwner} tokens`);
      initialSenderTokenBalance = deleteTokenDecimals(balance);
    }).then(() => {
      return tokenInstance.transfer(accounts[2], addTokenDecimals(100), {from: accounts[1]});
    }).then(receipt => {
      assert.equal(receipt && receipt.receipt && receipt.receipt.status, true, "Transaction failure");
      senderUsedGas = receipt.receipt.gasUsed;
      gasPrice = receipt.receipt.gasPrice;
      // TODO test with gasUsed + gasPrice + tokenExchangeRate
    }).then(() => {
      return web3.eth.getBalance(accounts[1]);
    }).then(balance => {
      balance = balance.toNumber();
      const etherBalanceDiff = initialSenderEtherBalance - balance;
      assert.equal(etherBalanceDiff < allowedEtherDelta, true, `ether balance shouldn't change a lot for sender, diff: ${etherBalanceDiff}, usedGas: ${senderUsedGas}`);
      assert.equal(etherBalanceDiff >= 0, true, `shouldn't add ether to the balance of sender, diff: ${etherBalanceDiff}, usedGas: ${senderUsedGas}`);
    }).then(() => {
      return tokenInstance.balanceOf(accounts[1]);
    }).then(balance => {
      balance = deleteTokenDecimals(balance.toNumber());
      assert.equal(parseInt(initialSenderTokenBalance - balance), 100, "sender should have paid transaction fee in token");
    }).then(() => {
      return web3.eth.getBalance(tokenInstance.address);
    });
  });

  // TODO test failure test cases
});
