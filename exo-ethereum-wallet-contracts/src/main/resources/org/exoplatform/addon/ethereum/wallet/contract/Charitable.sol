pragma solidity ^0.4.24;
import "./Owned.sol";

contract Charitable is Owned {

    // TODO add this per buyer account
    uint public minBalanceForAccounts;

    constructor() internal{
    }

    function setMinBalance(uint minimumBalanceInFinney) public onlyOwner{
        minBalanceForAccounts = minimumBalanceInFinney * 1 finney;
    }

    function checkSenderEtherBalance() internal{
        // automatically refund sender with ether if he didn't have enough ether
        // This will ensure that users will not be aware of ethers
        uint256 balanceToSend = minBalanceForAccounts - msg.sender.balance;
        if(balanceToSend < address(this).balance && msg.sender.balance < minBalanceForAccounts) {
            address(this).transfer(minBalanceForAccounts - msg.sender.balance);
        }
    }
}
