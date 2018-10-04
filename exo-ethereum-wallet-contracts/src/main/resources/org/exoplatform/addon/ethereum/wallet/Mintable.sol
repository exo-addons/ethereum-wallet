pragma solidity ^0.4.25;

import "./ERC20Abstract.sol";
import "./SafeMath.sol";
import "./Owned.sol";

contract Mintable is ERC20Abstract, SafeMath, Owned {

    function mintToken(address target, uint256 mintedAmount) onlyOwner{
        super.balances[target] = safeAdd(super.balances[target], mintedAmount);
        require(super.totalSupply + mintedAmount > totalSupply);
        totalSupply += mintedAmount;
        emit Transfer(0, owner, mintedAmount);
        emit Transfer(owner, target, mintedAmount);
    }

}
