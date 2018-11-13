pragma solidity ^0.4.24;
import "../DataOwned.sol";

contract TestERTTokenDataV2 is DataOwned {

    mapping (address => bool) internal frozenAccount_;

    constructor(address _proxyAddress, address _implementationAdress) public{
        proxyAddress_ = _proxyAddress;
        implementationAddress_ = _implementationAdress;
    }

    function isFrozen(address _target) public view returns(bool){
        return frozenAccount_[_target];
    }

    function setFrozenAccount(address _target, bool _frozen) public onlyContracts{
        frozenAccount_[_target] = _frozen;
    }

}
