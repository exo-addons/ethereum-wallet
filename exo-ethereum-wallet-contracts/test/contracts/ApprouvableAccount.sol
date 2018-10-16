pragma solidity ^0.4.24;
import "./Owned.sol";

contract ApprouvableAccount is Owned {

    event ApprovedAccount(address target);
    event DisapprovedAccount(address target);

    mapping (address => bool) public approvedAccount;

    constructor() internal{
        // TODO consider when ownership transferred twice,
        // the old owner should always be approved at first
        approveAccount(msg.sender);
    }

    function approveAccount(address _target) public onlyOwner returns (bool){
        if (!approvedAccount[_target]) {
            approvedAccount[_target] = true;
            emit ApprovedAccount(_target);
        }
    }

    function disapproveAccount(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        // TODO use external contract call for this
        if (approvedAccount[_target]) {
            approvedAccount[_target] = false;
            emit DisapprovedAccount(_target);
        }
    }

    function isApprovedAccount(address _target) public view returns (bool){
        return approvedAccount[_target];
    }

    modifier whenApproved(address _from, address _to){
        require (owner == _from || approvedAccount[_to]);
        _;
    }

}
