pragma solidity ^0.4.24;

import "./ERTTokenDataV1.sol";
import "./Owned.sol";

contract ERTTokenDataProxy is Owned{

    address internal dataAddress;

    function upgradeData(address _dataAddress) public onlyOwner{
        dataAddress = _dataAddress;
    }

    function name() public view returns(string){
        return ERTTokenDataV1(dataAddress).name();
    }

    function setName(string _name) public {
        ERTTokenDataV1(dataAddress).setName(_name);
    }

    function symbol() public view returns(string){
        return ERTTokenDataV1(dataAddress).symbol();
    }

    function setSymbol(string _symbol) public {
        ERTTokenDataV1(dataAddress).setSymbol(_symbol);
    }

    function totalSupply() public view returns(uint256){
        return ERTTokenDataV1(dataAddress).totalSupply();
    }

    function setTotalSupply(uint256 _totalSupply) public {
        ERTTokenDataV1(dataAddress).setTotalSupply(_totalSupply);
    }

    function decimals() public view returns(uint8){
        return ERTTokenDataV1(dataAddress).decimals();
    }

    function setDecimals(uint8 _decimals) public {
        ERTTokenDataV1(dataAddress).setDecimals(_decimals);
    }

    function balance(address _target) public view returns(uint256){
        return ERTTokenDataV1(dataAddress).balance(_target);
    }

    function setBalance(address _target, uint256 _balance) public {
        ERTTokenDataV1(dataAddress).setBalance(_target, _balance);
    }

    function getAllowance(address _account, address _spender) public view returns (uint256){
        return ERTTokenDataV1(dataAddress).getAllowance(_account, _spender);
    }

    function setAllowance(address _account, address _spender, uint256 _allowance) public{
        ERTTokenDataV1(dataAddress).setAllowance(_account, _spender, _allowance);
    }

    function approvedAccount(address _target) public view returns(bool){
        return ERTTokenDataV1(dataAddress).isApprovedAccount(_target);
    }

    function setApprovedAccount(address _target, bool _approved) public {
        ERTTokenDataV1(dataAddress).setApprovedAccount(_target, _approved);
    }

    function getTokenPriceInGas() public view returns(uint){
        return ERTTokenDataV1(dataAddress).getTokenPriceInGas();
    }

    function setTokenPriceInGas(uint _tokenPriceInGas) public {
        ERTTokenDataV1(dataAddress).setTokenPriceInGas(_tokenPriceInGas);
    }

    function getGasPriceLimit() public view returns(uint){
        return ERTTokenDataV1(dataAddress).getGasPriceLimit();
    }

    function setGasPriceLimit(uint _gasPriceLimit) public {
        ERTTokenDataV1(dataAddress).setGasPriceLimit(_gasPriceLimit);
    }

}