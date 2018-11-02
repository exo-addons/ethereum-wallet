pragma solidity ^0.4.24;
import "./Owned.sol";
import "./DataOwned.sol";

/*
 * @title Upgradability
 * @dev This contract will allow to do Data and Impl upgrade operations
 */
contract Upgradability is Owned{

    /*
     * @dev transfer data contract (identified by a version) ownership to proxyAddress and implementation address
     * (only contract owner can set it)
     * @param _dataVersion version number of data contract to transfer its ownership
     * @param _proxyAddress proxy contract address
     * @param _implementationAddress ERC20 Token implementation address
     */
    function transferDataOwnership(uint16 _dataVersion, address _proxyAddress, address _implementationAddress) public onlyOwner{
        require(dataAddress[_dataVersion] != address(0));
        DataOwned(dataAddress[_dataVersion]).transferDataOwnership(_proxyAddress, _implementationAddress);
    }

    /*
     * @dev Upgrade current implementation to the new one. This method can be called only through
     * proxy call to avoid calling this by error.
     * This method will make the current implementation unusable and it will send its funds to the new
     * contract implementation.
     * @param _newImplementation new implementation address
     */
    function upgradeTo(address _newImplementation) public onlyProxy{
        // Disable ownership
        owner = address(0);
        // Make the contract paused
        paused = true;
        // Transfer ether to new implementation
        _newImplementation.transfer(address(this).balance);
    }

}
