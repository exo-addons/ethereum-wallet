pragma solidity ^0.4.24;
import "./ERC20Abstract.sol";
import "./SafeMath.sol";
import "./Owned.sol";

/**
 * @title Mintable.sol
 * @dev Abstract contract to mint tokens
 */
contract Mintable is Owned, SafeMath, ERC20Abstract {

    // Event emitted when tokens are minted
    event MintedToken(address minter, address target, uint256 mintedAmount);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev Mint tokens to target address
     * @param _target target account to mint for
     * @param _mintedAmount added amount of tokens for the target account
     */
    function mintToken(address _target, uint256 _mintedAmount) public onlyOwner{
        require(_mintedAmount > 0);

        super._setBalance(_target, super.safeAdd(super.balance(_target), _mintedAmount));
        uint256 totalSupply = super.totalSupply();
        require(totalSupply + _mintedAmount > totalSupply);
        super._setTotalSupply(totalSupply + _mintedAmount);
        emit Transfer(0, owner, _mintedAmount);
        emit Transfer(owner, _target, _mintedAmount);
        emit MintedToken(msg.sender, _target, _mintedAmount);
    }

}
