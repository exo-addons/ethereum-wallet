pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ERC20Abstract.sol";

contract GasPayableInToken is Owned, ERC20Abstract {

    event TokenPriceChanged(uint256 tokenPrice, uint256 gasPriceInToken, uint256 gasPrice);
    event TransactionFee(address from, uint tokenFee, uint etherFeeRefund);
    event NoSufficientFund(uint balance);

    constructor() internal{
    }

    function setTokenPrice(uint256 _value) public onlyOwner{
        uint256 gasPriceInToken = (10 ** (uint(super.decimals()))) /_value * tx.gasprice;
        super.setGasPriceInToken(gasPriceInToken);
        emit TokenPriceChanged(_value, gasPriceInToken, tx.gasprice);
    }

    function payGasInToken(uint256 gasLimit) internal{
        // Gas used = Gas limit - gas left + fixed gas amount to use
        // for the following transfer operations
        if (tx.gasprice > super.getGasPriceLimit()) {
            return;
        }
        uint256 gasPriceInToken = super.getGasPriceInToken();
        require(gasPriceInToken > 0);
        uint256 gasToUse = 64220;
        uint256 gasUsed = gasLimit - gasleft() + gasToUse;
        uint256 tokenFeeAmount = gasUsed * gasPriceInToken;
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
