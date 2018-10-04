pragma solidity ^0.4.25;
import "./Owned.sol";

contract FreezableAccount is Owned {

    event FrozenAccount(address target);
    event UnFrozenAccount(address target);

    mapping (address => bool) public frozenAccount;

    function freezeAccount(address _target) onlyOwner returns (bool){
        if (!frozenAccount[_target]) {
            frozenAccount[_target] = true;
            emit FrozenAccount(_target);
        }
    }

    function unFrozenAccount(address _target) onlyOwner returns (bool){
        if (frozenAccount[_target]) {
            frozenAccount[_target] = false;
            emit UnFrozenAccount(_target);
        }
    }

    modifier whenNotFrozen(){
        require (!frozenAccount[msg.sender]);
        _;
    }
}
