import {ERC20_COMPLIANT_CONTRACT_ABI} from './WalletConstants.js';
import TruffleContract from 'truffle-contract';

export function getContractsDetails(account) {
  const contractsAddresses = getContractsAddresses();
  const contractDetails = {};
  const contractsDetailsPromises = [];

  contractsAddresses.forEach((address) => {
    contractDetails.address = address;
    contractDetails.icon = 'fa-file-contract';
    contractDetails.isContract = true;
    contractDetails.contract = getContractAtAddress(account, address);
    contractsDetailsPromises.push(contractDetails.contract.symbol.call()
      .then(symbol => contractDetails.symbol = symbol)
      .then(symbol => contractDetails.title = `Account in  ${symbol}`)
      .then(() => contractDetails.contract.balanceOf.call(account))
      .then(balance => contractDetails.balance = `${balance}`)
      .then(() => contractDetails)
    );
  });
  console.log(contractsDetailsPromises);
  return Promise.all(contractsDetailsPromises);
}
export function getContractsAddresses() {
  const contractsAddressesString = localStorage.getItem('wallet-contracts');
  let contractsAddresses = null;
  if (!contractsAddressesString) {
    contractsAddresses = [];
  } else {
    contractsAddresses = JSON.parse(contractsAddressesString);
  }
  return contractsAddresses;
}
export function saveContractAddress(account, address) {
  if (getContractAtAddress(account, address)) {
    const contractsAddresses = getContractsAddresses();
    if (contractsAddresses.indexOf(address) < 0) {
      contractsAddresses.push(address);
      const contractsAddressesString = JSON.stringify(contractsAddresses);
      localStorage.setItem('wallet-contracts', contractsAddressesString);
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