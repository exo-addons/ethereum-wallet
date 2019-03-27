pragma solidity >=0.4.24;
import "./ERTTokenV1.sol";

/**
 * @title Rewarding.sol
 * @dev Rewarding management contract
 */
contract AccountRewarding is ERTTokenV1 {

    // Event emited when an address is rewarded
    event Reward(address indexed _from, address indexed _to, uint256 _tokenAmount);

    constructor () internal {
    }

    /**
     * @dev reward `_value` token to `_to` from `msg.sender`
     * @param _to The address of the recipient
     * @param _value The amount of token to be rewarded
     */
    function reward(address _to, uint256 _value) public onlyAdmin(2) whenNotPaused whenApproved(_to) {
        uint256 gasLimit = gasleft();

        // Add the new reward balance to the recipient
        super._setRewardBalance(_to, super.safeAdd(super.rewardBalanceOf(_to), _value));

        // Emit a specific event for initialization operation
        emit Reward(msg.sender, _to, _value);

        // Transfer rewarded token amount
        require(super._transfer(msg.sender, _to, _value) == true);

        // Emit Standard ERC-20 event
        emit Transfer(msg.sender, _to, _value);

        // Pay transaction with token instead of ether
        super._payGasInToken(gasLimit);
    }

}
