pragma solidity ^0.4.24;
import "./Owned.sol";
import "./DataAccess.sol";

contract ApprouvableAccount is Owned, DataAccess {

    event ApprovedAccount(address target);
    event DisapprovedAccount(address target);

    constructor() internal{
    }

    modifier whenApproved(address _from, address _to){
        require (owner == _from || super.isApprovedAccount(_to));
        _;
    }

    function approveAccount(address _target) public onlyOwner returns (bool){
        if (!super.isApprovedAccount(_target)) {
            super.setApprovedAccount(_target, true);
            emit ApprovedAccount(_target);
        }
    }

    function disapproveAccount(address _target) public onlyOwner returns (bool){
        require (owner != _target);
        if (super.isApprovedAccount(_target)) {
            super.setApprovedAccount(_target, false);
            emit DisapprovedAccount(_target);
        }
    }

    function isApprovedAccount(address _target) public view returns (bool){
        return super.isApprovedAccount(_target);
    }

}
