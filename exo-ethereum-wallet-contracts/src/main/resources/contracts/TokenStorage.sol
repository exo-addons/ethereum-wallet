pragma solidity ^0.4.24;

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

    /**
     * @dev sets a new data contract address by version.
     * Only new versions are accepted to avoid overriding an existing reference to a version
     * (only contract owner can set it)
     * @param _dataVersion version number of data contract
     * @param _dataAddress address of data contract
     */
    function setDataAddress(uint16 _dataVersion, address _dataAddress) public{
        // Owner and proxy check
        require(msg.sender == owner || msg.sender == proxy);
        // Make sure that we can't change a reference of an existing data reference
        if(dataAddresses_[_dataVersion] == address(0)) {
            dataAddresses_[_dataVersion] = _dataAddress;
        }
        if (implementationAddress != address(0)) {
            TokenStorage(implementationAddress).setDataAddress(_dataVersion, _dataAddress);
        }
    }
}
