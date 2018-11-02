pragma solidity ^0.4.24;

/*
 * The variables that are shared between implementation and Proxy contracts
 */
contract TokenStorage {

    /*
     * Made internal because this contract is abstract
     */
    constructor() internal{
    }

    // Reference to the Token implementation reference
    address public implementationAddress;

    // Proxy and Token owner
    address public owner;

    // Map of data contracts by version number
    mapping(uint16 => address) internal dataAddress;

    // A local variable for each Token instance to pause old and useless
    // implementations when upgrading to a newer Token implementation version
    bool public paused;
}
