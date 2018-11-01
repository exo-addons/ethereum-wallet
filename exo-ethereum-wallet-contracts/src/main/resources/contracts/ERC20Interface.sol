pragma solidity ^0.4.24;

/*
 * ERC20 basic operations declaration
 * See https://github.com/ethereum/EIPs/blob/master/EIPS/eip-20.md
 */
contract ERC20Interface {
    /*
     * @param _owner The address from which the balance will be retrieved
     * @return the amount of balance of the address
     */
    function balanceOf(address _target) public view returns (uint256 balance);

    /*
     * @dev send `_value` token to `_to` from `msg.sender`
     * @param _to The address of the recipient
     * @param _value The amount of token to be transferred
     * @return Whether the transfer was successful or not
     */
    function transfer(address _to, uint256 _value) public returns (bool success);

    /*
     * @dev send `_value` token to `_to` from `_from` on the condition it is approved by `_from`
     * @param _from The address of the sender
     * @param _to The address of the recipient
     * @param _value The amount of token to be transferred
     * @return Whether the transfer was successful or not
     */
    function transferFrom(address _from, address _to, uint256 _value) public returns (bool success);

    /*
     * @dev `msg.sender` approves `_spender` to spend `_value` tokens
     * @param _spender The address of the account able to transfer the tokens
     * @param _value The amount of tokens to be approved for transfer
     * @return Whether the approval was successful or not
     */
    function approve(address _spender, uint256 _value) public returns (bool success);

    /*
     * @param _owner The address of the account owning tokens
     * @param _spender The address of the account able to transfer the tokens
     * @return Amount of remaining tokens allowed to spent
     */
    function allowance(address _owner, address _spender) public view returns (uint256 remaining);

    // Event emited when a token transfer happens is made
    event Transfer(address indexed _from, address indexed _to, uint256 _value);

    // Event emited when a token transfer Approval is made
    event Approval(address indexed _owner, address indexed _spender, uint256 _value);
}
