pragma solidity ^0.4.24;

contract Owned {
    event TransferOwnership(address newOwner);

    address public owner;

    constructor() internal{
        owner = msg.sender;
    }

    modifier onlyOwner(){
        require(msg.sender == owner);
        _;
    }

    function transferOwnership(address _newOwner) public onlyOwner{
        if (_newOwner != address(0)) {
            owner = _newOwner;
            emit TransferOwnership(_newOwner);
        }
    }
}
