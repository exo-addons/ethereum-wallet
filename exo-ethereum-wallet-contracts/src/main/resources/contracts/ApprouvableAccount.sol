pragma solidity ^0.4.24;
import "./Owned.sol";
import "./ERTTokenDataProxy.sol";

contract ApprouvableAccount is Owned, ERTTokenDataProxy {

    event ApprovedAccount(address target);
    event DisapprovedAccount(address target);

    constructor() internal{
    }

    function approveAccount(address _target) public onlyOwner returns (bool){
        if (!super.approvedAccount(_target)) {
            super.setApprovedAccount(_target, true);
            emit ApprovedAccount(_target);
        }
    }

    function disapproveAccount(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        // TODO use external contract call for this
        if (super.approvedAccount(_target)) {
            super.setApprovedAccount(_target, false);
            emit DisapprovedAccount(_target);
        }
    }

    function isApprovedAccount(address _target) public view returns (bool){
        return super.approvedAccount(_target);
    }

    modifier whenApproved(address _from, address _to){
        require (owner == _from || super.approvedAccount(_to));
        _;
    }

}
