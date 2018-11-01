pragma solidity ^0.4.24;
import './TokenStorage.sol';
import './Owned.sol';

/*
 * @dev Proxy contract that delegates calls to a dedicated ERC20 implementation
 * contract. The needed data here are implementation address and owner.
 */
contract ERTToken is TokenStorage, Owned {

    event Upgraded(uint16 implementationVersion, address implementationAddress, uint16 dataVersion, address dataAddress);

    constructor(address _implementationAddress, address _dataAddress) public{
        upgradeTo(1, _implementationAddress, 1, _dataAddress);
    }

    function upgradeTo(uint16 _implementationVersion, address _newImplementation, uint16 _dataVersion, address _dataAddress) public onlyOwner{
        // Change implementation reference and emit event
        implementationAddress = _newImplementation;
        dataAddress[_dataVersion] = _dataAddress;
        emit Upgraded(_implementationVersion, _newImplementation, _dataVersion, _dataAddress);
    }

    function () payable public{
      address _impl = implementationAddress;
      assembly {
         let ptr := mload(0x40)
         calldatacopy(ptr, 0, calldatasize)
         let result := delegatecall(gas, _impl, ptr, calldatasize, 0, 0)
         let size := returndatasize
         returndatacopy(ptr, 0, size)

         switch result
         case 0 { revert(ptr, size) }
         default { return(ptr, size) }
      }
    }
}
