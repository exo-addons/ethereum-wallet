pragma solidity ^0.4.24;
import "./Owned.sol";

contract Pausable is Owned {
    event Pause();
    event UnPause();

    bool public paused = false;

    modifier whenNotPaused(){
        require (!paused);
        _;
    }

    modifier whenPaused{
        require (paused);
        _;
    }

    function pause() public onlyOwner whenNotPaused returns (bool){
        paused = true;
        emit Pause();
        return true;
    }

    function unPause() public onlyOwner whenPaused returns (bool){
        paused = false;
        emit UnPause();
        return true;
    }
}
