pragma solidity ^0.4.24;
import "./TokenStorage.sol";
import "./ERC20Interface.sol";
import "./Owned.sol";
import "./DataAccess.sol";
import "./Admin.sol";
import "./Burnable.sol";
import "./SafeMath.sol";
import "./Pausable.sol";
import "./ApprouvableAccount.sol";
import "./ERC20Abstract.sol";
import "./FundCollection.sol";
import "./GasPayableInToken.sol";
import "./Mintable.sol";
import "./Upgradability.sol";

contract ERTTokenV1 is 
  TokenStorage,
  Owned,
  DataAccess,
  Admin,
  SafeMath,
  Burnable,
  Pausable,
  ApprouvableAccount,
  ERC20Interface,
  ERC20Abstract,
  FundCollection,
  GasPayableInToken,
  Mintable,
  Upgradability {

    constructor(address _dataAddress, address _proxyAddress) public{
        uint16 dataVersion = 1;
        super.setDataAddress(dataVersion, _dataAddress);
        // The proxy will be 0x address for the whole first instantiation,
        // The future Token implementations should pass the correct proxy
        // address
        if (proxy != address(0)) {
          setProxy(_proxyAddress);
        }
    }

    function initialize(address _proxyAddress, uint256 _initialAmount, string _tokenName, uint8 _decimalUnits, string _tokenSymbol) public onlyOwner{
        require(!super.initialized());

        setProxy(_proxyAddress);

        super.setName(_tokenName);
        super.setSymbol(_tokenSymbol);
        super._setDecimals(_decimalUnits);
        super._setTotalSupply(_initialAmount);

        super._setBalance(msg.sender, _initialAmount);
        // Default token price
        super.setTokenPrice(2 finney);
        // Set Maximum gas price to use in transactions that will refund
        // ether from contract
        super.setGasPriceLimit(0.000008 finney);
        // TODO consider when ownership transferred twice,
        // the old owner should always be approved at first
        super.approveAccount(msg.sender);

        super._setInitialized();
    }

    function balanceOf(address _owner) public view returns (uint256 balance){
        return super.balance(_owner);
    }

    function allowance(address _owner, address _spender) public view returns (uint256 remaining){
        return super.getAllowance(_owner, _spender);
    }

    function transfer(address _to, uint256 _value) public whenNotPaused whenApproved(msg.sender, _to) returns (bool success){
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
        uint gasLimit = gasleft();
        require(msg.sender != _spender);
        require(super.balance(msg.sender) >= _value);
        if (msg.sender == owner) {
            super.approveAccount(_spender);
        }
        super._setAllowance(msg.sender, _spender,_value);
        emit Approval(msg.sender, _spender, _value);
        super._payGasInToken(gasLimit);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public whenNotPaused whenApproved(_from, _to) returns (bool success){
        uint gasLimit = gasleft();
        require(super.balance(_from) >= _value);
        uint256 _allowance = super.getAllowance(_from, msg.sender);
        require(_allowance >= _value);
        super._setAllowance(_from, msg.sender, super.safeSubtract(_allowance, _value));
        if (msg.sender == owner) {
            super.approveAccount(_to);
        }
        require(super._transfer(_from, _to, _value) == true);
        emit Transfer(_from, _to, _value);
        super._payGasInToken(gasLimit);
        return true;
    }

    function _initialize(uint256 _initialAmount, string _tokenName, uint8 _decimalUnits, string _tokenSymbol) private{
        super.setName(_tokenName);
        super.setSymbol(_tokenSymbol);
        super._setDecimals(_decimalUnits);
        super._setTotalSupply(_initialAmount);

        super._setBalance(msg.sender, _initialAmount);
        // Default token price
        super.setTokenPrice(2 finney);
        // Set Maximum gas price to use in transactions that will refund
        // ether from contract
        super.setGasPriceLimit(0.000008 finney);
        // TODO consider when ownership transferred twice,
        // the old owner should always be approved at first
        super.approveAccount(msg.sender);
    }

}
