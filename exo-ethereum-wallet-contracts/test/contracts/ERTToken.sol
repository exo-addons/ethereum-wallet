pragma solidity ^0.4.24;
import "./ERC20Interface.sol";
import "./Owned.sol";
import "./SafeMath.sol";
import "./Pausable.sol";
import "./FreezableAccount.sol";
import "./ApprouvableAccount.sol";
import "./Charitable.sol";
import "./ERC20Abstract.sol";
import "./Mintable.sol";
import "./Withdrawable.sol";
import "./FundCollection.sol";

contract ERTToken is 
  Owned,
  SafeMath,
  Pausable,
  FreezableAccount,
  ApprouvableAccount,
  Charitable,
  ERC20Interface,
  ERC20Abstract,
  Mintable,
  Withdrawable,
  FundCollection {

    constructor(uint256 _initialAmount, string _tokenName, uint8 _decimalUnits, string _tokenSymbol) public{
        balances[msg.sender] = _initialAmount;
        totalSupply = _initialAmount;
        name = _tokenName;
        decimals = _decimalUnits;
        symbol = _tokenSymbol;
    }

    function balanceOf(address _owner) public view returns (uint256 balance){
        return balances[_owner];
    }

    function allowance(address _owner, address _spender) public view returns (uint256 remaining){
        return allowed[_owner][_spender];
    }

    function transfer(address _to, uint256 _value) public whenNotFrozen whenApproved(msg.sender, _to) returns (bool success){
        // Make sure that this is not about a fake transaction
        require(msg.sender != _to);
        if (msg.sender == owner) {
          super.approveAccount(_to);
        }
        // This is to avoid calling this function with empty tokens transfer
        // If the user doesn't have enough ethers, he will simply reattempt with empty tokens
        super._transfer(msg.sender, _to, _value);
        // TODO use external contract call for this
        // super.checkSenderEtherBalance();
        return true;
    }

    function approve(address _spender, uint256 _value) public whenNotFrozen whenApproved(msg.sender, _spender) returns (bool success){
        // Make sure that this is not about a fake transaction
        require(msg.sender != _spender);
        require(balances[msg.sender] >= _value);
        if (msg.sender == owner) {
          super.approveAccount(_spender);
        }
        allowed[msg.sender][_spender] = _value;
        emit Approval(msg.sender, _spender, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public whenNotFrozen whenApproved(_from, _to) returns (bool success){
        require(balances[_from] >= _value);
        require(allowed[_from][msg.sender] >= _value);
        allowed[_from][msg.sender] = safeSubtract(allowed[_from][msg.sender], _value);
        if (msg.sender == owner) {
          super.approveAccount(_to);
        }
        super._transfer(_from, _to, _value);
        // super.checkSenderEtherBalance();
        return true;
    }
}
