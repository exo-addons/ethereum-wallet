pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "./TestBurnable.sol";
import "./TestMintable.sol";

contract TestERTTokenV3 is TestERTTokenV2, TestBurnable,  TestMintable {

    constructor(address _dataAddressV1, address _dataAddressV2 ,address _proxyAddress) TestERTTokenV2(_dataAddressV1,_dataAddressV2,_proxyAddress) public{
        require(_dataAddressV2 != address(0));
        super._setDataAddress(2, _dataAddressV2);
    }

    function initialize(address _proxyAddress) public onlyOwner{
        super.setProxy(_proxyAddress);
    }

}


