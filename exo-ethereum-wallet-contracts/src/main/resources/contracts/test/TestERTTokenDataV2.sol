pragma solidity ^0.4.24;
import "../DataOwned.sol";

contract TestERTTokenDataV2 is DataOwned {

    mapping (address => bool) internal frozenAccount_;

    bool internal initialized_ = false;

    constructor() public{
    }

    function initialized() public view returns(bool){
        return initialized_;
    }

    function isFrozen(address _target) public view returns(bool){
        return frozenAccount_[_target];
    }

    function setInitialized() public onlyContracts{
        initialized_ = true;
    }

    function setFrozenAccount(address _target, bool _frozen) public onlyContracts{
        frozenAccount_[_target] = _frozen;
    }

}
