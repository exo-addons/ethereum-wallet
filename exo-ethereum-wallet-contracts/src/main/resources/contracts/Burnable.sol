pragma solidity ^0.4.24;
import "./SafeMath.sol";
import "./Owned.sol";
import "./DataAccess.sol";


contract Burnable is Owned, DataAccess, SafeMath {

    event Burn(address indexed burner, uint256 value);

    constructor() internal{
    }

    function burn(uint256 _value) public onlyOwner{
        uint256 ownerBalance = super.balance(msg.sender);
        require(ownerBalance >= _value);
        super.setBalance(msg.sender, super.safeSubtract(ownerBalance, _value));
        super.setTotalSupply(super.safeSubtract(super.totalSupply(), _value));
        emit Burn(msg.sender, _value);
    }
}


