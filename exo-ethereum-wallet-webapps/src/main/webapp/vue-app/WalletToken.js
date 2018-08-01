import {ERC20_COMPLIANT_CONTRACT_ABI} from './WalletConstants.js';

export function getContractsDetails(account, netId) {
  const contractsAddresses = getContractsAddresses(account, netId);
  const contractsDetailsPromises = [];

  contractsAddresses.forEach((address) => {
    const contractDetails = {};
    contractDetails.address = address;
    contractDetails.icon = 'fa-file-contract';
    contractDetails.isContract = true;
    contractDetails.contract = getContractAtAddress(account, address);
    const contractDetailsPromise = loadContractBalance(account, contractDetails);
    contractsDetailsPromises.push(contractDetailsPromise);
  });
  return Promise.all(contractsDetailsPromises);
}
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
    .catch((err) => {
      contractDetails.icon = 'warning';
      contractDetails.title = contractDetails.address;
      contractDetails.error = 'Error retrieving contract at specified address';
      return contractDetails;
    });
}
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
        console.error(e);
      }
    } else {
      throw new Error('Contract already exists');
    }
  }
}

export function getContractAtAddress(account, address) {
  const ERC20_CONTRACT = TruffleContract({
    abi: ERC20_COMPLIANT_CONTRACT_ABI
  });
  ERC20_CONTRACT.setProvider(window.localWeb3.currentProvider);
  ERC20_CONTRACT.defaults({
    from: account,
    gas: 300000
  });
  return ERC20_CONTRACT.at(address);
}

export function sendTokens(recipient, amount) {
  return this.contract.transfer(recipient, amount)
    .then(resp => {
      if (!resp.tx) {
        throw new Error('Error while proceeding transaction');
      }
      return resp;
    });
}