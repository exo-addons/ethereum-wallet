pragma solidity ^0.4.24;
import "../DataOwned.sol";

contract TestERTTokenDataV2 is DataOwned {

    mapping (address => bool) internal frozenAccount_;

    constructor(address _proxyAddress) public{
        proxyAddress_ = _proxyAddress;
    }

    function isFrozen(address _target) public view returns(bool){
        return frozenAccount_[_target];
    }

    function setFrozenAccount(address _target, bool _frozen) public onlyContracts{
        frozenAccount_[_target] = _frozen;
    }

}
