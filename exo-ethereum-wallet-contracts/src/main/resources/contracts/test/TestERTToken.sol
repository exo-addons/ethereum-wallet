pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "../ERTToken.sol";
import "./TestERTTokenV3.sol";

/*
 * @dev used to get its ABI only without deploying this contract
 */
contract TestERTToken is ERTToken, TestERTTokenV2, TestERTTokenV3 {

    constructor(address _dataAddressV1 ,address _proxyAddress) ERTToken(_proxyAddress, _dataAddressV1) public{
    }

}


