pragma solidity ^0.4.24;
import "./Owned.sol";
import "./DataOwned.sol";

/*
 * @title Upgradability
 * @dev This contract will allow to do Data and Impl upgrade operations
 */
contract Upgradability is Owned{
    // Event emitted when an upgrade is made to a new implementation
    event Upgraded(uint16 implementationVersion, address implementationAddress);
     // Event emitted when an upgrade is made to a new data
    event UpgradedData(uint16 dataVersion, address DataAddress);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev Upgrade to a new implementation of ERC20 contract
     * @param _proxy proxy contract address
     * @param _version the version of the new implementation that should be higher
     * that current version
     * @param _newImplementation new ERC20 contract address
     */
    function upgradeImplementation(address _proxy, uint16 _version, address _newImplementation) public onlyOwner{
        require(_version > version);
        require(implementationAddress != _newImplementation);

        version = _version;
        implementationAddress = _newImplementation;

        for (uint i=0; i< dataVersions_.length; i++) {
          _transferDataOwnership(dataVersions_[i], _proxy, _newImplementation);
        }

        emit Upgraded(_version, _newImplementation);
    }

    /**
     * @dev Upgrade to a new implementation of ERC20 contract
     * @param _proxy proxy contract address
     * @param _version the version of the new implementation that should be higher
     * that current version
     * @param _newImplementation new ERC20 contract address
     * @param _dataVersion version number of data contract
     * @param _dataAddress address of data contract
     */
    function upgradeDataAndImplementation(address _proxy, uint16 _version, address _newImplementation, uint16 _dataVersion, address _dataAddress) public onlyOwner{
        // Upgrade data before implementation to perform ownership
        // transfer for new implementation just after
        upgradeData(_dataVersion, _dataAddress);
        upgradeImplementation(_proxy, _version, _newImplementation);
    }

    /**
     * @dev Upgrade to a new data contract
     * @param _dataVersion version number of data contract
     * @param _dataAddress address of data contract
     */
    function upgradeData(uint16 _dataVersion, address _dataAddress) public onlyOwner{
         super._setDataAddress(_dataVersion, _dataAddress);
         emit UpgradedData(_dataVersion, _dataAddress);
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
