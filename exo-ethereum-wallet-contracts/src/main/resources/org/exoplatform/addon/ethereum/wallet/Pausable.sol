pragma solidity ^0.4.25;
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

    function pause() onlyOwner whenNotPaused returns (bool){
        paused = true;
        emit Pause();
        return true;
    }

    function unPause() onlyOwner whenPaused returns (bool){
        paused = false;
        emit UnPause();
        return true;
    }
}
