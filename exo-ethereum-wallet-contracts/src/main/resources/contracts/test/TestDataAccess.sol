pragma solidity >=0.4.24;
import "../TokenStorage.sol";
import "./TestERTTokenDataV2.sol";


contract TestDataAccess is TokenStorage{

    constructor() internal{
    }

    function isFrozen(address _target) public view returns(bool){
        address dataAddress = super.getDataAddress(2);
        require(dataAddress != address(0));
        return TestERTTokenDataV2(dataAddress).isFrozen(_target);
    }

    function _setFrozenAccount(address _target, bool frozen) internal{
        address dataAddress = super.getDataAddress(2);
        require(dataAddress != address(0));
        TestERTTokenDataV2(dataAddress).setFrozenAccount(_target, frozen);
    }
}
