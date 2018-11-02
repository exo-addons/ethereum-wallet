pragma solidity ^0.4.24;
import "./ERC20Abstract.sol";
import "./ERC20Interface.sol";
import "./SafeMath.sol";
import "./Owned.sol";

contract Mintable is Owned, SafeMath, ERC20Abstract {

    event MintedToken(address minter, address target, uint256 mintedAmount);

    function mintToken(address target, uint256 mintedAmount) public onlyOwner{
        super._setBalance(target, super.safeAdd(super.balance(target), mintedAmount));
        uint256 totalSupply = super.totalSupply();
        require(totalSupply + mintedAmount > totalSupply);
        super._setTotalSupply(totalSupply + mintedAmount);
        emit Transfer(0, owner, mintedAmount);
        emit Transfer(owner, target, mintedAmount);
        emit MintedToken(msg.sender, target, mintedAmount);
    }

}
