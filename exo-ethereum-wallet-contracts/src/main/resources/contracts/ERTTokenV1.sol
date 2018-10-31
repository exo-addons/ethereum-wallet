pragma solidity ^0.4.24;
import "./ERC20Interface.sol";
import "./Owned.sol";
import "./ERTTokenDataProxy.sol";
import "./SafeMath.sol";
import "./Pausable.sol";
import "./ApprouvableAccount.sol";
import "./ERC20Abstract.sol";
import "./FundCollection.sol";
import "./GasPayableInToken.sol";
import "./Mintable.sol";

contract ERTTokenV1 is 
  Owned,
  ERTTokenDataProxy,
  SafeMath,
  Pausable,
  ApprouvableAccount,
  ERC20Interface,
  ERC20Abstract,
  FundCollection,
  GasPayableInToken,
  Mintable {

    string public constant VERSION = "1.0.0";

    constructor(address _dataAddress, uint256 _initialAmount, string _tokenName, uint8 _decimalUnits, string _tokenSymbol) public{
        super.upgradeData(_dataAddress);
        super.setName(_tokenName);
        super.setDecimals(_decimalUnits);
        super.setSymbol(_tokenSymbol);
        super.setTotalSupply(_initialAmount);
        super.setBalance(msg.sender, _initialAmount);
        super.setTokenPriceInGas(10 ** (uint(_decimalUnits) - 6));
        // TODO consider when ownership transferred twice,
        // the old owner should always be approved at first
        super.approveAccount(msg.sender);
    }

    function balanceOf(address _owner) public view returns (uint256 balance){
        return super.balance(_owner);
    }

    function allowance(address _owner, address _spender) public view returns (uint256 remaining){
        return super.getAllowance(_owner, _spender);
    }

    function transfer(address _to, uint256 _value) public whenNotPaused notExcessiveGasPrice whenApproved(msg.sender, _to) returns (bool success){
        uint gasLimit = gasleft();
        // Make sure that this is not about a fake transaction
        require(msg.sender != _to);
        if (msg.sender == owner) {
            super.approveAccount(_to);
        }
        // This is to avoid calling this function with empty tokens transfer
        // If the user doesn't have enough ethers, he will simply reattempt with empty tokens
        require(super._transfer(msg.sender, _to, _value) == true);
        emit Transfer(msg.sender, _to, _value);
        super._payGasInToken(gasLimit);
        return true;
    }

    function approve(address _spender, uint256 _value) public whenNotPaused whenApproved(msg.sender, _spender) returns (bool success){
        // Make sure that this is not about a fake transaction
        require(msg.sender != _spender);
        require(super.balance(msg.sender) >= _value);
        if (msg.sender == owner) {
            super.approveAccount(_spender);
        }
        super.setAllowance(msg.sender, _spender,_value);
        emit Approval(msg.sender, _spender, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public whenNotPaused whenApproved(_from, _to) returns (bool success){
        require(super.balance(_from) >= _value);
        uint256 _allowance = super.getAllowance(_from, msg.sender);
        require(_allowance >= _value);
        super.setAllowance(_from, msg.sender, safeSubtract(_allowance, _value));
        if (msg.sender == owner) {
            super.approveAccount(_to);
        }
        require(super._transfer(_from, _to, _value) == true);
        emit Transfer(_from, _to, _value);
        return true;
    }
}
