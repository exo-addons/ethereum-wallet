pragma solidity ^0.4.24;
import "./Owned.sol";

contract FundCollection is Owned {

    event DepositReceived(address from, uint amount);

    constructor() internal{
    }

    function() public payable onlyOwner {
        require(msg.data.length == 0);
        require(msg.value > 0);
        emit DepositReceived(msg.sender, msg.value);
    }
}
