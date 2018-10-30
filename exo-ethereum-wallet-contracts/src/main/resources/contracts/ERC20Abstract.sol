pragma solidity ^0.4.24;
import "./ERC20Interface.sol";
import "./SafeMath.sol";
import "./ERTTokenDataProxy.sol";

contract ERC20Abstract is ERTTokenDataProxy, SafeMath, ERC20Interface {

    constructor() internal{
    }

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
