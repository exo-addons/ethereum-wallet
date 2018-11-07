pragma solidity ^0.4.24;
import "../SafeMath.sol";
import "../Owned.sol";
import "../DataAccess.sol";


/**
 * @title Burnable.sol
 * @dev This is an abstract contract that is used to burn tokens from owner account
 */
contract TestBurnable is Owned, DataAccess, SafeMath {

    // Event emitted when the owner burnt some tokens
    event Burn(address burner, uint256 value);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev Burn tokens from owner account
     * @param _value amount of tokens to burn
     */
    function burn(uint256 _value) public onlyOwner{
        uint256 ownerBalance = super.balance(msg.sender);
        require(ownerBalance >= _value);
        super._setBalance(msg.sender, super.safeSubtract(ownerBalance, _value));
        super._setTotalSupply(super.safeSubtract(super.totalSupply(), _value));
        emit Burn(msg.sender, _value);
    }
}
