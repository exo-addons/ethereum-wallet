pragma solidity ^0.4.24;
import "../ERTTokenV1.sol";
import "./TestAccountFreeze.sol";
import "./TestDataAccess.sol";

contract TestERTTokenV2 is ERTTokenV1, TestDataAccess, TestAccountFreeze{

    constructor(address _dataAddressV1, address _dataAddressV2 ,address _proxyAddress) ERTTokenV1(_dataAddressV1, _proxyAddress) public{
        require(_dataAddressV2 != address(0));
        super._setDataAddress(2, _dataAddressV2);
    }

    function initialize(address _proxyAddress) public onlyOwner{
        require(!super.initializedV2());
        super.setProxy(_proxyAddress);
        super._setInitializedV2();
    }

    function transfer(address _to, uint256 _value) public whenNotFrozen returns (bool success){
        return super.transfer(_to, _value);
    }

    function approve(address _spender, uint256 _value) public whenNotFrozen returns (bool success){
        return super.approve(_spender, _value);
    }

    function transferFrom(address _from, address _to, uint256 _value) public whenNotFrozen returns (bool success){
        return super.transferFrom(_from, _to, _value);
    }
}


