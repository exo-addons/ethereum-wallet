pragma solidity ^0.4.24;
import "./Owned.sol";

contract FreezableAccount is Owned {

    event FrozenAccount(address target);
    event UnFrozenAccount(address target);

    mapping (address => bool) public frozenAccount;

    constructor() internal{
    }

    function freezeAccount(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        if (!frozenAccount[_target]) {
            frozenAccount[_target] = true;
            emit FrozenAccount(_target);
        }
    }

    function unFrozenAccount(address _target) public onlyOwner returns (bool){
        if (frozenAccount[_target]) {
            frozenAccount[_target] = false;
            emit UnFrozenAccount(_target);
        }
    }

    modifier whenNotFrozen(){
        require (owner == msg.sender || !frozenAccount[msg.sender]);
        _;
    }
}
