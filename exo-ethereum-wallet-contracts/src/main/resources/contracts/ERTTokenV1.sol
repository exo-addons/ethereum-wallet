pragma solidity ^0.4.24;
import "./TokenStorage.sol";
import "./Owned.sol";
import "./DataAccess.sol";
import "./Admin.sol";
import "./SafeMath.sol";
import "./Pausable.sol";
import "./ApprouvableAccount.sol";
import "./ERC20Abstract.sol";
import "./FundCollection.sol";
import "./GasPayableInToken.sol";
import "./Upgradability.sol";

/**
 * @title ERTTokenV1.sol
 * @dev ERC20 Token implementation
 */
contract ERTTokenV1 is 
  TokenStorage,
  Owned,
  DataAccess,
  Admin,
  SafeMath,
  Pausable,
  ApprouvableAccount,
  ERC20Abstract,
  FundCollection,
  GasPayableInToken,
  Upgradability {

    /**
     * @dev Sets the data address and proxy address if given
     * @param _dataAddress address of ERC20 Contract address (mandatory)
     * @param _proxyAddress optional proxy contract address
     */
    constructor(address _dataAddress, address _proxyAddress) public{
        require(_dataAddress != address(0));

        // Set data address in ERC20 implementation storage only
        uint16 dataVersion = 1;
        super.setDataAddress(dataVersion, _dataAddress);
        // The proxy will be 0x address for the whole first instantiation,
        // The future Token implementations should pass the correct proxy
        // address
        if (proxy != address(0)) {
          setProxy(_proxyAddress);
        }
    }

    /**
     * @dev initialize the ERC20 Token attributes when it's the first time that we deploy the first version of contract.
     * Once deployed, this method couldn''t be called again and shouldn't be inherited from future versions of Token
     * contracts
     * @param _proxyAddress proxy contract address
     * @param _initialAmount initial amount of tokens
     * @param _tokenName ERC20 token name
     * @param _decimalUnits token decimals
     * @param _tokenSymbol ERC20 token symbol
     */
    function initialize(address _proxyAddress, uint256 _initialAmount, string _tokenName, uint8 _decimalUnits, string _tokenSymbol) public onlyOwner{
        require(!super.initialized());

        super.setProxy(_proxyAddress);

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
        // Set owner as approved account
        super.approveAccount(msg.sender);

        super._setInitialized();
    }

    /**
     * @param _target The address from which the balance will be retrieved
     * @return the amount of balance of the address
     */
    function balanceOf(address _target) public view returns (uint256 balance){
        return super.balance(_target);
    }

    /**
     * @param _target The address of the account owning tokens
     * @param _spender The address of the account able to transfer the tokens
     * @return Amount of remaining tokens allowed to spent
     */
    function allowance(address _target, address _spender) public view returns (uint256 remaining){
        return super.getAllowance(_target, _spender);
    }

    /**
     * @dev send `_value` token to `_to` from `msg.sender`
     * @param _to The address of the recipient
     * @param _value The amount of token to be transferred
     * @return Whether the transfer was successful or not
     */
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

    /**
     * @dev `msg.sender` approves `_spender` to spend `_value` tokens
     * @param _spender The address of the account able to transfer the tokens
     * @param _value The amount of tokens to be approved for transfer
     * @return Whether the approval was successful or not
     */
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

    /**
     * @dev send `_value` token to `_to` from `_from` on the condition it is approved by `_from`
     * @param _from The address of the sender
     * @param _to The address of the recipient
     * @param _value The amount of token to be transferred
     * @return Whether the transfer was successful or not
     */
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
}