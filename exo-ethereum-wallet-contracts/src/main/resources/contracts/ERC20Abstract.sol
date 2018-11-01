pragma solidity ^0.4.24;
import "./ERC20Interface.sol";
import "./SafeMath.sol";
import "./DataAccess.sol";

/*
 * @title ERC20Abstract
 * An abstract contract to define internally a common operation
 * with a common logic for transfer operation
 */
contract ERC20Abstract is DataAccess, SafeMath, ERC20Interface {

    /*
     * Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /*
     * @dev Transfers an amount of ERC20 tokens from an address to another.
     * @param _from address of sender
     * @param _to address of receiver
     * @param _value amount of tokens to transfer
     * @return true if the transfer completed successfully
     */
    function _transfer(address _from, address _to, uint _value) internal returns (bool){
        // Prevent transfer transaction with no tokens
        require(_value > 0);
        // Prevent transfer to 0x0 address. Use burn() instead
        require(_to != 0x0);
        // Check if the sender has enough
        uint256 fromBalance = super.balance(_from);
        require(fromBalance >= _value);
        // Subtract from the sender
        super.setBalance(_from, safeSubtract(fromBalance, _value));
        // Add the same to the recipient
        super.setBalance(_to, safeAdd(super.balance(_to), _value));
        return true;
    }

}
