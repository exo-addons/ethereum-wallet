pragma solidity ^0.4.24;
import './Owned.sol';

contract ERTToken is TokenImplStorage, Owned {

    address private implementation;

    event Upgraded(address implementation);

    constructor(address _implementation) public{
        upgradeTo(_implementation);
    }

    function upgradeTo(address newImplementation) public onlyOwner{
        // Change implementation reference and emit event
        implementation = newImplementation;
        emit Upgraded(newImplementation);
    }

    function () payable public{
        address _implementation = implementation;
        assembly {
        calldatacopy(0x0, 0x0, calldatasize)
        let success := delegatecall(sub(gas, 10000), _implementation, 0x0, calldatasize, 0, 0)
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
