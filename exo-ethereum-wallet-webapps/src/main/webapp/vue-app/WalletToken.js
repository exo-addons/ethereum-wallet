import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from './WalletConstants.js';

/*
 * Get the list of Contracts with details:
 * {
 *   name: name of contract,
 *   symbol: symbol of Token currency,
 *   balance: balance of current account in Tokens,
 *   contract: truffle contract object,
 *   icon: contract icon,
 *   isContract: true,
 *   isDefault: is default contract coming from configuration
 * }
 */
export function getContractsDetails(account, netId) {
  let contractsAddresses = getContractsAddresses(account, netId);
  if((!window.walletSettings.defaultNetworkId || window.walletSettings.defaultNetworkId === netId)
      && window.walletSettings.defaultContractsToDisplay
      && window.walletSettings.defaultContractsToDisplay.length) {
    contractsAddresses = contractsAddresses.concat(window.walletSettings.defaultContractsToDisplay);
  }
  // Remove duplicates and empty strings if existing
  contractsAddresses = contractsAddresses.filter((contractAddress, pos ,tmpArray) =>  contractAddress && contractAddress.trim().length && tmpArray.indexOf(contractAddress) === pos);

  const contractsDetailsPromises = [];

  contractsAddresses.forEach((address) => {
    if (address && address.trim().length) {
      const contractDetails = {};
      contractDetails.address = address;
      contractDetails.icon = 'fa-file-contract';
      contractDetails.isContract = true;
      contractDetails.address = address;
      contractDetails.isDefault = window.walletSettings.defaultContractsToDisplay 
                                  && window.walletSettings.defaultContractsToDisplay.indexOf(address) > -1;
      
      const contractDetailsPromise = getContractAtAddress(account, address)
        .then((loadedContract, error) => {
          if (error) {
            throw error;
          }
          return contractDetails.contract = loadedContract;
        })
        .then(loadedContract => loadContractBalance(account, contractDetails, loadedContract))
        .catch((e) => {
          contractDetails.icon = 'warning';
          contractDetails.title = contractDetails.address;
          contractDetails.error = 'Error retrieving contract at specified address';
          return contractDetails;
        });
      contractsDetailsPromises.push(contractDetailsPromise);
    }
  });
  return Promise.all(contractsDetailsPromises);
}

/*
 * Loads contract balance and cache it in sessionStorage once loaded
 */
export function loadContractBalance(account, contractDetails, contract) {
  if (!contractDetails || (!contract && !contractDetails.contract)) {
    return null;
  }
  if (!contract) {
    contract = contractDetails.contract;
  }
  return contract.symbol.call()
    .then(symbol => contractDetails.symbol = symbol)
    .then(symbol => contractDetails.title = `Account in Token ${symbol}`)
    .then(() => contract.name.call())
    .then(name => contractDetails.name = name)
    .then(() => contract.balanceOf.call(account))
    .then(balance => contractDetails.balance = parseFloat(balance))
    .then(() => {
      return contractDetails;
    })
    .then(() => {
      localStorage.setItem(`exo-wallet-contract-${account}-${contractDetails.address}`.toLowerCase(), JSON.stringify({
        symbol: contractDetails.symbol,
        name: contractDetails.name,
        address: contractDetails.address
      }));
      return contractDetails;
    })
    .catch(err => {
      contractDetails.icon = 'warning';
      contractDetails.title = contractDetails.address;
      contractDetails.error = 'Error retrieving contract at specified address';
      return contractDetails;
    });
}

/*
 * Deletes contract from list on contracts displayed by the user in wallet application
 */
export function deleteContractFromStorage(account, netId, address) {
  address = address.toLowerCase();
  let contractAddresses = localStorage.getItem(`exo-wallet-contracts-${account}-${netId}`.toLowerCase());
  if (contractAddresses) {
    contractAddresses = JSON.parse(contractAddresses);
    if (contractAddresses.indexOf(address) >= 0) {
      contractAddresses.splice(contractAddresses.indexOf(address), 1);
      localStorage.setItem(`exo-wallet-contracts-${account}-${netId}`.toLowerCase(), JSON.stringify(contractAddresses));
      return true;
    }
  }
  return false;
}

/*
 * Gets the list of contracts to display for current account on a chosen network.
 * This information is retrieved from localStorage
 */
export function getContractsAddresses(account, netId) {
  const contractsAddressesString = localStorage.getItem(`exo-wallet-contracts-${account}-${netId}`.toLowerCase());
  let contractsAddresses = null;
  if (!contractsAddressesString) {
    contractsAddresses = [];
  } else {
    contractsAddresses = JSON.parse(contractsAddressesString);
  }
  return contractsAddresses;
}

/*
 * Save a new Contract address as default contract to display for all users
 */
export function saveContractAddressAsDefault(address) {
  address = address.toLowerCase();
  return fetch('/portal/rest/wallet/api/contract/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: $.param({address: address})
  })
    .then(resp => {
      if (resp && resp.ok && window.walletSettings && window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.indexOf(address) < 0) {
        window.walletSettings.defaultContractsToDisplay.push(address);
      }
      return resp;
    });
}

/*
 * Validate Contract existence and save its address in localStorage
 */
export function saveContractAddress(account, address, netId, isDefaultContract) {
  return getContractAtAddress(account, address)
    .then((foundContract, error) => {
      if (error) {
        throw error;
      }
      if (foundContract && foundContract.balanceOf) {
        if (isDefaultContract && window.walletSettings && window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.indexOf(address) >= 0) {
          throw new Error('Contract already exists in the list');
        }

        const contractsAddresses = getContractsAddresses(account, netId);

        if (isDefaultContract || contractsAddresses.indexOf(address) < 0) {
          return foundContract.balanceOf.call(account)
            .then((balance) => {
              if (balance === 0 || balance) {
                address = address.toLowerCase();
                if(contractsAddresses.indexOf(address) < 0) {
                  contractsAddresses.push(address);
                  localStorage.setItem(`exo-wallet-contracts-${account}-${netId}`.toLowerCase(), JSON.stringify(contractsAddresses));
                }
                return true;
              } else {
                return false;
              }
            });
        } else if (!isDefaultContract) {
          throw new Error('Contract already exists');
        }
      } else {
        throw new Error('Invalid contract address');
      }
    });
}

/*
 * Create new instance of ERC20 compliant contract ready to deploy
 */
export function createNewERC20TokenContract(account, newTokenGas, newTokenGasPrice) {
  try {
    const CONTRACT_DATA = {
      contractName: 'Standard ERC20 Token',
      abi: ERC20_COMPLIANT_CONTRACT_ABI,
      bytecode: ERC20_COMPLIANT_CONTRACT_BYTECODE
    };
    const NEW_TOKEN = window.TruffleContract(CONTRACT_DATA);
  
    NEW_TOKEN.defaults({
      from: account,
      gas: newTokenGas.toString(),
      gasPrice: newTokenGasPrice.toString()
    });
  
    NEW_TOKEN.setProvider(window.localWeb3.currentProvider);
    return Promise.resolve(NEW_TOKEN);
  } catch (e) {
    return Promise.reject(e);
  }
}

/*
 * Retrieve an ERC20 contract instance at specified address
 */
export function getContractAtAddress(account, address) {
  try {
    const ERC20_CONTRACT = window.TruffleContract({
      abi: ERC20_COMPLIANT_CONTRACT_ABI
    });
    // Use Old version of Web3 (from Metamask) providersince TruffleContract is not compatible with new Web3 version
    if (window.localWeb3.currentProvider.host && window.localWeb3.currentProvider.host.indexOf("http") === 0) {
      ERC20_CONTRACT.setProvider(new Web3.providers.HttpProvider(window.localWeb3.currentProvider.host));
    } else {
      ERC20_CONTRACT.setProvider(window.localWeb3.currentProvider);
    }
    ERC20_CONTRACT.defaults({
      from: account,
      gas: window.walletSettings.userDefaultGas
    });
    return ERC20_CONTRACT.at(address);
  } catch (e) {
    return Promise.reject(e);
  }
}