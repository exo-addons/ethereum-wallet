pragma solidity ^0.4.25;
import "./Owned.sol";
import "./ERC20Abstract.sol";
import "./SafeMath.sol";

contract Withdrawable is ERC20Abstract, Owned, SafeMath {

    function withdraw(address target) public onlyOwner{
        uint tokenBalance = super.balances[target];
        require(tokenBalance > 0);

        uint balanceToWithdraw = super.safeMult(tokenBalance, this.balance / super.totalSupply);

        super.balances[target] = 0;
        super.balances[msg.sender] = super.safeAdd(super.balances[msg.sender], tokenBalance);

        this.transfer(balanceToWithdraw);
    }

}
