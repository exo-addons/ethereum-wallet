pragma solidity ^0.4.24;
import './TokenStorage.sol';

contract Owned is TokenStorage {

    event TransferOwnership(address newOwner);
    event TransferProxyOwnership(address proxy);

    constructor() internal{
        owner = msg.sender;
    }

    modifier onlyOwner(){
        require(msg.sender == owner);
        _;
    }

    modifier onlyProxy(){
        require(msg.sender == proxy);
        _;
    }

    function getOwner() public view returns(address){
        return owner;
    }

    function transferOwnership(address _newOwner) public onlyOwner{
        if (_newOwner != address(0)) {
            owner = _newOwner;
            emit TransferOwnership(_newOwner);
        }
    }

    function setProxy(address _proxy) public onlyOwner{
        require(_proxy != address(0));
        proxy = _proxy;
        emit TransferProxyOwnership(_proxy);
    }
}
