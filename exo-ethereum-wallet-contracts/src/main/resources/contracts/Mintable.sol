pragma solidity ^0.4.24;
import "./ERC20Abstract.sol";
import "./ERC20Interface.sol";
import "./SafeMath.sol";
import "./Owned.sol";

contract Mintable is Owned, SafeMath, ERC20Abstract {

    function mintToken(address target, uint256 mintedAmount) public onlyOwner{
        balances[target] = super.safeAdd(balances[target], mintedAmount);
        require(totalSupply + mintedAmount > totalSupply);
        totalSupply += mintedAmount;
        emit Transfer(0, owner, mintedAmount);
        emit Transfer(owner, target, mintedAmount);
    }

}
