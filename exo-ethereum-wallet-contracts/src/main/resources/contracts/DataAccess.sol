pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ERTTokenDataV1.sol";

/*
 * This contract will allow to access data using ERTTokenDataV1
 * Only current contract will be able to change data in ERTTokenData using restriction
 * added by contract DataOwned.sol where dataOwner = current contract address
 */
contract DataAccess is Owned{

    ERTTokenDataV1 internal ertTokenDataV1;

    function upgradeData(address _dataAddress) public onlyOwner{
        ertTokenDataV1 = ERTTokenDataV1(_dataAddress);
    }

    function initialized() public view returns(bool){
        return ertTokenDataV1.initialized();
    }

    function setInitialized(bool _initialized) internal onlyOwner{
        ertTokenDataV1.setInitialized(_initialized);
    }

    function name() public view returns(string){
        return ertTokenDataV1.name();
    }

    function setName(string _name) public onlyOwner{
        ertTokenDataV1.setName(_name);
    }

    function symbol() public view returns(string){
        return ertTokenDataV1.symbol();
    }

    function setSymbol(string _symbol) public onlyOwner{
        ertTokenDataV1.setSymbol(_symbol);
    }

    function totalSupply() public view returns(uint256){
        return ertTokenDataV1.totalSupply();
    }

    function setTotalSupply(uint256 _totalSupply) internal{
        ertTokenDataV1.setTotalSupply(_totalSupply);
    }

    function decimals() public view returns(uint8){
        return ertTokenDataV1.decimals();
    }

    function setDecimals(uint8 _decimals) internal{
        ertTokenDataV1.setDecimals(_decimals);
    }

    function balance(address _target) public view returns(uint256){
        return ertTokenDataV1.balance(_target);
    }

    function setBalance(address _target, uint256 _balance) internal{
        ertTokenDataV1.setBalance(_target, _balance);
    }

    function getAllowance(address _account, address _spender) public view returns (uint256){
        return ertTokenDataV1.getAllowance(_account, _spender);
    }

    function setAllowance(address _account, address _spender, uint256 _allowance) internal{
        ertTokenDataV1.setAllowance(_account, _spender, _allowance);
    }

    function isApprovedAccount(address _target) public view returns(bool){
        return ertTokenDataV1.isApprovedAccount(_target);
    }

    function setApprovedAccount(address _target, bool _approved) internal{
        ertTokenDataV1.setApprovedAccount(_target, _approved);
    }

    function getTokenPriceInGas() public view returns(uint256){
        return ertTokenDataV1.getTokenPriceInGas();
    }

    function setTokenPriceInGas(uint _tokenPriceInGas) public onlyOwner{
        ertTokenDataV1.setTokenPriceInGas(_tokenPriceInGas);
    }

    function getGasPriceLimit() public view returns(uint256){
        return ertTokenDataV1.getGasPriceLimit();
    }

    function setGasPriceLimit(uint _gasPriceLimit) public onlyOwner{
        ertTokenDataV1.setGasPriceLimit(_gasPriceLimit);
    }

    function isAdmin(address _target) public view returns(bool){
        return ertTokenDataV1.isAdmin(_target);
    }

    function setAdmin(address _target, bool _isAdmin) public onlyOwner{
        ertTokenDataV1.setAdmin(_target, _isAdmin);
    }

}
