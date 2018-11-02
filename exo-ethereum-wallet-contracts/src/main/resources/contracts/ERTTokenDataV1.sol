pragma solidity ^0.4.24;
import './DataOwned.sol';

contract ERTTokenDataV1 is DataOwned {

    /* ERC20Abstract.sol */
    mapping (address => uint256) internal balances_;

    mapping (address => mapping (address => uint256)) internal allowed_;

    string internal name_;

    string internal symbol_;

    uint256 internal totalSupply_;

    uint8 internal decimals_;

    bool internal initialized_ = false;

    bool internal paused_ = false;

    /* ApprouvableAccount.sol */
    mapping (address => bool) internal approvedAccount_;

    // Used in Admin.sol
    // Map of admin address => administration level
    mapping (address => uint8) internal admin_;

    /* GasPayableInToken.sol */
    uint256 internal gasPriceInToken_;

    uint256 internal gasPriceLimit_;

    constructor() public{
    }

    /* View methods publically accessible */

    function initialized() public view returns(bool){
        return initialized_;
    }

    function name() public view returns(string){
        return name_;
    }

    function symbol() public view returns(string){
        return symbol_;
    }

    function totalSupply() public view returns(uint256){
        return totalSupply_;
    }

    function balance(address _target) public view returns(uint256){
        return balances_[_target];
    }

    function decimals() public view returns(uint8){
        return decimals_;
    }

    function getAllowance(address account, address spender) public view returns (uint256){
        return allowed_[account][spender];
    }

    function isApprovedAccount(address _target) public view returns(bool){
        return approvedAccount_[_target];
    }

    function getGasPriceInToken() public view returns(uint256){
        return gasPriceInToken_;
    }

    function getGasPriceLimit() public view returns(uint256){
        return gasPriceLimit_;
    }

    function isAdmin(address _target, uint8 level) public view returns(bool){
        return admin_[_target] >= level;
    }

    function isPaused() public view returns (bool){
        return paused_;
    }

    /* Write methods accessible from Token Implementation Contract only */

    function setInitialized() public onlyOwner{
        initialized_ = true;
    }

    function setPaused(bool _paused) public onlyOwner{
        paused_ = _paused;
    }

    function setName(string _name) public onlyOwner{
        name_ = _name;
    }

    function setSymbol(string _symbol) public onlyOwner{
        symbol_ = _symbol;
    }

    function setTotalSupply(uint256 _totalSupply) public onlyOwner{
        totalSupply_ = _totalSupply;
    }

    function setBalance(address _target, uint256 _balance) public onlyOwner{
        balances_[_target] = _balance;
    }

    function setDecimals(uint8 _decimals) public onlyOwner{
        decimals_ = _decimals;
    }

    function setAllowance(address account, address spender, uint256 allowance) public onlyOwner{
        allowed_[account][spender] = allowance;
    }

    function setApprovedAccount(address _target, bool _approved) public onlyOwner{
        approvedAccount_[_target] = _approved;
    }

    function setGasPriceInToken(uint256 _gasPriceInToken) public onlyOwner{
        gasPriceInToken_ = _gasPriceInToken;
    }

    function setGasPriceLimit(uint _gasPriceLimit) public onlyOwner{
        gasPriceLimit_ = _gasPriceLimit;
    }

    function setAdmin(address _target, uint8 _level) public onlyOwner{
        admin_[_target] = _level;
    }

}
