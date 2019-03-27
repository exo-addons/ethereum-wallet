pragma solidity >=0.4.24;
import "../ERTToken.sol";
import "./TestTokenNewerVersion.sol";

/*
 * @dev used to get its ABI only without deploying this contract
 */
contract TestERTToken is ERTToken, TestTokenNewerVersion {

    constructor(address _implementationAddress, address _dataAddress) ERTToken(_implementationAddress, _dataAddress) public{
    }

}


