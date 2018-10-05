pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ApprouvableBuyerAccount.sol";
import "./ERC20Abstract.sol";

contract FundCollection is Owned, ApprouvableBuyerAccount, ERC20Abstract {

    event FundsReceived(address from, uint amount);

    uint256 public sellPrice;
    uint256 public buyPrice;

    function setPrices(uint256 newSellPrice, uint256 newBuyPrice) public onlyOwner{
        sellPrice = newSellPrice;
        buyPrice = newBuyPrice;
    }

    function buy() payable public whenBuyerApproved(msg.sender) returns (uint amount){
        amount = msg.value / buyPrice;
        // Avoid idempotent operation
        if (msg.sender != owner) {
            super._transfer(owner, msg.sender, amount);
        }
        emit FundsReceived(msg.sender, amount);
        return amount;
    }

}
