pragma solidity ^0.4.24;

import "./Owned.sol";
import "./ERTTokenDataProxy.sol";

contract Admin is Owned, ERTTokenDataProxy {

    event AddAdmin(address target);
    event RemoveAdmin(address target);

    constructor() internal{
    }

    function addAdmin(address _target) public onlyOwner returns (bool){
        if (!super.addAdmin(_target) && _target != address(0)) {
            super.setAdmin(_target, true);
            emit AddAdmin(_target);
        }
    }

    function removeAdmin(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        if (super.addAdmin(_target)) {
            super.setAdmin(_target, false);
            emit RemoveAdmin(_target);
        }
    }

    function isAdmin(address _target) public view returns (bool){
        return super.addAdmin(_target);
    }

    modifier onlyAdmin(address _from, address _to){
        require (owner == _from || super.addAdmin(_to));
        _;
    }
    
   
}



 