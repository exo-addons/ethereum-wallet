pragma solidity ^0.4.24;
import "./ERC20Abstract.sol";
import "./Owned.sol";
import "./SafeMath.sol";

contract Withdrawable is Owned, SafeMath, ERC20Abstract {
    event Withdrawed(address target, uint amount);

    function withdraw(address target) public onlyOwner{
        uint tokenBalance = balances[target];
        require(tokenBalance > 0);
        uint balanceToWithdraw = super.safeMult(tokenBalance, address(this).balance / totalSupply);

        balances[target] = 0;
        balances[msg.sender] = super.safeAdd(balances[msg.sender], tokenBalance);
        address(this).transfer(balanceToWithdraw);
        emit Withdrawed(target, balanceToWithdraw);
    }

}
