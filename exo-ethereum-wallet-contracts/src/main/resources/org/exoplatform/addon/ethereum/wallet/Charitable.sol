pragma solidity ^0.4.25;
import "./Owned.sol";

contract Charitable is Owned {

    // TODO add this per buyer account
    uint public minBalanceForAccounts;

    function setMinBalance(uint minimumBalanceInFinney) onlyOwner{
        minBalanceForAccounts = minimumBalanceInFinney * 1 finney;
    }

    function checkSenderEtherBalance() private {
        // automatically refund sender with ether if he didn't have enough ether
        // This will ensure that users will not be aware of ethers
        uint256 balanceToSend = minBalanceForAccounts - msg.sender.balance;
        if(balanceToSend < this.balance && msg.sender.balance < minBalanceForAccounts) {
            this.transfer(minBalanceForAccounts - msg.sender.balance);
        }
    }
}
