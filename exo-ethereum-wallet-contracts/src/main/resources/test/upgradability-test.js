var ERTToken = artifacts.require("ERTToken");
var TestERTTokenV2 = artifacts.require("TestERTTokenV2");
var ERTTokenV1 = artifacts.require("ERTTokenV1");
var ERTTokenDataV1 = artifacts.require("ERTTokenDataV1");
var TestERTTokenDataV2 = artifacts.require("TestERTTokenDataV2");
var TestERTTokenV3 = artifacts.require("TestERTTokenV3");

const decimals = Math.pow(10, 18);

contract('Upgradability', function(accounts) {

  let tokenInstance;
  let tokenDataV1Instance;
  let tokenDataV2Instance;

  it('Test ownership of V2 contract', function() {
   return TestERTTokenV2.deployed()
     .then(instance => {
       tokenInstance = instance;
       return tokenInstance.owner.call();
     }).then(function(result) {
       assert.equal(result, accounts[0] , 'the owner of TestERTTokenV2 is wrong'); 
     });
  })

  it('Test ownership of V2 Data contract', function() {
    return TestERTTokenDataV2.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.implementation.call();
      }).then(function(result) {
        assert.equal(result, TestERTTokenV2.address , 'the implementation of TestERTTokenDataV2 is wrong'); 
        return tokenInstance.proxy.call();
      }).then(function(result) {
        assert.equal(result, ERTToken.address , 'the proxy of TestERTTokenDataV2 is wrong'); 
      });
  })

  it('Test ownership of V3 contract', function() {
    return TestERTTokenV3.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.owner.call();
      }).then(function(result) {
        assert.equal(result, accounts[0] , 'the owner of TestERTTokenV3 is wrong'); 
      });
  })

  const fiveWei = web3.toWei(5, 'ether').toString();

  it('Send ether to Proxy', () => {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return web3.eth.getBalance(ERTToken.address)
      })
      .then(result => {
        initialProxyBalance = Number(result.toString());
        return web3.eth.sendTransaction({
          from : accounts[0],
          to: ERTToken.address,
          value : fiveWei
        });
      })
      .then(result => web3.eth.getBalance(ERTToken.address))
      .then(result => 
        assert.equal(Number(String(result)), initialProxyBalance + Number(fiveWei), 'the balance of ERTToken is wrong ')
      );
  });

  it('Upgrade implementation to V2 and add data contract V2', () => {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.upgradeImplementation(ERTToken.address, 1, TestERTTokenV2.address);
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, "Version 1 shouldn't be accepted. It should be greater than previous one to be accepted");
        return tokenInstance.upgradeImplementation(ERTToken.address, 2, ERTTokenV1.address);
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, "New implementation should be different from old one");
        return tokenInstance.upgradeImplementation(ERTToken.address, 2, TestERTTokenV2.address, {from: accounts[5]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, "Only owner can upgrade implementation");
        return tokenInstance.implementationAddress.call();
      }).then(function(implementation) {
        assert.equal(implementation, ERTTokenV1.address, 'Current V1 implementation seems to be wrong');
        return tokenInstance.upgradeDataAndImplementation(ERTToken.address, 2, TestERTTokenV2.address, 2, TestERTTokenDataV2.address);
      }).then(function(receipt) {
        const upgradedEvent = receipt.logs.find(log => log && log.event && log.event === 'Upgraded');
        assert.isDefined(upgradedEvent ,'Upgraded event should be emitted');
        assert.equal(upgradedEvent.args.implementationVersion, 2, 'the implementation version is wrong');
        assert.equal(upgradedEvent.args.implementationAddress, TestERTTokenV2.address,'the implementation address is wrong');

        const upgradedDataEvent = receipt.logs.find(log => log && log.event && log.event === 'UpgradedData');
        assert.isDefined(upgradedDataEvent ,'UpgradedData event should be emitted');
        assert.equal(upgradedDataEvent.args.dataVersion, 2, 'the data version is wrong in event');
        assert.equal(upgradedDataEvent.args.dataAddress, TestERTTokenDataV2.address,'the data V2 address is wrong in event');
        return tokenInstance.implementationAddress.call();
      }).then(function(implementation) {
        assert.equal(implementation, TestERTTokenV2.address, 'Currently used implementation on Proxy contract should be V2 implementation after the upgrade');
        return tokenInstance.version.call();
      }).then(version => {
        assert.equal(version, 2, 'Currently used implementation version on Proxy contract should be 2 after the upgrade');
        return tokenInstance.getDataAddress(1);
      }).then(dataAddressV1 => {
        assert.equal(dataAddressV1, ERTTokenDataV1.address , 'the Data Token V1 address should be preserved on proxy contract'); 
        return tokenInstance.getDataAddress(2);
      }).then(dataAddressV2 => {
        assert.equal(dataAddressV2, TestERTTokenDataV2.address , 'the Data Token V2 address should be added on proxy contract'); 
        return ERTTokenDataV1.deployed();
      }).then(instance => {
        tokenDataV1Instance = instance;
        return tokenDataV1Instance.implementation.call();
      }).then(address => {
        assert.equal(address, TestERTTokenV2.address , 'the Data Token should have transferred its ownership to implementation V2'); 
        return tokenDataV1Instance.proxy.call();
      }).then(address => {
        assert.equal(address, ERTToken.address , 'the Data Token should have transferred its ownership to implementation V2'); 
        return TestERTTokenDataV2.deployed();
      }).then(instance => {
        tokenDataV2Instance = instance;
        return tokenDataV2Instance.implementation.call();
      }).then(address => {
        assert.equal(address, TestERTTokenV2.address , 'the Data Token V2 should have transferred its ownership to implementation V2'); 
        return tokenDataV2Instance.proxy.call();
      }).then(address => {
        assert.equal(address, ERTToken.address , 'the Data Token V2 should have transferred its ownership to implementation V2'); 
        return web3.eth.getBalance(ERTToken.address);
      }).then(function(result) {
        assert.equal(String(result), fiveWei, 'the balance of the ERTToken should be 5 ether');
        return web3.eth.getBalance(ERTTokenV1.address);
      }).then(result => {
        assert.equal(result, 0, 'the balance of ERTTokenV1 should be 0 ');
        return web3.eth.getBalance(TestERTTokenV2.address);
      }).then(function(result) {
        assert.equal(result, 0, 'the balance of TestERTokenV2 should be 0');
      });
  });

  it('Old implementation should be kept paused', function() {
   return ERTTokenV1.deployed()
     .then(instance => {
       tokenInstance = instance;
       return tokenInstance.paused.call();
     }).then(function(result) {
       assert.equal(result, true, 'old and useless implementation should be paused'); 
     });
  })

  it('New implementations should be kept paused', function() {
   return TestERTTokenV2.deployed()
     .then(instance => {
       return instance.paused.call();
     }).then(function(result) {
       assert.equal(result, true, 'V2 implementation should be paused to avoid calling the contract impl directly');
       return TestERTTokenV3.deployed();
     }).then(instance => {
       return instance.paused.call();
     }).then(function(result) {
       assert.equal(result, true, 'V3 implementation should be paused to avoid calling the contract impl directly'); 
     });
  })

  it("proxy contract shouldn't be paused", function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.paused.call();
      }).then(function(result) {
        assert.equal(result, false, "proxy contract shouldn't be paused"); 
      });
  })

  it('Test access to Data V1 information after upgrade', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;  
        return tokenInstance.name();
      }).then(function(name) {
        assert.equal(name, 'Curries', 'has not the correct name');
        return tokenInstance.totalSupply();
      }).then(function(totalSupply) {
        assert.equal(totalSupply.toNumber(), 100000 * decimals, 'has not the correct totalSupply');
        return tokenInstance.isFrozen(accounts[5]);
      }).then(function(result) {
        assert.equal(result, false, 'accounts should be not freezen');
      });
  })

  it('test data write using new contract Implementation', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;  
        return tokenInstance.freeze(accounts[5]);
      }).then(function(result) {
        return tokenInstance.isFrozen(accounts[5]);
      }).then(function(result) {
        assert.equal(result, true, 'accounts should be frozen ');
        return tokenInstance.unFreeze(accounts[5]);
      }).then(function(result) {
        return tokenInstance.isFrozen(accounts[5]);
      }).then(function(result) {
        assert.equal(result, false, "accounts shouldn't be frozen");
      });
  })

  it('should not make transfer token after freeze accounts ', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.freeze(accounts[5]);
      }).then(() => {
        return tokenInstance.transfer(accounts[5], 50 * decimals);
      }).then(receipt => {
        return tokenInstance.balanceOf(accounts[5]);
      }).then(balance => {
        assert.equal(balance.toNumber(), 50 * decimals, 'Wrong balance of accounts[5]');
        return tokenInstance.transfer(accounts[3], 20 * decimals, {from : accounts[5]});
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, "accounts[5] is frozen, thus he shouldn't be able to send his funds");
        return tokenInstance.unFreeze(accounts[5]);
      }).then(function(result) {
        return tokenInstance.isFrozen(accounts[5]);
      }).then(function(result) {
        assert.equal(result, false, "accounts[5] shouldn't be frozen");
        return tokenInstance.transfer(accounts[0], 20 * decimals, {from : accounts[5]});
      }).then(receipt => {
        return tokenInstance.balanceOf(accounts[5]);
      }).then(balance => {
        // Transfer is payed by Token, thus we can't check exact balance
        assert.isTrue(balance.toNumber() < 30 * decimals, 'Wrong balance of accounts[5]');
      });
  })

  it('Test Data write for OLD implementation ', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;  
        return tokenInstance.setSellPrice(web3.toWei(3, 'finney'));
      }).then(function(result) {
        return tokenInstance.getSellPrice();
      }).then(function(sellPrice) {
        assert.equal(sellPrice.toNumber(), web3.toWei(3, 'finney').toString(), 'writing for old implementation is wrong should be the new setting ');
      });
  })

  it('when new implementation V3 was provided', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
          return tokenInstance.implementationAddress.call();
        }).then(function(implementation) {
          assert.equal(implementation, TestERTTokenV2.address, 'should return the current implementation');    
          return tokenInstance.upgradeImplementation(ERTToken.address, 3, TestERTTokenV3.address);
        }).then(function(receipt) {
          return tokenInstance.implementationAddress.call();
        }).then(function(implementation) {
          assert.equal(implementation, TestERTTokenV3.address, 'should return the given implementation'); 
        });
  })

  it('Old implementation should be paused', function() {
    return TestERTTokenV2.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.paused.call();
      }).then(function(result) {
        assert.equal(result, true , 'old and useless implementation should be paused'); 
      });
  })

  it('new implementation should be paused', function() {
    return TestERTTokenV3.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.paused.call();
      }).then(function(result) {
        assert.equal(result, true, 'new implementation should be paused to avoid calling the contract impl directly'); 
      });
  })

  it('access to data and use Impl V2 and V1', function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.totalSupply();
      }).then(function(totalSupply) {
        assert.equal(totalSupply, 100000 * decimals, 'has not the correct totalSupply');
        return tokenInstance.freeze(accounts[6]);
      }).then(function(result) {
        return tokenInstance.isFrozen(accounts[6]);
      }).then(function(result) {
        assert.equal(result, true, 'accounts should be frozen ');
        return tokenInstance.transfer(accounts[6], String(50 * decimals));
      }).then(receipt => {
        return tokenInstance.unFreeze(accounts[6]);
      }).then(function(result) {
        return tokenInstance.transfer(accounts[0], String(20 * decimals), {from : accounts[6]});
      }).then(receipt => {
        return tokenInstance.balanceOf(accounts[6]);
      }).then(balance => {
        assert.isTrue(balance.toNumber() < 30 * decimals, 'Wrong balance of accounts[6], the sender is not Frozen so he made the transfer');
      });
  });

  it('should Burn tokens', function () {
    return ERTToken.deployed().
      then(instance => {
        tokenInstance = instance ;
        return tokenInstance.burn(100001 * decimals);
      }).then(assert.fail).catch(function(error) {
        assert(error.message.indexOf('revert') >= 0, 'message must contain revert: no burn with value larger than the sender s balance');
        return tokenInstance.totalSupply();
      }).then(function (totalSupply){
        assert.equal(totalSupply.toNumber(), 100000  * decimals , 'the current total supply is wrong');
        return tokenInstance.burn(35 * decimals , {from : accounts[0]});
      }).then(function (receipt){
        assert(receipt.logs.length = 1,'number of emitted event is wrong');
        assert.equal(receipt.logs[0].event, 'Burn','should be the "Burn" event');
        assert.equal(receipt.logs[0].args.burner, accounts[0],'the account the tokens are burned from is wrong');
        assert.equal(receipt.logs[0].args.value, 35 * decimals,'the burned amount is wrong');
        return tokenInstance.totalSupply();
      }).then(function (totalSupply){
        assert.equal(totalSupply, (100000- 35) * decimals, 'the total Supply is wrong');
      });
  });

  it("should mint token ", function() {
    return ERTToken.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.balanceOf(accounts[1]);
      }).then(function(balance) {
        assert.equal(balance.valueOf(), 0, "it should be 0");
        return tokenInstance.mintToken(accounts[1], 50 * decimals, {from : accounts[0]});
      }).then(function(result) {
        assert.equal(result.logs.length, 3,'number of emitted event is wrong');
        assert.equal(result.logs[0].event, 'Transfer','should be the "Transfer" event');
        assert.equal(result.logs[0].args._from, 0,'the account the tokens are transferred from is wrong');
        assert.equal(result.logs[0].args._to, accounts[0],'the account the tokens are transferred to is wrong');
        assert.equal(result.logs[0].args._value, 50 * decimals,'the transfer amount is wrong');
        assert.equal(result.logs[1].event, 'Transfer','should be the "Transfer" event');
        assert.equal(result.logs[1].args._from, accounts[0],'the account the tokens are transferred from is wrong');
        assert.equal(result.logs[1].args._to, accounts[1],'the account the tokens are transferred to is wrong');
        assert.equal(result.logs[1].args._value, 50 * decimals,'the transfer amount is wrong')
        assert.equal(result.logs[2].event, 'MintedToken','should be the "MintedToken" event');
        assert.equal(result.logs[2].args.minter, accounts[0],'the account the tokens are transferred from is wrong');
        assert.equal(result.logs[2].args.target, accounts[1],'the account the tokens are transferred to is wrong');
        assert.equal(result.logs[2].args.mintedAmount, 50 * decimals,'the minted amount is wrong');
        return tokenInstance.balanceOf(accounts[1]);
      }).then(function(balance) {
        assert.equal(balance.valueOf(), 50 * decimals, "it should be 50 * 10 ^ 18");
        return tokenInstance.totalSupply();
      }).then(function(totalSupply) {
        assert.equal(totalSupply, (100000-35+50) * decimals, 'has not the correct totalSupply');  
      });
  });

  it('owner of TestERTTokenV2 again after another new impl', function() {
    return TestERTTokenV2.deployed()
      .then(instance => {
        tokenInstance = instance;
        return tokenInstance.getDataAddress(1);
      }).then(function(result) {
        assert.equal(result, 0x0 , 'the TestERTTokenV2 shouldn\'t have a reference to new address'); 
      });
  });
});


