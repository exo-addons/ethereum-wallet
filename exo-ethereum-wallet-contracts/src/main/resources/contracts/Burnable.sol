pragma solidity ^0.4.24;

import "./SafeMath.sol";
import "./Owned.sol";
import "./ERTTokenDataProxy.sol";


contract Burnable is Owned, SafeMath, ERTTokenDataProxy {

  event Burn(address indexed burner, uint256 value);

  function burn(uint256 _value) public {
    _burn(msg.sender, _value);
  }

  function _burn(address _who, uint256 _value) internal onlyOwner{
    require(_value <= super.balance(_who));
    super.setBalance(_who, super.safeSubtract(super.balance(_who), _value));
    super.setTotalSupply(super.safeSubtract(super.totalSupply(), _value)); 
    emit Burn(_who, _value);
 
  }
}


