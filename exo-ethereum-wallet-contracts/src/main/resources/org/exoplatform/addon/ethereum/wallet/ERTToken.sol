pragma solidity ^0.4.25;
import "./ERC20Abstract.sol";
import "./SafeMath.sol";
import "./Owned.sol";
import "./Pausable.sol";
import "./FreezableAccount.sol";
import "./ApprouvableAccount.sol";
import "./Withdrawable.sol";
import "./FundCollection.sol";
import "./Charitable.sol";
import "./Mintable.sol";

contract ERTToken is ERC20Abstract, SafeMath, Owned, Pausable, FreezableAccount, ApprouvableAccount, Withdrawable, FundCollection, Charitable, Mintable {

    constructor(uint256 _initialAmount,
        string _tokenName,
        uint8 _decimalUnits,
        string _tokenSymbol) public{
        balances[msg.sender] = _initialAmount;
        totalSupply = _initialAmount;
        name = _tokenName;
        decimals = _decimalUnits;
        symbol = _tokenSymbol;
        approveAccount(msg.sender);
    }

    function transfer(address _to, uint256 _value) public whenNotFrozen whenApproved(_to) returns (bool success){
        approveAccount(_to);
        // This is to avoid calling this function with empty tokens transfer
        // If the user doesn't have enough ethers, he will simply 
        _transfer(msg.sender, _to, _value);
        // TODO use external contract call for this
        checkSenderEtherBalance();
        return true;
    }

    function approve(address _spender, uint256 _value) public whenNotFrozen whenApproved(_spender) returns (bool success){
        approveAccount(_spender);
        allowed[msg.sender][_spender] = _value;
        emit Approval(msg.sender, _spender, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public whenNotFrozen whenApproved(_to) returns (bool success){
        approveAccount(_to);
        allowed[_from] = safeSubtract(allowed[_from][msg.sender], _value);
        _transfer(msg.sender, _to, _value);
        checkSenderEtherBalance();
        return true;
    }

    function balanceOf(address _owner) public view returns (uint256 balance){
        return balances[_owner];
    }

    function allowance(address _owner, address _spender) public view returns (uint256 remaining){
        return allowed[_owner][_spender];
    }

}
