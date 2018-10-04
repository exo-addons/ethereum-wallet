pragma solidity ^0.4.15;

import "./tokenproxy.sol"

// ================= Safemath Contract start ============================
/* taking ideas from FirstBlood token */
contract SafeMath {

  function safeAdd(uint256 x, uint256 y) internal returns(uint256) {
    uint256 z = x + y;
    assert((z >= x) && (z >= y));
    return z;
  }

  function safeSubtract(uint256 x, uint256 y) internal returns(uint256) {
    assert(x >= y);
    uint256 z = x - y;
    return z;
  }

  function safeMult(uint256 x, uint256 y) internal returns(uint256) {
    uint256 z = x * y;
    assert((x == 0)||(z/x == y));
    return z;
  }
}
// ================= Safemath Contract end ==============================

// ================= Ownable Contract start =============================
contract Ownable {
  // TODO Add event for TransferOwnership

  event TransferOwnership(address newOwner);

  address public owner;

  function Ownable() {
    owner = msg.sender;
  }

  modifier onlyOwner() {
    require(msg.sender == owner);
    _;
  }

  function transferOwnership(address _newOwner) onlyOwner {
    if (_newOwner != address(0)) {
      owner = _newOwner;
      TransferOwnership(_newOwner);
    }
  }
}
// ================= Ownable Contract end ===============================


// ================= managaeble  Contract start =============================
contract Managaeble is Ownable{
  // TODO add constructor to add owner as manager
 
 struct manager{
   string name;
 }

  mapping(address => manager) public managers;
  address[] public list;


  Managaeble(string _owner) public {
    managers[msg.sender].name = _owner;
  }

  // TODO List of managers

  event AddManager(address newManager);
  // TODO add modifier onlyManager

  modifier onlyManager(){
    for(uint index = 0; index < list; index++){
    require(managers[index]==msg.sender);
    _;
  }

  function addManager(address _newManager, string _name) onlyOwner {
    if (_newManager != address(0)) {
      // TODO
      manager.name = _name;
      list.push( _newManager);
      emit AddManager(_newManager);
    }
  }

}
// ================= managaeble Contract end ===============================


// ================= ERC20 Token Contract start =========================
contract ERC20 {

  string  public name;
  string  public symbol;
  uint256 public decimals;
  uint public totalSupply;

// TODO constructor
  ERC20(string _name, string _symbol, uint256 _decimals) {
    name = _name;
    symbol = _symbol;
    decimals = _decimals;
  }

  mapping(address => uint) public balances;

  function balanceOf(address _who) constant returns (uint);
  function allowance(address _owner, address _spender) constant returns (uint);

  function transfer(address _to, uint _value) returns (bool ok);
  function transferFrom(address _from, address _to, uint _value) returns (bool ok);
  function approve(address _spender, uint _value) returns (bool ok);

  event Transfer(address indexed from, address indexed to, uint value);
  event Approval(address indexed owner, address indexed spender, uint value);
}
// ================= ERC20 Token Contract end ===========================


// ================= freezable Contract start ===============================
contract Freezable is Ownable, managaeble {
  // TODO add notFrozenAccount modifier and use it in all public methods that will modify blockchain data
  
  address[] public frozenAccounts;


  //mapping (address => uint) public frozenAccount;
  //event FrozenAccount(address target, bool frozen);
  // TODO delete "bool freeze"
  //function freezeAccount(address _target, bool freeze) onlyOwner public {
  //frozenAccount[_target] = freeze;
  //emit FrozenAccount(_target, freeze);
  //}


  event FrozenAccount(address ftarget);
  event NotFrozenAccount(address nftarget);

  bool public frozen = false;

  modifier whenNotFrozen() {
    require (!frozen);
    _;
  }

  modifier whenFrozen {
    require (frozen) ;
    _;
  }

  function freezeAccount(address _ftarget) onlyManager whenNotFrozen returns (bool) {
    frozen = true;
    frozenAccount.push(_ftarget);
    FrozenAccount(_ftarget);
    return true;
  }

  function unFrozenAccount(address _uftarget) onlyManager whenFrozen returns (bool) {
    frozen = false;
    NotFrozenAccount(_uftarget);
    return true;
  }


  // TODO unFreezeAccount
}
// ================= freezable Contract end ============================


// ================= burnable Contract start ===============================
contract Burnable is ERC20, SafeMath, Ownable {
  event BurnFunds(address indexed from, uint256 value);

  function burn(uint256 _value) returns (bool success) {
    require (balances[msg.sender] < _value)  ;          // Check if the sender has enough
    require (_value <= 0)  ;
    balances[msg.sender] = SafeMath.safeSubtract(balances[msg.sender], _value);     // Subtract from the sender
    totalSupply = SafeMath.safeSubtract(totalSupply,_value);                        // Updates totalSupply
    BurnFunds(msg.sender, _value);
    return true;
  }
}
// ================= burnable Contract end ============================

// ================= Pausable Token Contract start ======================
contract Pausable is Ownable, managaeble {
  event Pause();
  event UnPause();

  bool public paused = false;

  modifier whenNotPaused() {
    require (!paused);
    _;
  }

  modifier whenPaused {
    require (paused) ;
    _;
  }

  function pause() onlyManager whenNotPaused returns (bool) {
    paused = true;
    Pause();
    return true;
  }

  function unPause() onlyManager whenPaused returns (bool) {
    paused = false;
    Unpause();
    return true;
  }
}
// ================= Pausable Token Contract end ========================

// ================= ExoToken Contract start ======================
contract ExoToken is ERC20, SafeMath, Ownable, freezable, Pausable, managaeble, Proxy {   
  address public eXoContract;
  mapping (address => mapping (address => uint)) public allowed;

  //attak short address
  modifier onlyPayloadSize(uint size) {
    require(msg.data.length >= size + 4) ;
    _;
  }

  function ExoToken(uint256 _initialSupply) public {
    
    // TODO make a call to ERC 20 constructor
    token = new Token("eXoTken", "eXo", 18);
    balanceOf[msg.sender] = _initialSupply;
    totalSupply = _initialSupply;
  }



  function getName() onlyOwner constant public returns(string) {
   return name;
  }
    
  function setName(string _newName) onlyOwner public {
    name = _newName;
  }


  function setSymbol(string _newSymbol) onlyOwner public {
    symbol = _newSymbol;
  }




  function transfer(address _to, uint _value) whenNotFrozen whenNotPaused onlyPayloadSize(2 * 32) returns (bool success) {
    require(!frozenAccount[_to]);
    require (_to != 0x0);  
    balances[msg.sender] = safeSubtract(balances[msg.sender], _value);
    balances[_to] = safeAdd(balances[_to], _value);
    Transfer(msg.sender, _to, _value);
    return true;
  }


  function transferFrom(address _from, address _to, uint _value) whenNotFrozen onlyPayloadSize(3 * 32) returns (bool success) {
    var _allowance = allowed[_from][msg.sender];
    require(!frozenAccount[_from]); // Check if sender is frozen
    require(!frozenAccount[_to]);
    balances[_to] = safeAdd(balances[_to], _value);
    balances[_from] = safeSubtract(balances[_from], _value);
    allowed[_from][msg.sender] = safeSubtract(_allowance, _value);
    Transfer(_from, _to, _value);
    return true;
  }

  function balanceOf(address _owner) constant returns (uint balance) {
    return balances[_owner];
  }


  function approve(address _spender, uint _value)  whenNotFrozen whenNotPaused onlyPayloadSize(2 * 32) returns (bool success) {
    allowed[msg.sender][_spender] = _value;
    Approval(msg.sender, _spender, _value);
    return true;
  }

  function allowance(address _owner, address _spender) constant returns (uint remaining) {
    return allowed[_owner][_spender];
  }


  function setContract(address _eXoContract) onlyOwner {
   if (_eXoContract != address(0)) {
      eXoContract = _eXoContract;
    }
  }


  function () public payable {
     revert();
  }

}
// ================= ExoToken Contract end ========================

