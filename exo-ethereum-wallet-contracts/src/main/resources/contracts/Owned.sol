pragma solidity ^0.4.24;
import './TokenStorage.sol';

/**
 * @title Owned.sol
 * @dev Abstract contract to determine ownership of the contract
 */
contract Owned is TokenStorage {

    // Event emitted when owner changes
    event TransferOwnership(address newOwner);
    // Event emitted when proxy address changes
    event TransferProxyOwnership(address proxy);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
        owner = msg.sender;
    }

    /**
     * @dev a modifier to check if the transaction issuer is the owner
     */
    modifier onlyOwner(){
        require(msg.sender == owner);
        _;
    }

    /**
     * @dev a modifier to check if the transaction issuer is the proxy contract
     */
    modifier onlyProxy(){
        require(msg.sender == proxy);
        _;
    }

    /**
     * @dev Modifies the owner of the contract
     * @param _newOwner new owner
     */
    function transferOwnership(address _newOwner) public onlyOwner{
        if (_newOwner != address(0)) {
            owner = _newOwner;
            emit TransferOwnership(_newOwner);
        }
    }

    /**
     * @dev Modifies the proxy of the contract
     * @param _proxy new proxy contract address
     */
    function setProxy(address _proxy) public onlyOwner{
        require(_proxy != address(0));
        proxy = _proxy;
        emit TransferProxyOwnership(_proxy);
    }
}
