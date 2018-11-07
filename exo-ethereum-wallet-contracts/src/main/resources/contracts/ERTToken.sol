pragma solidity ^0.4.24;
import './TokenStorage.sol';
import './Owned.sol';
import './DataOwned.sol';
import './Upgradability.sol';

/**
 * @title ERTToken.sol
 * @dev Proxy contract that delegates calls to a dedicated ERC20 implementation
 * contract. The needed data here are implementation address and owner.
 */
contract ERTToken is TokenStorage, Owned {

    // Event emitted when an upgrade is made to a new implementation
    event Upgraded(uint16 implementationVersion, address implementationAddress);

    /**
     * @param _implementationAddress First version of the ERC20 Token implementation address
     * @param _dataAddress First version of data contract address
     */
    constructor(address _implementationAddress, address _dataAddress) public{
        require(_dataAddress != address(0));
        require(_implementationAddress != address(0));

        // Update data address before setting implementation
        // to not call setDataAddress on ERC20 implementation too
        super.setDataAddress(1, _dataAddress);

        // Set implementation address and version
        upgradeImplementation(1, _implementationAddress);

        // Set current proxy address so that ERC20 implementation can access it when calls are delegated to it
        proxy = address(this);
    }

    /**
     * @dev Upgrade to a new implementation of ERC20 contract
     * @param _version the version of the new implementation that should be higher
     * that current version
     * @param _newImplementation new ERC20 contract address
     */
    function upgradeImplementation(uint16 _version, address _newImplementation) public onlyOwner{
        // Change implementation reference and emit event
        require(implementationAddress != _newImplementation);
        require(_version > version);
        version = _version;
        // The current implementation can be = 0x address when deploying the contract at the first time
        if (implementationAddress != 0) {
          Upgradability(implementationAddress).upgradeImplementationTo(_newImplementation);
        }
        implementationAddress = _newImplementation;

        emit Upgraded(_version, _newImplementation);
    }

    /**
     * @dev Called for all calls that aren't implemented on proxy, like
     * ERC20 methods. This is payable to enable owner to give money to
     * the ERC20 contract
     */
    function () payable public{
      _delegateCall(implementationAddress);
    }

    /**
     * @dev Delegate call to ERC20 contract
     */
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
