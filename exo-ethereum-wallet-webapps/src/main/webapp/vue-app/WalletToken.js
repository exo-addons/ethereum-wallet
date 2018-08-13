import {ERC20_COMPLIANT_CONTRACT_ABI, ERC20_COMPLIANT_CONTRACT_BYTECODE} from './WalletConstants.js';

/*
 * Get the list of Contracts with details:
 * {
 *   name: name of contract,
 *   symbol: symbol of Token currency,
 *   balance: balance of current account in Tokens,
 *   contract: web3.js contract object,
 *   icon: contract icon,
 *   error: true if there is an error,
 *   isContract: true,
 *   isDefault: is default contract coming from configuration
 * }
 */
export function getContractsDetails(account, netId, onlyDefault) {
  let contractsAddresses = onlyDefault ? [] : getContractsAddresses(account, netId);
  if(window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.length) {
    contractsAddresses = contractsAddresses.concat(window.walletSettings.defaultContractsToDisplay);
  }
  // Remove duplicates and empty strings if existing
  contractsAddresses = contractsAddresses.filter((contractAddress, pos ,tmpArray) =>  contractAddress && contractAddress.trim().length && tmpArray.indexOf(contractAddress) === pos);

  const contractsDetailsPromises = [];
  for (let i = 0; i < contractsAddresses.length; i++) {
    const address = contractsAddresses[i];
    if (address && address.trim().length) {
      const contractDetails = {};
      contractDetails.address = address;
      contractDetails.icon = 'fa-file-contract';
      contractDetails.isContract = true;
      contractDetails.address = address;
      contractDetails.isDefault = window.walletSettings.defaultContractsToDisplay 
                                  && window.walletSettings.defaultContractsToDisplay.indexOf(address) > -1;
      contractsDetailsPromises.push(retrieveContractDetails(account, contractDetails));
    }
  }
  return Promise.all(contractsDetailsPromises);
}

/*
 * Retrieve an ERC20 contract instance at specified address
 */
export function retrieveContractDetails(account, contractDetails) {
  return getContractInstance(account, contractDetails.address)
    .then((contractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!contractInstance) {
        throw new Error('Can\'t find contract instance');
      }
      return contractDetails.contract = contractInstance;
    })
    .then(() => contractDetails.contract.methods.symbol().call())
    .then(symbol => contractDetails.symbol = symbol)
    .then(symbol => contractDetails.title = `Account in Token ${symbol}`)
    .then(() => contractDetails.contract.methods.name().call())
    .then(name => contractDetails.name = name)
    .then(() => contractDetails.contract.methods.balanceOf(account).call())
    .then(balance => contractDetails.balance = parseFloat(`${balance}`))
    .then(() => {
      return contractDetails;
    })
    .then(() => {
      // Store contract persistent details in localStorage
      localStorage.setItem(`exo-wallet-contract-${account}-${contractDetails.address}`.toLowerCase(), JSON.stringify({
        symbol: contractDetails.symbol,
        name: contractDetails.name,
        address: contractDetails.address
      }));
      return contractDetails;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error", e);
      transformContracDetailsToFailed(contractDetails, e);
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
 * Creates new Web3 conract deployment transaction
 */
export function deployContractInstance(...args) {
  return new window.localWeb3.eth.Contract(ERC20_COMPLIANT_CONTRACT_ABI).deploy({
    data: ERC20_COMPLIANT_CONTRACT_BYTECODE,
    arguments: args
  });
}

/*
 * Removes a Contract address defined as default contract to display for all users
 */
export function removeContractAddressFromDefault(address) {
  return fetch('/portal/rest/wallet/api/contract/remove', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: $.param({
      address: address,
      networkId: window.walletSettings.currentNetworkId
    })
  }).then(resp => {
    if (resp && resp.ok) {
      window.walletSettings.defaultContractsToDisplay.splice(window.walletSettings.defaultContractsToDisplay.indexOf(address), 1);
    } else {
      throw new Error('Error deleting contract as default');
    }
  });
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
    body: $.param({
      address: address,
      networkId: window.walletSettings.currentNetworkId
    })
  })
    .then(resp => {
      if (!window.walletSettings.defaultContractsToDisplay) {
        window.walletSettings.defaultContractsToDisplay = [];
      }
      if (resp && resp.ok && window.walletSettings.defaultContractsToDisplay.indexOf(address) < 0) {
        window.walletSettings.defaultContractsToDisplay.push(address);
      }
      return resp;
    });
}

/*
 * Validate Contract existence and save its address in localStorage
 */
export function saveContractAddress(account, address, netId, isDefaultContract) {
  if (isDefaultContract && window.walletSettings && window.walletSettings.defaultContractsToDisplay && window.walletSettings.defaultContractsToDisplay.indexOf(address) >= 0) {
    return Promise.reject(new Error('Contract already exists in the list'));
  }
  return getContractInstance(account, address)
    .then((foundContract, error) => {
      if (error) {
        throw error;
      }
      return foundContract.methods.balanceOf(account).call();
    }).then((balance, error) => {
      if (error) {
        throw new Error('Invalid contract address');
      }

      const contractsAddresses = getContractsAddresses(account, netId);
      if (isDefaultContract || contractsAddresses.indexOf(address) < 0) {
        return retrieveContractDetails(account, {address: address})
          .then((contractDetails, error) => {
            if (error) {
              throw error;
            }
            if (contractDetails && !contractDetails.error) {
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
    }).then(saved => {
      if (saved && isDefaultContract) {
        return saveContractAddressAsDefault(address)
          .then(() => saved);
      }
      return saved;
    })
}

function getContractInstance(account, address) {
  try {
    const contractInstance = new window.localWeb3.eth.Contract(
        ERC20_COMPLIANT_CONTRACT_ABI, 
        address,
        {
          from: account, 
          gas: window.walletSettings.userDefaultGas,
          gasPrice: window.walletSettings.gasPrice,
          data: ERC20_COMPLIANT_CONTRACT_BYTECODE
        }
      );
    return Promise.resolve(contractInstance);
  } catch (e) {
    return Promise.reject(e);
  }
}

function transformContracDetailsToFailed(contractDetails, e) {
  contractDetails.icon = 'warning';
  contractDetails.title = contractDetails.address;
  contractDetails.error = `Error retrieving contract at specified address ${e}`;
  return contractDetails;
}