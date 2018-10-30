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

    /* ApprouvableAccount.sol */
    mapping (address => bool) internal approvedAccount_;

    /* GasPayableInToken.sol */
    uint internal tokenPriceInGas_;

    uint internal gasPriceLimit_;

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

    function getTokenPriceInGas() public view returns(uint){
        return tokenPriceInGas_;
    }

    function setTokenPriceInGas(uint _tokenPriceInGas) public onlyOwner{
        tokenPriceInGas_ = _tokenPriceInGas;
    }

    function getGasPriceLimit() public view returns(uint){
        return gasPriceLimit_;
    }

    function setGasPriceLimit(uint _gasPriceLimit) public onlyOwner{
        gasPriceLimit_ = _gasPriceLimit;
    }

}
