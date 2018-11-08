pragma solidity ^0.4.24;
import './TokenStorage.sol';
import "./Admin.sol";
import "./DataAccess.sol";

/**
 * @title Pausable.sol
 * @dev Abstract contract to determine whether the contract is paused or not
 */
contract Pausable is TokenStorage, DataAccess, Admin {

    // Event emitted when the contract is paused
    event ContractPaused();
    // Event emitted when the contract is un-paused
    event ContractUnPaused();

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev a modifier to check if the contract is paused
     */
    modifier whenNotPaused(){
        require (!paused && !super.isPaused());
        _;
    }

    /**
     * @dev pause the contract
     */
    function pause() public onlyAdmin(5){
        super._setPaused(true);
        paused = true;
        emit ContractPaused();
    }

    /**
     * @dev unpause the contract
     */
    function unPause() public onlyAdmin(5){
        super._setPaused(false);
        paused = false;
        emit ContractUnPaused();
    }
}
