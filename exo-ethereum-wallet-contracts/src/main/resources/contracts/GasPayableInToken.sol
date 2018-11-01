pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ERC20Abstract.sol";

contract GasPayableInToken is Owned, ERC20Abstract {

    event TransactionFee(address from, uint tokenFee, uint etherFeeRefund);
    event NoSufficientFund(uint balance);

    constructor() internal{
    }

    /*
     * This is to avoid refunding a lot of ethers from contract
     */
    modifier notExcessiveGasPrice(){
        require(tx.gasprice < super.getGasPriceLimit());
        _;
    }

    function setTokenPriceInGas(uint _value) public onlyOwner{
        super.setTokenPriceInGas(_value);
    }

    function setGasPriceLimit(uint _value) public onlyOwner{
        super.setGasPriceLimit(_value);
    }

    function _payGasInToken(uint256 gasLimit) internal{
        // Gas used = Gas limit - gas left + fixed gas amount to use
        // for the following transfer operations
        uint256 tokenPriceInGas = super.getTokenPriceInGas();
        require(tokenPriceInGas > 0);
        uint256 gasToUse = 66670;
        if (msg.sender == owner) {
            gasToUse = 63690;
        }
        uint256 gasUsed = gasLimit - gasleft() + gasToUse;
        uint256 tokenFeeAmount = gasUsed * tokenPriceInGas;
        uint256 etherFeeRefund = gasUsed * tx.gasprice;
        uint256 contractBalance = address(this).balance;
        if (etherFeeRefund > contractBalance) {
            emit NoSufficientFund(contractBalance);
        } else {
            // Transfer Tokens from sender to contract owner
            require(super._transfer(msg.sender, owner, tokenFeeAmount) == true);
            // Transfer equivalent ether balance from contract to sender
            msg.sender.transfer(etherFeeRefund);
            emit TransactionFee(msg.sender, tokenFeeAmount, etherFeeRefund);
        }
    }
}
