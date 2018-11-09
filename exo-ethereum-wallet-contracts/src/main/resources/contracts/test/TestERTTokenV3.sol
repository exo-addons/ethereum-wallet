pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "./TestBurnable.sol";
import "./TestMintable.sol";

contract TestERTTokenV3 is TestERTTokenV2, TestBurnable,  TestMintable {

    constructor(address _dataAddressV1, address _dataAddressV2 ,address _proxyAddress)  TestERTTokenV2(_dataAddressV1,_dataAddressV2,_proxyAddress) public
    {
     super.setProxy(_proxyAddress);
    }

 function transfer(address _to, uint256 _value) public whenNotFrozen returns (bool success){
        return super.transfer(_to, _value);
    }

}


