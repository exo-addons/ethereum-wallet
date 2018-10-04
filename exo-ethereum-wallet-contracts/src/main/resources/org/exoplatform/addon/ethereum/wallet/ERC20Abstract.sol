pragma solidity ^0.4.25;

import "./ERC20Interface.sol";
import "./SafeMath.sol";

contract ERC20Abstract is ERC20Interface, SafeMath {

    mapping (address => uint256) public balances;

    mapping (address => mapping (address => uint256)) public allowed;

    string public name;

    uint8 public decimals;

    string public symbol;

    // This generates a public event on the blockchain that will notify clients
    event Transfer(address indexed from, address indexed to, uint256 value);

    /**
     * Internal transfer, only can be called by this contract
     */
    function _transfer(address _from, address _to, uint _value) private {
        // Prevent transfer transaction with no tokens
        require(_value > 0);
        // Prevent transfer to 0x0 address. Use burn() instead
        require(_to != 0x0);
        // Check if the sender has enough
        require(balances[_from] >= _value);
        // Subtract from the sender
        balances[_from] = safeSubtract(balances[_from], _value);
        // Add the same to the recipient
        balances[_to] = safeAdd(balances[_to], _value);
        emit Transfer(_from, _to, _value);
    }

}
