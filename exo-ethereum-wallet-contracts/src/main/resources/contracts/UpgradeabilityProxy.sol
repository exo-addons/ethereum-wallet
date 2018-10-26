pragma solidity ^0.4.24;

import './Proxy.sol';


contract UpgradeabilityProxy is Proxy {
 
  event Upgraded(address indexed implementation);

  // Storage position of the address of the current implementation
  bytes32 private constant implementationPosition = keccak256("org.exoplateform.wallet.proxy.implementation");

 
  constructor() public {}
           


  function implementation() public view returns (address impl) {
    bytes32 position = implementationPosition;
    assembly {
      impl := sload(position)
    }
  }

 
  function setImplementation(address newImplementation) internal {
    bytes32 position = implementationPosition;
    assembly {
      sstore(position, newImplementation)
    }
  }

  
  function _upgradeTo(address newImplementation) internal {
    address currentImplementation = implementation();
    require(currentImplementation != newImplementation);
    setImplementation(newImplementation);
    emit Upgraded(newImplementation);
  }
}