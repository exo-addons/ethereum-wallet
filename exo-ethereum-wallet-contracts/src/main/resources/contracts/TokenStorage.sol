pragma solidity ^0.4.24;

contract TokenStorage {

    constructor() internal{
    }

    address internal implementationAddress;
    address internal owner;
    mapping(uint16 => address) internal dataAddress;
}
