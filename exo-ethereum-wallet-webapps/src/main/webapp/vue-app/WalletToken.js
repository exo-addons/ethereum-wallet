import {ERC20_COMPLIANT_CONTRACT_ABI} from './WalletConstants.js';

/*
 * Get the list of Contracts with details:
 * {
 *   name: name of conract,
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

  const contractsDetailsPromises = [];

  contractsAddresses.forEach((address) => {
    const contractDetails = {};
    contractDetails.address = address;
    contractDetails.icon = 'fa-file-contract';
    contractDetails.isContract = true;
    contractDetails.address = address;
    contractDetails.isDefault = window.walletSettings.defaultContractsToDisplay 
                                && window.walletSettings.defaultContractsToDisplay.indexOf(address) > -1;
    contractDetails.contract = getContractAtAddress(account, address);
    const contractDetailsPromise = loadContractBalance(account, contractDetails);
    contractsDetailsPromises.push(contractDetailsPromise);
  });
  return Promise.all(contractsDetailsPromises);
}

/*
 * Loads contract balance and cache it in sessionStorage once loaded
 */
export function loadContractBalance(account, contractDetails) {
  return contractDetails.contract.symbol.call()
    .then(symbol => contractDetails.symbol = symbol)
    .then(symbol => contractDetails.title = `Account in Token ${symbol}`)
    .then(() => contractDetails.contract.name.call())
    .then(name => contractDetails.name = name)
    .then(() => contractDetails.contract.balanceOf.call(account))
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
 * Validate Contract existence and save its address in localStorage
 */
export function saveContractAddress(account, address, netId) {
  if (getContractAtAddress(account, address)) {
    const contractsAddresses = getContractsAddresses(account, netId);
    if (contractsAddresses.indexOf(address) < 0) {
      let contract = null;
      try {
        contract = getContractAtAddress(account, address);
        return contract.balanceOf.call(account)
          .then((balance) => {
            if (balance === 0 || balance) {
              contractsAddresses.push(address);
              const contractsAddressesString = JSON.stringify(contractsAddresses);
              localStorage.setItem(`exo-wallet-contracts-${account}-${netId}`.toLowerCase(), contractsAddressesString);
              return true;
            } else {
              return false;
            }
          });
      } catch (e) {
        console.warn('Error while saving contract', e);
      }
    } else {
      throw new Error('Contract already exists');
    }
  }
}

/*
 * Construct an ERC20 contract instance using Truffle
 */
export function getContractAtAddress(account, address) {
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
}