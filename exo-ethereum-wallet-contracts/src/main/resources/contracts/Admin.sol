pragma solidity ^0.4.24;
import "./Owned.sol";
import "./DataAccess.sol";

contract Admin is Owned, DataAccess {

    event AddedAdmin(address target, uint8 level);
    event RemovedAdmin(address target);

    constructor() internal{
    }

    function addAdmin(address _target, uint8 _level) public onlyOwner{
        // Admin levels from 1 to 5 only
        require(_level > 0 && _level < 6);
        if (_target != address(0)) {
            super._setAdmin(_target, _level);
            emit AddedAdmin(_target, _level);
        }
    }

    function removeAdmin(address _target) public onlyOwner{
        require (owner != _target);
        if (super.isAdmin(_target, 1)) {
            super._setAdmin(_target, 0);
            emit RemovedAdmin(_target);
        }
    }

    function isAdmin(address _target, uint8 _level) public view returns (bool){
        return super.isAdmin(_target, _level);
    }

    modifier onlyAdmin(uint8 _level){
        require (msg.sender == owner || super.isAdmin(msg.sender, _level));
        _;
    }
}


