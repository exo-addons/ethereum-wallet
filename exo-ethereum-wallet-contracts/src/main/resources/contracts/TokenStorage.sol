pragma solidity ^0.4.24;
import './DataOwned.sol';

/**
 * @title TokenStorage.sol
 * @dev This is an abstract contract that holds the variables that are shared between
 * implementation and Proxy contracts
 */
contract TokenStorage {

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    // Reference to the Token implementation reference
    address public implementationAddress;

    // Proxy and Token owner
    address public owner;

    // Proxy address
    address public proxy;

    // A local variable for each Token instance to pause old and useless
    // implementations when upgrading to a newer Token implementation version
    bool public paused;

    // Current implementation version
    uint16 public version;

    // Map of data contracts by version number
    mapping(uint16 => address) internal dataAddresses_;

    // An array of data versions
    uint16[] internal dataVersions_;

    /**
     * @dev sets a new data contract address by version.
     * Only new versions are accepted to avoid overriding an existing reference to a version
     * (only contract owner can set it)
     * @param _dataVersion version number of data contract
     * @param _dataAddress address of data contract
     */
    function _setDataAddress(uint16 _dataVersion, address _dataAddress) internal{
        // Make sure that we can't change a reference of an existing data reference
        if(dataAddresses_[_dataVersion] == address(0)) {
            dataAddresses_[_dataVersion] = _dataAddress;
            dataVersions_.push(_dataVersion);
        }
    }

    /**
     * @dev transfers data ownership to a proxy and token implementation
     * @param _dataVersion Data version to transfer its ownership
     */
    function _transferDataOwnership(uint16 _dataVersion, address _proxy, address _implementation) internal{
        require(dataAddresses_[_dataVersion] != address(0));
        require(_proxy != address(0));
        require(_implementation != address(0));
        DataOwned(dataAddresses_[_dataVersion]).transferDataOwnership(_proxy, _implementation);
    }
}
