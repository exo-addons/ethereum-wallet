import {convertTokenAmountReceived, computeBalance} from './WalletUtils.js';

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
  let contractsAddresses = [];
  if (onlyDefault) {
    contractsAddresses = window.walletSettings.defaultContractsToDisplay || [];
  } else {
    let overviewAccounts = window.walletSettings.userPreferences.overviewAccountsToDisplay || [];
    contractsAddresses = overviewAccounts.filter(contractAddress => contractAddress && contractAddress.indexOf('0x') === 0);
  }

  const contractsDetailsPromises = [];
  for (let i = 0; i < contractsAddresses.length; i++) {
    const address = contractsAddresses[i];
    if (address && address.trim().length) {
      const contractDetails = {};
      contractDetails.address = address;
      contractDetails.icon = 'fa-file-contract';
      contractDetails.isContract = true;
      contractDetails.networkId = netId;
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
  console.debug(`retrieve contract at address ${contractDetails && contractDetails.address} with account ${account}`);
  let adminLevelComputed, approveAccountComputed, computeContractType = !contractDetails.contractType;
  contractDetails.retrievedAttributes = 0;
  let contractToSave = false;
  return getContractInstance(account, contractDetails.address, true)
    .then((contractInstance, error) => {
      if (error) {
        throw error;
      }
      if (!contractInstance) {
        throw new Error('Can\'t find contract instance');
      }
      return contractDetails.contract = contractInstance;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error retrieving instance", contractDetails.address, new Error(e));
    })
    .then(() => getSavedContractDetails(contractDetails.address, contractDetails.networkId))
    .then(savedDetails => {
      if (savedDetails) {
        if(!savedDetails || !savedDetails.owner || !savedDetails.decimals || !savedDetails.sellPrice) {
          contractToSave = true;
        }
        Object.keys(savedDetails).forEach(key => {
          contractDetails[key] = savedDetails[key];
        });
        // Convert to numbers
        if(contractDetails.contractType) {
          contractDetails.contractType = Number(contractDetails.contractType);
        }
        if(contractDetails.sellPrice) {
          contractDetails.sellPrice = Number(contractDetails.sellPrice);
        }
      }
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error retrieving saved details", contractDetails.address, new Error(e));
    })
    .then(() => contractDetails.contract.methods.symbol().call())
    .then(symbol => {
      contractDetails.symbol = symbol;
      contractDetails.retrievedAttributes++;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error retrieving symbol", contractDetails.address, new Error(e));
    })
    .then(() => contractDetails.contract.methods.name().call())
    .then(name => {
      contractDetails.name = name;
      contractDetails.title = name;
      contractDetails.retrievedAttributes++;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error retrieving name", contractDetails.address, new Error(e));
    })
    .then(() => contractDetails.contract.methods.decimals().call())
    .then(decimals => {
      contractDetails.decimals = decimals || 0;
      contractDetails.retrievedAttributes++;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error retrieving decimals", contractDetails.address, new Error(e));
    })
    .then(() => computeBalance(contractDetails.address))
    .then((contractBalance) => {
      if (contractBalance) {
        contractDetails.contractBalance = contractBalance.balance;
        contractDetails.contractBalanceFiat = contractBalance.balanceFiat;
      }
    })
    .then(() =>
      contractDetails.contract.methods.getSellPrice().call()
        .then(sellPrice => {
          contractDetails.contractType = 1;
          contractDetails.sellPrice = window.localWeb3.utils.fromWei(String(sellPrice), "ether");
          contractDetails.retrievedAttributes++;
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving sellPrice", contractDetails.address, new Error(e));
        })
        .then(() => contractDetails.contractType && contractDetails.contract.methods.owner().call())
        .then(owner =>  {
          if (owner) {
            contractDetails.owner = owner;
            contractDetails.isOwner = owner.toLowerCase() === account && account.toLowerCase();
          }
          contractDetails.retrievedAttributes++;
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving sellPrice", contractDetails.address, new Error(e));
        })
        .then(() => contractDetails.contractType && contractDetails.contract.methods.totalSupply().call())
        .then(totalSupply =>  {
          if (totalSupply) {
            contractDetails.totalSupply = totalSupply;
          }
          contractDetails.retrievedAttributes++;
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving totalSupply", contractDetails.address, new Error(e));
        })
        .then(() => contractDetails.contractType && contractDetails.contract.methods.isApprovedAccount && contractDetails.contract.methods.isApprovedAccount(account).call())
        .then(approvedAccount =>  {
          contractDetails.contractType = 1;
          contractDetails.isApproved = approvedAccount;
          approveAccountComputed = true;
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving isApprovedAccount", contractDetails.address, new Error(e));
        })
        .then(() => contractDetails.contractType && contractDetails.contract.methods.getAdminLevel && contractDetails.contract.methods.getAdminLevel(account).call())
        .then(habilitationLevel =>  {
          contractDetails.contractType = 1;
          if (habilitationLevel) {
            adminLevelComputed = true;
            contractDetails.adminLevel = Number(habilitationLevel);
            contractDetails.isAdmin = habilitationLevel > 0;
            if (contractDetails.isAdmin) {
              contractDetails.isApproved = true;
            }
          }
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving getAdminLevel", contractDetails.address, new Error(e));
        })
        .then(() => contractDetails.contractType && contractDetails.contract.methods.isPaused && contractDetails.contract.methods.isPaused().call())
        .then(isPaused =>  {
          contractDetails.isPaused = isPaused ? true: false;
          contractDetails.retrievedAttributes++;
        })
        .catch(e => {
          console.debug("retrieveContractDetails method - error retrieving isPaused", contractDetails.address, new Error(e));
        }).
        finally(() => {
          if (contractDetails.retrievedAttributes === 0) {
            transformContracDetailsToFailed(contractDetails);
          } else if (contractDetails.retrievedAttributes > 5) {
            if (contractToSave || contractDetails.contractType === 0) {
              saveContractAddressAsDefault(contractDetails);
            }
            contractDetails.contractType = 1;
          } else if (computeContractType && !adminLevelComputed && !approveAccountComputed) {
            contractDetails.contractType = 0;
          }
          if (contractDetails.contractType === 1) {
            contractDetails.contractTypeLabel = 'ERT Token';
          } else {
            contractDetails.contractTypeLabel = 'Standard ERC20 Token';
          }
        })
    )
    .then(() => contractDetails.contract.methods.balanceOf(account).call())
    .then(balance => {
      contractDetails.balance = convertTokenAmountReceived(balance, contractDetails.decimals);
      return contractDetails;
    })
    .catch(e => {
      console.debug("retrieveContractDetails method - error", account, contractDetails, new Error(e), e);
      return contractDetails;
    })
    .finally(() => {
      console.debug(`finished retrieving contract at address ${contractDetails && contractDetails.address} with account ${account}`);
    });
}

/*
 * Creates Web3 conract deployment transaction
 */
export function newContractInstance(abi, bin, ...args) {
  if (!abi || !bin) {
    return null;
  }
  if (args && args.length) {
    return new window.localWeb3.eth.Contract(abi).deploy({
      data: bin,
      arguments: args
    });
  } else {
    return new window.localWeb3.eth.Contract(abi).deploy({
      data: bin
    });
  }
}

/*
 * Creates Web3 conract deployment transaction
 */
export function deployContract(contractInstance, account, gasLimit, gasPrice, transactionHashCallback) {
  let transactionHash;
  return contractInstance.send({
    from: account,
    gas: gasLimit,
    gasPrice: gasPrice
  })
    .on('transactionHash', hash => {
      return transactionHashCallback && transactionHashCallback(transactionHash = hash);
    })
    .on('receipt', receipt => {
      return transactionHashCallback && transactionHashCallback(transactionHash);
    });
}

/*
 * Retrieve contract details from eXo Platform server if exists
 */
export function getSavedContractDetails(address, networkId) {
  return fetch(`/portal/rest/wallet/api/contract/getContract?address=${address}&networkId=${networkId}`, {
    method: 'GET',
    credentials: 'include'
 })
 .then(resp => {
   if(resp && resp.ok) {
     return resp.json();
   } else {
     throw new Error(`Error getting contract details from server with address ${address} on network with id ${networkId}`);
   }
 })
 .then(contractDetails => contractDetails && contractDetails.address ? contractDetails : null)
 .catch(e => {
   console.debug("Error getting contract details from server", e);
   return null; 
 });
}

/*
 * Creates a Web3 conract instance
 */
export function newContractInstanceByName(tokenName, ...args) {
  let contractBin, contractAbi;
  return fetch(`/portal/rest/wallet/api/contract/bin/${tokenName}`, {
       method: 'GET',
       credentials: 'include'
    })
    .then(resp => {
      if(resp && resp.ok) {
        return resp.text();
      } else {
        throw new Error(`Cannot find contract BIN with name ${tokenName}`);
      }
    })
    .then(bin => contractBin = bin.indexOf('0x') === 0 ? bin : `0x${bin}`)
    .then(() => fetch(`/portal/rest/wallet/api/contract/abi/${tokenName}`, {
      method: 'GET',
      credentials: 'include'
    }))
    .then(resp => {
      if(resp && resp.ok) {
        return resp.json();
      } else {
        throw new Error(`Cannot find contract ABI with name ${tokenName}`);
      }
    })
    .then(abi => contractAbi = abi)
    .then(() => newContractInstance(contractAbi, contractBin, ...args));
}

/*
 * Creates a Web3 conract instance
 */
export function newContractInstanceByNameAndAddress(tokenName, tokenAddress) {
  let contractBin, contractAbi;
  return fetch(`/portal/rest/wallet/api/contract/bin/${tokenName}`, {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => {
    if(resp && resp.ok) {
      return resp.text();
    } else {
      throw new Error(`Cannot find contract BIN with name ${tokenName}`);
    }
  })
  .then(bin => contractBin = bin.indexOf('0x') === 0 ? bin : `0x${bin}`)
  .then(() => fetch(`/portal/rest/wallet/api/contract/abi/${tokenName}`, {
    method: 'GET',
    credentials: 'include'
  }))
  .then(resp => {
    if(resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error(`Cannot find contract ABI with name ${tokenName}`);
    }
  })
  .then(abi => contractAbi = abi)
  .then(() => getContractInstance(window.localWeb3.eth.defaultAccount, tokenAddress, false, contractAbi, contractBin));
}

export function estimateContractDeploymentGas(instance) {
  return instance.estimateGas((error, estimatedGas) => {
    if (error) {
      throw new Error(`Error while estimating contract deployment gas ${error}`);
    }
    return estimatedGas;
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
export function saveContractAddressAsDefault(contractDetails) {
  contractDetails.address = contractDetails.address.toLowerCase();
  console.debug("save contract as default", contractDetails);
  return fetch('/portal/rest/wallet/api/contract/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: contractDetails.name,
      symbol: contractDetails.symbol,
      address: contractDetails.address,
      networkId: contractDetails.networkId,
      owner: contractDetails.owner,
      contractType: contractDetails.contractType,
      sellPrice: contractDetails.sellPrice,
      decimals: contractDetails.decimals
    })
  })
    .then(resp => {
      if (!window.walletSettings.defaultContractsToDisplay) {
        window.walletSettings.defaultContractsToDisplay = [];
      }
      if (resp && resp.ok && window.walletSettings.defaultContractsToDisplay.indexOf(contractDetails.address) < 0) {
        window.walletSettings.defaultContractsToDisplay.push(contractDetails.address);
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
  return getContractInstance(account, address, true)
    .then((foundContract, error) => {
      if (error) {
        throw error;
      }
      // Test on existence of balanceOf method in contract code
      return foundContract.methods.balanceOf(account).call();
    }).then((balance, error) => {
      if (error) {
        throw new Error('Invalid contract address');
      }

      let overviewAccounts = window.walletSettings.userPreferences.overviewAccounts || [];
      overviewAccounts = overviewAccounts.filter(contractAddress => contractAddress && contractAddress.indexOf('0x') === 0);
      if (isDefaultContract || overviewAccounts.indexOf(address) < 0) {
        return retrieveContractDetails(account, {address: address, networkId: netId})
          .then((contractDetails, error) => {
            if (error) {
              throw error;
            }
            if (contractDetails && !contractDetails.error) {
              return contractDetails;
            } else {
              return false;
            }
          });
      } else if (!isDefaultContract) {
        throw new Error('Contract already exists');
      }
    }).then(contractDetails => {
      if (contractDetails && isDefaultContract) {
        return saveContractAddressAsDefault(contractDetails).then(() => contractDetails);
      }
      return contractDetails;
    })
}

export function getContractDeploymentTransactionsInProgress(networkId) {
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${networkId}`;
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}

export function removeContractDeploymentTransactionsInProgress(networkId, transactionHash) {
  const STORAGE_KEY = `exo-wallet-contract-deployment-progress-${networkId}`;
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return;
  } else {
    storageValue = JSON.parse(storageValue);
  }

  if (storageValue[transactionHash]) {
    delete storageValue[transactionHash];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(storageValue));
  }
}

export function getContractInstance(account, address, usePromise, abi, bin) {
  try {
    const contractInstance = new window.localWeb3.eth.Contract(
        abi ? abi : window.walletSettings.contractAbi,
        address,
        {
          from: account && account.toLowerCase(),
          gas: window.walletSettings.userPreferences.defaultGas,
          gasPrice: window.walletSettings.normalGasPrice,
          data: bin ? bin : window.walletSettings.contractBin
        }
      );
    if (usePromise) {
      return Promise.resolve(contractInstance);
    } else {
      return contractInstance;
    }
  } catch (e) {
    console.debug('An error occurred while retrieving contract instance', new Error(e));
    if (usePromise) {
      return Promise.reject(e);
    } else {
      return null;
    }
  }
}

function transformContracDetailsToFailed(contractDetails, e) {
  contractDetails.icon = 'warning';
  contractDetails.title = contractDetails.address;
  contractDetails.error = `Error retrieving contract at specified address ${e ? e : ''}`;
  return contractDetails;
}
