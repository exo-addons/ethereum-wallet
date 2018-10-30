pragma solidity ^0.4.24;
import './Owned.sol';

contract Proxy is Owned {

  address private implementation;

  bool isPayable;

  bool preserveContext;

  event Upgraded(address implementation);

  constructor() internal{}

  function upgradeTo(address newImplementation) public onlyOwner{
    implementation = Owned(newImplementation);
    if (!preserveContext) {
        if (!implementation.delegatecall(bytes4(keccak256("transferOwnership(address)")), address(this))) {
            revert();
        }
    }
    emit Upgraded(newImplementation);
  }

  function () payable public {
    // Avoid getting ether when contract doesn't allow it
    if (msg.value > 0 && !isPayable) {
        revert();
    }

    uint256 value = msg.value;
    bool _preserveContext = preserveContext;
    address _implementation = implementation;
    assembly {
      calldatacopy(0x0, 0x0, calldatasize)
      let success := 0
      switch _preserveContext
      case 0 {
          success := callcode(sub(gas, 10000), _implementation, value, 0x0, calldatasize, 0, 0)
      }
      default {
          success := delegatecall(sub(gas, 10000), _implementation, 0x0, calldatasize, 0, 0)
      }
      let retSz := returndatasize
      returndatacopy(0, 0, retSz)
      switch success
      case 0 {
        revert(0, retSz)
      }
      default {
        return(0, retSz)
      }
    }
  }
}
