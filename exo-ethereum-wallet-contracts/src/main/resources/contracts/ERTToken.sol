pragma solidity ^0.4.24;
import './Proxy.sol';

contract ERTToken is Proxy {

    constructor(address implementation) public{
        isPayable = true;
        preserveContext = true;
        upgradeTo(implementation);
    }

}
