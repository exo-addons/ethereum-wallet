pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "../ERTToken.sol";
import "./TestERTTokenV3.sol";

/*
 * @dev used to get its ABI only without deploying this contract
 */
contract TestERTToken is ERTToken, TestERTTokenV3 {

    constructor(address _implementationAddress, address _dataAddress) ERTToken(_implementationAddress, _dataAddress) public{
    }

}


