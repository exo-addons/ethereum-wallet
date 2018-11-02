pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ERC20Abstract.sol";

/**
 * @title GasPayableInToken.sol
 * @dev An abstract contract to pay gas using ether collected on contract
 * instead of letting users pay the transaction fee
 */
contract GasPayableInToken is Owned, ERC20Abstract {

    // Event emitted when the owner changes the token price
    event TokenPriceChanged(uint256 tokenPrice, uint256 gasPriceInToken, uint256 gasPrice);
    // Event emitted when the transaction fee is payed by contract ether balance
    event TransactionFee(address from, uint tokenFee, uint etherFeeRefund);
    // Event emitted when the contract doesn't have enough founds to pay gas
    event NoSufficientFund(uint balance);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev Set the token price in ether and calculate consequetly the gas price in
     * tokens. (determine the amount tokens for 1 gas)
     * @param _value the amount of 1 token price in WEI
     */
    function setTokenPrice(uint256 _value) public onlyOwner{
        uint256 gasPriceInToken = (10 ** (uint(super.decimals()))) /_value * tx.gasprice;
        super._setGasPriceInToken(gasPriceInToken);
        emit TokenPriceChanged(_value, gasPriceInToken, tx.gasprice);
    }

    /**
     * @dev Pay transaction fee from contract ether balance and deducts the equivalent in tokens
     * from issuer tokens balance.
     * @param _gasLimit gas limit of transaction, used to calculate used gas
     */
    function _payGasInToken(uint256 _gasLimit) internal{
        // Unnecessary to transfer Tokens from Owner to himself
        if (msg.sender == owner) {
            return;
        }
        // Gas used = Gas limit - gas left + fixed gas amount to use
        // for the following transfer operations
        if (tx.gasprice > super.getGasPriceLimit()) {
            return;
        }
        uint256 gasPriceInToken = super.getGasPriceInToken();
        require(gasPriceInToken > 0);

        // Used gas until this instruction + a fixed gas
        // that will be used to finish the transaction
        uint256 gasUsed = _gasLimit - gasleft() + 64220;
        uint256 tokenFeeAmount = gasUsed * gasPriceInToken;
        uint256 etherFeeRefund = gasUsed * tx.gasprice;
        uint256 contractBalance = address(this).balance;
        if (etherFeeRefund > contractBalance) {
            // No sufficient funds on contract, thus the issuer will pay gas by himself using his ether
            // and not tokens
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
