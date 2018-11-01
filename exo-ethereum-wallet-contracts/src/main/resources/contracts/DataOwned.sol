pragma solidity ^0.4.24;

contract DataOwned {
    event TransferOwnership(address proxyAddress, address implementationAddress);

    address public proxyAddress;
    address public implementationAddress;

    constructor() internal{
        // Keep 
        implementationAddress = msg.sender;
    }

    modifier onlyOwner(){
        address sender = msg.sender;
        require(sender == proxyAddress || sender == implementationAddress);
        _;
    }

    function transferDataOwnership(address proxyAddress_, address implementationAddress_) public onlyOwner{
        require (proxyAddress_ != address(0) && implementationAddress_ != address(0));
        proxyAddress = proxyAddress_;
        implementationAddress = implementationAddress_;
        emit TransferOwnership(proxyAddress_, implementationAddress_);
    }
}
