pragma solidity ^0.4.24;
import './TokenStorage.sol';
import './Owned.sol';
import './DataOwned.sol';
import './Upgradability.sol';

/*
 * @dev Proxy contract that delegates calls to a dedicated ERC20 implementation
 * contract. The needed data here are implementation address and owner.
 */
contract ERTToken is TokenStorage, Owned {

    event Upgraded(uint16 implementationVersion, address implementationAddress);

    constructor(address _implementationAddress, address _dataAddress) public{
        super.setDataAddress(1, _dataAddress);
        upgradeTo(1, _implementationAddress);
    }

    function upgradeTo(uint16 _version, address _newImplementation) public onlyOwner{
        // Change implementation reference and emit event
        require(_version > version);
        version = _version;
        if (implementationAddress != 0) {
          Upgradability(implementationAddress).upgradeTo(_newImplementation);
        }
        implementationAddress = _newImplementation;

        emit Upgraded(_version, _newImplementation);
    }

    function () payable public{
      _delegateCall(implementationAddress);
    }

    function _delegateCall(address _impl) private{
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
