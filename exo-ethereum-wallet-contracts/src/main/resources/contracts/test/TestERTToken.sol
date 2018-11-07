pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "../ERTToken.sol";

/*
 * @dev used to get its ABI only without deploying this contract
 */
contract TestERTToken is ERTToken, TestERTTokenV2{

    constructor(address _dataAddressV1, address _dataAddressV2 ,address _proxyAddress) ERTToken(address(this), _dataAddressV1) TestERTTokenV2(_dataAddressV1, _dataAddressV2 ,_proxyAddress) public{
    }

}


