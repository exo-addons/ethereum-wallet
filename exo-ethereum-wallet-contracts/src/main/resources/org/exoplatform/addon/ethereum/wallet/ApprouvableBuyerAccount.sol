pragma solidity ^0.4.25;
import "./Owned.sol";

contract ApprouvableBuyerAccount is Owned {

    event ApprovedBuyerAccount(address target);
    event DisapprovedBuyerAccount(address target);

    mapping (address => bool) public approvedBuyerAccount;

    function approveBuyerAccount(address _target) onlyOwner returns (bool){
        if (!approvedBuyerAccount[_target]) {
            approvedBuyerAccount[_target] = true;
            emit ApprovedBuyerAccount(_target);
        }
    }

    function disapproveBuyerAccount(address _target) onlyOwner returns (bool){
        // TODO use external contract call for this
        if (approvedBuyerAccount[_target]) {
            approvedBuyerAccount[_target] = false;
            emit DisapprovedBuyerAccount(_target);
        }
    }

    modifier whenBuyerApproved(address _target){
        require (msg.sender == super.owner || approvedBuyerAccount[_target]);
        _;
    }

}
