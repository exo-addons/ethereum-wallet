pragma solidity ^0.4.25;

contract Owned {
    event TransferOwnership(address newOwner);

    address public owner;

    constructor(){
        owner = msg.sender;
    }

    modifier onlyOwner(){
        require(msg.sender == owner);
        _;
    }

    function transferOwnership(address _newOwner) onlyOwner{
        if (_newOwner != address(0)) {
            owner = _newOwner;
            TransferOwnership(_newOwner);
        }
    }
}
