pragma solidity ^0.4.25;
import "./Owned.sol";

contract ApprouvableAccount is Owned {

    event ApprovedAccount(address target);
    event DisapprovedAccount(address target);

    mapping (address => bool) public approvedAccount;

    function approveAccount(address _target) onlyOwner returns (bool){
        if (!approvedAccount[_target]) {
            approvedAccount[_target] = true;
            emit ApprovedAccount(_target);
        }
    }

    function disapproveAccount(address _target) onlyOwner returns (bool){
        // TODO use external contract call for this
        if (approvedAccount[_target]) {
            approvedAccount[_target] = false;
            emit DisapprovedAccount(_target);
        }
    }

    modifier whenApproved(address _target){
        require (msg.sender == super.owner || approvedAccount[_target]);
        _;
    }

}
