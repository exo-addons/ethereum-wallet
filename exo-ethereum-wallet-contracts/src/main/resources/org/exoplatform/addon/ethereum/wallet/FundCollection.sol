pragma solidity ^0.4.25;
import "./Owned.sol";
import "./ApprouvableBuyerAccount.sol";
import "./ERC20Abstract.sol";

contract FundCollection is Owned, ApprouvableBuyerAccount, ERC20Abstract {

    event FundsReceived(address from, uint amount);

    uint256 public sellPrice;
    uint256 public buyPrice;

    function setPrices(uint256 newSellPrice, uint256 newBuyPrice) onlyOwner{
        sellPrice = newSellPrice;
        buyPrice = newBuyPrice;
    }

    function buy() payable whenBuyerApproved(msg.sender) returns (uint amount){
        amount = msg.value / buyPrice;
        // Avoid idempotent operation
        if (msg.sender != super.owner) {
          _transfer(super.owner, msg.sender, amount);
        }
        emit FundsReceived(msg.sender, amount);
        return amount;
    }

}
