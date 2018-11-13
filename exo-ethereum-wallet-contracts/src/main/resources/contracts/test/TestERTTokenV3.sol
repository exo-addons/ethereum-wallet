pragma solidity ^0.4.24;
import "./TestERTTokenV2.sol";
import "./TestBurn.sol";
import "./TestMint.sol";


contract TestERTTokenV3 is TestERTTokenV2, TestBurn, TestMint {

    constructor(address _proxyAddress)  TestERTTokenV2(_proxyAddress)  public{
    }
}


