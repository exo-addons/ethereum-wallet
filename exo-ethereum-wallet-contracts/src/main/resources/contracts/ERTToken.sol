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

    event Upgraded(uint16 implementationVersion, uint16 dataVersion, address implementationAddress, address dataAddress);

    constructor(address _implementationAddress, address _dataAddress) public{
        version = 1;
        implementationAddress = _implementationAddress;
        dataAddress[1] = _dataAddress;
    }

    function upgradeTo(uint16 _version, uint16 _dataVersion, address _newImplementation, address _dataAddress) public onlyOwner{
        // Change implementation reference and emit event
        require(_version > version);
        DataOwned(dataAddress[_dataVersion]).transferDataOwnership(address(this), _newImplementation);
        Upgradability(implementationAddress).upgradeTo(_newImplementation);
        version = _version;
        implementationAddress = _newImplementation;
        dataAddress[_dataVersion] = _dataAddress;

        emit Upgraded(_version, _dataVersion, _newImplementation, _dataAddress);
    }

    function () payable public{
      _delegateCall(implementationAddress);
    }

    function _delegateCall(address _impl) internal{
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
