pragma solidity ^0.4.24;
import "./Proxy.sol";

contract ERTTokenData is Proxy {

    constructor(address implementation) public{
        isPayable = false;
        preserveContext = false;
        upgradeTo(implementation);
    }

}
