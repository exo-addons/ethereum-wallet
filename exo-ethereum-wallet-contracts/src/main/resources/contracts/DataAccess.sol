pragma solidity ^0.4.24;
import "./TokenStorage.sol";
import "./Owned.sol";
import "./ERTTokenDataV1.sol";

/*
 * This contract will allow to access data using ERTTokenDataV1
 * Only current contract will be able to change data in ERTTokenData using restriction
 * added by contract DataOwned.sol where dataOwner = current contract address
 */
contract DataAccess is TokenStorage, Owned{

    constructor() internal{
    }

    /* View methods */

    function getDataAddress(uint16 version) public view returns(address){
        return dataAddress[version];
    }

    function initialized() public view returns(bool){
        return ERTTokenDataV1(dataAddress[1]).initialized();
    }

    function name() public view returns(string){
        return ERTTokenDataV1(dataAddress[1]).name();
    }

    function symbol() public view returns(string){
        return ERTTokenDataV1(dataAddress[1]).symbol();
    }

    function totalSupply() public view returns(uint256){
        return ERTTokenDataV1(dataAddress[1]).totalSupply();
    }

    function decimals() public view returns(uint8){
        return ERTTokenDataV1(dataAddress[1]).decimals();
    }

    function balance(address _target) public view returns(uint256){
        return ERTTokenDataV1(dataAddress[1]).balance(_target);
    }

    function getAllowance(address _account, address _spender) public view returns (uint256){
        return ERTTokenDataV1(dataAddress[1]).getAllowance(_account, _spender);
    }

    function isApprovedAccount(address _target) public view returns(bool){
        return ERTTokenDataV1(dataAddress[1]).isApprovedAccount(_target);
    }

    function getTokenPriceInGas() public view returns(uint256){
        return ERTTokenDataV1(dataAddress[1]).getTokenPriceInGas();
    }

    function getGasPriceLimit() public view returns(uint256){
        return ERTTokenDataV1(dataAddress[1]).getGasPriceLimit();
    }

    function isAdmin(address _target) public view returns(bool){
        return ERTTokenDataV1(dataAddress[1]).isAdmin(_target);
    }

    function isPaused() public view returns (bool){
        return ERTTokenDataV1(dataAddress[1]).isPaused();
    }

    /* Public owner write methods */

    function setName(string _name) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).setName(_name);
    }

    function setTokenPriceInGas(uint _tokenPriceInGas) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).setTokenPriceInGas(_tokenPriceInGas);
    }

    function setGasPriceLimit(uint _gasPriceLimit) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).setGasPriceLimit(_gasPriceLimit);
    }

    function setAdmin(address _target, bool _isAdmin) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).setAdmin(_target, _isAdmin);
    }

    function setSymbol(string _symbol) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).setSymbol(_symbol);
    }

    function setDataAddress(uint16 dataVersion_, address dataAddress_) public onlyOwner{
        require(dataAddress[dataVersion_] == address(0));
        dataAddress[dataVersion_] = dataAddress_;
    }

    function transferDataOwnership(address proxyAddress_, address implementationAddress_) public onlyOwner{
        ERTTokenDataV1(dataAddress[1]).transferDataOwnership(proxyAddress_, implementationAddress_);
    }

    /* Internal write methods */

    function setInitialized(bool _initialized) internal{
        ERTTokenDataV1(dataAddress[1]).setInitialized(_initialized);
    }

    function setPaused(bool _paused) internal{
        ERTTokenDataV1(dataAddress[1]).setPaused(_paused);
    }

    function setTotalSupply(uint256 _totalSupply) internal{
        ERTTokenDataV1(dataAddress[1]).setTotalSupply(_totalSupply);
    }

    function setDecimals(uint8 _decimals) internal{
        ERTTokenDataV1(dataAddress[1]).setDecimals(_decimals);
    }

    function setBalance(address _target, uint256 _balance) internal{
        ERTTokenDataV1(dataAddress[1]).setBalance(_target, _balance);
    }

    function setAllowance(address _account, address _spender, uint256 _allowance) internal{
        ERTTokenDataV1(dataAddress[1]).setAllowance(_account, _spender, _allowance);
    }

    function setApprovedAccount(address _target, bool _approved) internal{
        ERTTokenDataV1(dataAddress[1]).setApprovedAccount(_target, _approved);
    }

}
