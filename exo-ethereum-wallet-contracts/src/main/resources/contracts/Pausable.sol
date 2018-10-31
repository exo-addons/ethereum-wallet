pragma solidity ^0.4.24;
import "./Owned.sol";

contract Pausable is Owned {
    event ContractPaused();
    event ContractUnPaused();

    constructor() internal{
    }

    modifier whenNotPaused(){
        require (!paused);
        _;
    }

    modifier whenPaused(){
        require (paused);
        _;
    }

    function isPaused() public view returns (bool){
        return paused;
    }

    function pause() public onlyOwner whenNotPaused returns (bool){
        paused = true;
        emit ContractPaused();
        return true;
    }

    function unPause() public onlyOwner whenPaused returns (bool){
        paused = false;
        emit ContractUnPaused();
        return true;
    }
}
