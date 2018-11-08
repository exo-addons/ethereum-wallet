pragma solidity ^0.4.24;
import "./Admin.sol";
import "./DataAccess.sol";

/**
 * @title ApprouvableAccount.sol
 * @dev This is an abstract contract that is used to approve and disapprove addresses.
 * The modifier whenApproved is used from ERC20 Token contract to test if the receiver
 * and sender are approved accounts or not. This mechanism will avoid to send accidently
 * tokens outside a known community of users.
 */
contract ApprouvableAccount is DataAccess, Admin {

    // emitted only when a disapproved account is approved
    event ApprovedAccount(address target);

    // emitted only when a approved account is disapproved
    event DisapprovedAccount(address target);

    /**
     * @dev Made internal because this contract is abstract
     */
    constructor() internal{
    }

    /**
     * @dev Modifier to make a function callable only when
     * the receiver and the sender are approved or one of them
     * is the owner of the ERC20 Token Contract
     * @param _from transaction sender
     * @param _to transaction receiver
     */
    modifier whenApproved(address _from, address _to){
        require (owner == _from || owner == _to || (super.isApprovedAccount(_to) && super.isApprovedAccount(_from)));
        _;
    }

    /**
     * @dev Sets an account as approved to receive and send ERC20 tokens
     * @param _target address to approve
     */
    function approveAccount(address _target) public onlyAdmin(1){
        if (!super.isApprovedAccount(_target)) {
            super._setApprovedAccount(_target, true);
            emit ApprovedAccount(_target);
        }
    }

    /**
     * @dev Sets an account as disapproved to receive and send ERC20 tokens
     * @param _target address to disapprove
     */
    function disapproveAccount(address _target) public onlyAdmin(1){
        require (owner != _target);
        if (super.isApprovedAccount(_target)) {
            super._setApprovedAccount(_target, false);
            emit DisapprovedAccount(_target);
        }
    }

    /**
     * @dev Checks if an address is approved to receive and send ERC20 tokens
     * @param _target address to check if it's approved
     * @return true is the address is approved
     */
    function isApprovedAccount(address _target) public view returns (bool){
        return super.isApprovedAccount(_target);
    }

}
