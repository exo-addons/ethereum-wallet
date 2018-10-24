pragma solidity ^0.4.24;
import "./ERC20Interface.sol";
import "./SafeMath.sol";

contract ERC20Abstract is SafeMath, ERC20Interface {

    mapping (address => uint256) public balances;

    mapping (address => mapping (address => uint256)) public allowed;

    string public name;

    string public symbol;

    uint256 public totalSupply;

    uint8 public decimals;

    constructor() internal{
    }

    function _transfer(address _from, address _to, uint _value) internal returns (bool){
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
        return true;
    }

}
