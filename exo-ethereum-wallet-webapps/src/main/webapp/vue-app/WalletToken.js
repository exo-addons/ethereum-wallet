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

      const contractDetailsPromise = getContractAtAddress(account, address)
        .then((loadedContract, error) => {
          if (error) {
            throw error;
          }
          return contractDetails.contract = loadedContract;
        })
        .then(loadedContract => loadContractDetails(account, contractDetails, loadedContract, address))
        .then(() => {
          if (!contractDetails.newWeb3Contract) {
            contractDetails.newWeb3Contract = getNewWeb3ContractInstance(account, address);
          }
          return contractDetails;
        })
        .catch(e => {
          console.debug("loadContractDetails method - error", e);
          return transformContracDetailsToFailed(contractDetails, e);
        });
      contractsDetailsPromises.push(contractDetailsPromise);
    }
  }
  return Promise.all(contractsDetailsPromises);
}

/*
 * Loads contract balance, symbol and name and cache it in sessionStorage once loaded
 */
export function loadContractDetails(account, contractDetails, contract, address) {
  if (!contractDetails || (!contract && !contractDetails.contract)) {
    throw new Error('Contract instance is mandatory');
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
    .then(balance => contractDetails.balance = parseFloat(`${balance}`))
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
    .catch((e) => {
      console.debug("loadContractDetails method - error", e);
      if(address) {
        return getContractDetailsAtAddressUsingNewWeb3(account, contractDetails, address)
          .catch(error => {
            console.debug("getContractDetailsAtAddressUsingNewWeb3 method - error", error);
            return transformContracDetailsToFailed(contractDetails, error);
          });
      } else {
        return transformContracDetailsToFailed(contractDetails, e);
      }
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
          return loadContractDetails(account, {}, foundContract)
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
    console.debug("createNewERC20TokenContract method - error", e);
    return Promise.reject(e);
  }
}


export function getNewWeb3ContractInstance(account, address) {
  const ERC20_CONTRACT = new window.localWeb3.eth.Contract(
    ERC20_COMPLIANT_CONTRACT_ABI, 
    address, 
    {
      from: account, 
      gas: window.walletSettings.userDefaultGas,
      gasPrice: window.walletSettings.gasPrice,
      data: ERC20_COMPLIANT_CONTRACT_BYTECODE
    }
  );
  return ERC20_CONTRACT;
}
/*
 * Retrieve an ERC20 contract instance at specified address
 */
export function getContractDetailsAtAddressUsingNewWeb3(account, contractDetails, address) {
  const ERC20_CONTRACT = getNewWeb3ContractInstance(account, address);
  contractDetails.newWeb3Contract = ERC20_CONTRACT;
  return ERC20_CONTRACT.methods.symbol().call()
    .then(symbol => contractDetails.symbol = symbol)
    .then(symbol => contractDetails.title = `Account in Token ${symbol}`)
    .then(() => ERC20_CONTRACT.methods.name().call())
    .then(name => contractDetails.name = name)
    .then(() => ERC20_CONTRACT.methods.balanceOf(account).call())
    .then(balance => contractDetails.balance = parseFloat(`${balance}`))
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
    .catch(e => {
      console.debug("getContractDetailsAtAddressUsingNewWeb3 method - error", e);
      contractDetails.icon = 'warning';
      contractDetails.title = contractDetails.address;
      contractDetails.error = `Error retrieving contract at specified address ${e}`;
      return contractDetails;
    });
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
    if (typeof window.web3v20 === 'undefined' || !window.web3v20) {
      ERC20_CONTRACT.setProvider(window.localWeb3.currentProvider);
    } else {
      ERC20_CONTRACT.setProvider(window.web3v20.currentProvider);
    }

    ERC20_CONTRACT.defaults({
      from: account,
      gas: window.walletSettings.userDefaultGas,
      gasPrice: window.walletSettings.gasPrice
    });
    return ERC20_CONTRACT.at(address);
  } catch (e) {
    console.debug("getContractAtAddress method - error", e);
    return Promise.reject(e);
  }
}

function transformContracDetailsToFailed(contractDetails, e) {
  contractDetails.icon = 'warning';
  contractDetails.title = contractDetails.address;
  contractDetails.error = `Error retrieving contract at specified address ${e}`;
  return contractDetails;
}