pragma solidity ^0.4.24;
import "../Owned.sol";
import "./TestDataAccess.sol";

contract TestAccountFreeze is Owned, TestDataAccess{

    event FrozenAccount(address target);
    event UnFrozenAccount(address target);

    constructor() internal{
    }

    function freeze(address _target) public onlyOwner{
        if (!super.isFrozen(_target)) {
            super._setFrozenAccount(_target, true);
            emit FrozenAccount(_target);
        }
    }

    function unFreeze(address _target) public onlyOwner{
        if (super.isFrozen(_target)) {
            super._setFrozenAccount(_target, false);
            emit UnFrozenAccount(_target);
        }
    }

    function isFrozen(address _target) public view returns (bool){
        return super.isFrozen(_target);
    }

    modifier whenNotFrozen(){
        require (!super.isFrozen(msg.sender));
        _;
    }

}


