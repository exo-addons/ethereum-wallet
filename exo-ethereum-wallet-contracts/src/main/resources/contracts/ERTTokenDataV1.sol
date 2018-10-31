pragma solidity ^0.4.24;
import './Owned.sol';

contract ERTTokenDataV1 is Owned {

    /* ERC20Abstract.sol */
    mapping (address => uint256) internal balances_;

    mapping (address => mapping (address => uint256)) internal allowed_;

    string internal name_;

    string internal symbol_;

    uint256 internal totalSupply_;

    uint8 internal decimals_;

    bool internal initialized_ = false;

    /* ApprouvableAccount.sol */
    mapping (address => bool) internal approvedAccount_;
    
    /* Admin.sol */
    mapping (address => bool) internal admin_;

    /* GasPayableInToken.sol */
    uint256 internal tokenPriceInGas_;

    uint256 internal gasPriceLimit_ = 200000000000;

    constructor() public{
    }

    function initialized() public view returns(bool){
        return initialized_;
    }

    function setInitialized(bool _initialized) public onlyOwner{
        initialized_ = _initialized;
    }

    function name() public view returns(string){
        return name_;
    }

    function setName(string _name) public onlyOwner{
        name_ = _name;
    }

    function symbol() public view returns(string){
        return symbol_;
    }

    function setSymbol(string _symbol) public onlyOwner{
        symbol_ = _symbol;
    }

    function totalSupply() public view returns(uint256){
        return totalSupply_;
    }

    function setTotalSupply(uint256 _totalSupply) public onlyOwner{
        totalSupply_ = _totalSupply;
    }

    function decimals() public view returns(uint8){
        return decimals_;
    }

    function setDecimals(uint8 _decimals) public onlyOwner{
        decimals_ = _decimals;
    }

    function balance(address _target) public view returns(uint256){
        return balances_[_target];
    }

    function setBalance(address _target, uint256 _balance) public onlyOwner{
        balances_[_target] = _balance;
    }

    function getAllowance(address account, address spender) public view returns (uint256){
        return allowed_[account][spender];
    }

    function setAllowance(address account, address spender, uint256 allowance) public{
        allowed_[account][spender] = allowance;
    }

    function isApprovedAccount(address _target) public view returns(bool){
        return approvedAccount_[_target];
    }

    function setApprovedAccount(address _target, bool _approved) public onlyOwner{
        approvedAccount_[_target] = _approved;
    }

    function getTokenPriceInGas() public view returns(uint256){
        return tokenPriceInGas_;
    }

    function setTokenPriceInGas(uint _tokenPriceInGas) public onlyOwner{
        tokenPriceInGas_ = _tokenPriceInGas;
    }

    function getGasPriceLimit() public view returns(uint256){
        return gasPriceLimit_;
    }

    function setGasPriceLimit(uint _gasPriceLimit) public onlyOwner{
        gasPriceLimit_ = _gasPriceLimit;
    }
    
    function isAdmin(address _target) public view returns(bool){
        return admin_[_target];
    }

    function setAdmin(address _target, bool _admin) public onlyOwner{
        admin_[_target] = _admin;
    }

}
