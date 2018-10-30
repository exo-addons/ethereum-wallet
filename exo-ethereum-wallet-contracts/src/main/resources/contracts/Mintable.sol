pragma solidity ^0.4.24;
import "./ERC20Abstract.sol";
import "./ERC20Interface.sol";
import "./SafeMath.sol";
import "./Owned.sol";
import "./ERTTokenDataProxy.sol";

contract Mintable is Owned, ERTTokenDataProxy, SafeMath, ERC20Abstract {

    function mintToken(address target, uint256 mintedAmount) public onlyOwner{
        super.setBalance(target, super.safeAdd(super.balance(target), mintedAmount));
        uint256 totalSupply = super.totalSupply();
        require(totalSupply + mintedAmount > totalSupply);
        super.setTotalSupply(totalSupply + mintedAmount);
        emit Transfer(0, owner, mintedAmount);
        emit Transfer(owner, target, mintedAmount);
    }

}
