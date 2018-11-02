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
        require (!paused && !super.isPaused());
        _;
    }

    function pause() public onlyOwner returns (bool){
        super._setPaused(true);
        paused = true;
        emit ContractPaused();
        return true;
    }

    function unPause() public onlyOwner returns (bool){
        super._setPaused(false);
        paused = false;
        emit ContractUnPaused();
        return true;
    }
}
