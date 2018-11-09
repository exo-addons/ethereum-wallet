pragma solidity ^0.4.24;
import "../TokenStorage.sol";
import "./TestERTTokenDataV2.sol";


contract TestDataAccess is TokenStorage{

    constructor() internal{
    }

    function isFrozen(address _target) public view returns(bool){
        return TestERTTokenDataV2(dataAddresses_[2]).isFrozen(_target);
    }

    function _setFrozenAccount(address _target, bool frozen) internal{
        TestERTTokenDataV2(dataAddresses_[2]).setFrozenAccount(_target, frozen);
    }

}
