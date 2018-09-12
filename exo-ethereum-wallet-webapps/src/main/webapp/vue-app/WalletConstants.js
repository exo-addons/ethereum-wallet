export const ERC20_COMPLIANT_CONTRACT_ABI = [
  {
    "constant": true,
    "inputs": [],
    "name": "name",
    "outputs": [
      {
        "name": "",
        "type": "string"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": false,
    "inputs": [
      {
        "name": "_spender",
        "type": "address"
      },
      {
        "name": "_value",
        "type": "uint256"
      }
    ],
    "name": "approve",
    "outputs": [
      {
        "name": "success",
        "type": "bool"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [],
    "name": "totalSupply",
    "outputs": [
      {
        "name": "",
        "type": "uint256"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": false,
    "inputs": [
      {
        "name": "_from",
        "type": "address"
      },
      {
        "name": "_to",
        "type": "address"
      },
      {
        "name": "_value",
        "type": "uint256"
      }
    ],
    "name": "transferFrom",
    "outputs": [
      {
        "name": "success",
        "type": "bool"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [],
    "name": "decimals",
    "outputs": [
      {
        "name": "",
        "type": "uint8"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [],
    "name": "version",
    "outputs": [
      {
        "name": "",
        "type": "string"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [
      {
        "name": "_owner",
        "type": "address"
      }
    ],
    "name": "balanceOf",
    "outputs": [
      {
        "name": "balance",
        "type": "uint256"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [],
    "name": "symbol",
    "outputs": [
      {
        "name": "",
        "type": "string"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": false,
    "inputs": [
      {
        "name": "_to",
        "type": "address"
      },
      {
        "name": "_value",
        "type": "uint256"
      }
    ],
    "name": "transfer",
    "outputs": [
      {
        "name": "success",
        "type": "bool"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": false,
    "inputs": [
      {
        "name": "_spender",
        "type": "address"
      },
      {
        "name": "_value",
        "type": "uint256"
      },
      {
        "name": "_extraData",
        "type": "bytes"
      }
    ],
    "name": "approveAndCall",
    "outputs": [
      {
        "name": "success",
        "type": "bool"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "constant": true,
    "inputs": [
      {
        "name": "_owner",
        "type": "address"
      },
      {
        "name": "_spender",
        "type": "address"
      }
    ],
    "name": "allowance",
    "outputs": [
      {
        "name": "remaining",
        "type": "uint256"
      }
    ],
    "payable": false,
    "type": "function"
  },
  {
    "inputs": [
      {
        "name": "_initialAmount",
        "type": "uint256"
      },
      {
        "name": "_tokenName",
        "type": "string"
      },
      {
        "name": "_decimalUnits",
        "type": "uint8"
      },
      {
        "name": "_tokenSymbol",
        "type": "string"
      }
    ],
    "type": "constructor"
  },
  {
    "payable": false,
    "type": "fallback"
  },
  {
    "anonymous": false,
    "inputs": [
      {
        "indexed": true,
        "name": "_from",
        "type": "address"
      },
      {
        "indexed": true,
        "name": "_to",
        "type": "address"
      },
      {
        "indexed": false,
        "name": "_value",
        "type": "uint256"
      }
    ],
    "name": "Transfer",
    "type": "event"
  },
  {
    "anonymous": false,
    "inputs": [
      {
        "indexed": true,
        "name": "_owner",
        "type": "address"
      },
      {
        "indexed": true,
        "name": "_spender",
        "type": "address"
      },
      {
        "indexed": false,
        "name": "_value",
        "type": "uint256"
      }
    ],
    "name": "Approval",
    "type": "event"
  }
];
export const ERC20_COMPLIANT_CONTRACT_BYTECODE = "0x608060405234801561001057600080fd5b506040516107e53803806107e5833981016040908152815160208084015183850151606086015160068054600160a060020a0319163390811790915560009081526001855295862085905594849055908501805193959094919391019161007c916003918601906100a9565b506004805460ff191660ff8416179055805161009f9060059060208401906100a9565b5050505050610144565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106100ea57805160ff1916838001178555610117565b82800160010185558215610117579182015b828111156101175782518255916020019190600101906100fc565b50610123929150610127565b5090565b61014191905b80821115610123576000815560010161012d565b90565b610692806101536000396000f3006080604052600436106100b95763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166306fdde0381146100be578063095ea7b31461014857806318160ddd1461018057806323b872dd146101a757806327e235e3146101d1578063313ce567146101f25780635c6581651461021d57806370a08231146102445780638da5cb5b1461026557806395d89b4114610296578063a9059cbb146102ab578063dd62ed3e146102cf575b600080fd5b3480156100ca57600080fd5b506100d36102f6565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561010d5781810151838201526020016100f5565b50505050905090810190601f16801561013a5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561015457600080fd5b5061016c600160a060020a0360043516602435610384565b604080519115158252519081900360200190f35b34801561018c57600080fd5b506101956103ea565b60408051918252519081900360200190f35b3480156101b357600080fd5b5061016c600160a060020a03600435811690602435166044356103f0565b3480156101dd57600080fd5b50610195600160a060020a03600435166104f4565b3480156101fe57600080fd5b50610207610506565b6040805160ff9092168252519081900360200190f35b34801561022957600080fd5b50610195600160a060020a036004358116906024351661050f565b34801561025057600080fd5b50610195600160a060020a036004351661052c565b34801561027157600080fd5b5061027a610547565b60408051600160a060020a039092168252519081900360200190f35b3480156102a257600080fd5b506100d3610556565b3480156102b757600080fd5b5061016c600160a060020a03600435166024356105b1565b3480156102db57600080fd5b50610195600160a060020a036004358116906024351661063b565b6003805460408051602060026001851615610100026000190190941693909304601f8101849004840282018401909252818152929183018282801561037c5780601f106103515761010080835404028352916020019161037c565b820191906000526020600020905b81548152906001019060200180831161035f57829003601f168201915b505050505081565b336000818152600260209081526040808320600160a060020a038716808552908352818420869055815186815291519394909390927f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925928290030190a350600192915050565b60005481565b600160a060020a0383166000818152600260209081526040808320338452825280832054938352600190915281205490919083118015906104315750828110155b151561043c57600080fd5b600160a060020a038085166000908152600160205260408082208054870190559187168152208054849003905560001981101561049e57600160a060020a03851660009081526002602090815260408083203384529091529020805484900390555b83600160a060020a031685600160a060020a03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef856040518082815260200191505060405180910390a3506001949350505050565b60016020526000908152604090205481565b60045460ff1681565b600260209081526000928352604080842090915290825290205481565b600160a060020a031660009081526001602052604090205490565b600654600160a060020a031681565b6005805460408051602060026001851615610100026000190190941693909304601f8101849004840282018401909252818152929183018282801561037c5780601f106103515761010080835404028352916020019161037c565b336000908152600160205260408120548211156105cd57600080fd5b33600081815260016020908152604080832080548790039055600160a060020a03871680845292819020805487019055805186815290519293927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef929181900390910190a350600192915050565b600160a060020a039182166000908152600260209081526040808320939094168252919091522054905600a165627a7a72305820ac7f2cb16ce6214df7efd8701d597b8d23a027da7ca5f463f83a416f7ad3248b0029";

export const FIAT_CURRENCIES = {
  "aud":{
    "value":"aud",
    "text":"Australia Dollar (AUD)",
    "symbol": "$ (AUD)"
  },
  "brl":{
    "value":"brl",
    "text":"Brazil Real (R$)",
    "symbol": "R$"
  },
  "cad":{
    "value":"cad",
    "text":"Canadian dollar (CAD)",
    "symbol": "$ (CAD)"
  },
  "chf":{
    "value":"chf",
    "text":"Switzerland Franc (CHF)",
    "symbol": "CHF"
  },
  "clp":{
    "value":"clp",
    "text":"Chile Peso (CLP)",
    "symbol": "$ (CLP)"
  },
  "cny":{
    "value":"cny",
    "text":"China Yuan Renminbi (CNY)",
    "symbol": "¥ (CNY)"
  },
  "czk":{
    "value":"czk",
    "text":"Czech Republic Koruna (Kč)",
    "symbol": "Kč"
  },
  "dkk":{
    "value":"dkk",
    "text":"Denmark Krone (DKK)",
    "symbol": "kr (DKK)"
  },
  "eur":{
    "value":"eur",
    "text":"Euro Member Countries (€)",
    "symbol": "€"
  },
  "gbp":{
    "value":"gbp",
    "text":"United Kingdom Pound (£)",
    "symbol": "£"
  },
  "hkd":{
    "value":"hkd",
    "text":"Hong Kong Dollar (HKD)",
    "symbol": "$ (HKD)"
  },
  "huf":{
    "value":"huf",
    "text":"Hungary Forint (Ft)",
    "symbol": "Ft"
  },
  "idr":{
    "value":"idr",
    "text":"Indonesia Rupiah (Rp)",
    "symbol": "Rp"
  },
  "inr":{
    "value":"inr",
    "text":"India Rupee (INR)",
    "symbol": "INR"
  },
  "jpy":{
    "value":"jpy",
    "text":"Japan Yen (¥)",
    "symbol": "¥"
  },
  "krw":{
    "value":"krw",
    "text":"Korea (South) Won (₩)",
    "symbol": "₩"
  },
  "mxn":{
    "value":"mxn",
    "text":"Mexico Peso (MXN)",
    "symbol": "$ (MXN)"
  },
  "myr":{
    "value":"myr",
    "text":"Malaysia Ringgit (RM)",
    "symbol": "RM"
  },
  "nok":{
    "value":"nok",
    "text":"Norway Krone (NOK)",
    "symbol": "kr (NOK)"
  },
  "nzd":{
    "value":"nzd",
    "text":"New Zealand Dollar (NZD)",
    "symbol": "$ (NZD)"
  },
  "php":{
    "value":"php",
    "text":"Philippines Piso (₱)",
    "symbol": "₱"
  },
  "pkr":{
    "value":"pkr",
    "text":"Pakistan Rupee (₨)",
    "symbol": "₨"
  },
  "pln":{
    "value":"pln",
    "text":"Poland Zloty (zł)",
    "symbol": "zł"
  },
  "usd":{
    "value":"usd",
    "text":"United States Dollar ($)",
    "symbol": "$"
  },
  "rub":{
    "value":"rub",
    "text":"Russia Ruble (₽)",
    "symbol": "₽"
  },
  "sek":{
    "value":"sek",
    "text":"Sweden Krona (SEK)",
    "symbol": "kr (SEK)"
  },
  "sgd":{
    "value":"sgd",
    "text":"Singapore Dollar (SGD)",
    "symbol": "$ (SGD)"
  },
  "thb":{
    "value":"thb",
    "text":"Thailand Baht (THB)",
    "symbol": "฿ (THB)"
  },
  "try":{
    "value":"try",
    "text":"Turkey Lira (TRY)",
    "symbol": "TRY"
  },
  "twd":{
    "value":"twd",
    "text":"Taiwan New Dollar (NT$)",
    "symbol": "NT$"
  },
  "zar":{
    "value":"zar",
    "text":"South Africa Rand (ZAR)",
    "symbol": "R (ZAR)"
  }
}

export const NETWORK_NAMES = {
  0: '',
  1: 'Ethereum Main',
  2: 'Ethereum Classic main',
  3: 'Ropsten',
  4: 'Rinkeby',
  42: 'Kovan'
}

export const OK = 'OK';
export const ERROR_WALLET_NOT_CONFIGURED = 'ERROR_WALLET_NOT_CONFIGURED';
export const ERROR_WALLET_SETTINGS_NOT_LOADED = 'ERROR_WALLET_SETTINGS_NOT_LOADED';
export const ERROR_WALLET_DISCONNECTED = 'ERROR_WALLET_DISCONNECTED';