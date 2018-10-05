pragma solidity ^0.4.24;
import "./Owned.sol";

contract ApprouvableBuyerAccount is Owned {

    // TODO use external contract call for this
    event ApprovedBuyerAccount(address target);
    event DisapprovedBuyerAccount(address target);

    mapping (address => bool) public approvedBuyerAccount;

    constructor() internal {
        // TODO consider when ownership transferred twice, the old owner should always be approved at first
        approveBuyerAccount(msg.sender);
    }

    function approveBuyerAccount(address _target) public onlyOwner returns (bool){
        if (!approvedBuyerAccount[_target]) {
            approvedBuyerAccount[_target] = true;
            emit ApprovedBuyerAccount(_target);
        }
    }

    function disapproveBuyerAccount(address _target) public onlyOwner returns (bool){
        if (approvedBuyerAccount[_target]) {
            approvedBuyerAccount[_target] = false;
            emit DisapprovedBuyerAccount(_target);
        }
    }

    modifier whenBuyerApproved(address _buyer){
        require (owner == _buyer || approvedBuyerAccount[_buyer]);
        _;
    }

}
