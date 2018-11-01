pragma solidity ^0.4.24;
import './TokenStorage.sol';
import "./Owned.sol";
import "./DataAccess.sol";

contract Pausable is TokenStorage, Owned, DataAccess {
    event ContractPaused();
    event ContractUnPaused();

    constructor() internal{
    }

    modifier whenNotPaused(){
        require (!super.isPaused());
        _;
    }

    modifier whenPaused(){
        require (super.isPaused());
        _;
    }

    function pause() public onlyOwner whenNotPaused returns (bool){
        super.setPaused(true);
        emit ContractPaused();
        return true;
    }

    function unPause() public onlyOwner whenPaused returns (bool){
        super.setPaused(false);
        emit ContractUnPaused();
        return true;
    }
}
