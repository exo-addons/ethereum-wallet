pragma solidity ^0.4.24;

import "./Owned.sol";
import "./DataAccess.sol";

contract Admin is Owned, DataAccess {

    event AddedAdmin(address target);
    event RemovedAdmin(address target);

    constructor() internal{
    }

    function addAdmin(address _target) public onlyOwner returns (bool){
        if (!super.isAdmin(_target) && _target != address(0)) {
            super.setAdmin(_target, true);
            emit AddedAdmin(_target);
        }
    }

    function removeAdmin(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        if (super.isAdmin(_target)) {
            super.setAdmin(_target, false);
            emit RemovedAdmin(_target);
        }
    }

    function isAdmin(address _target) public view returns (bool){
        return super.isAdmin(_target);
    }

    modifier onlyAdmin(){
        require (msg.sender == owner || super.isAdmin(msg.sender));
        _;
    }
}



 