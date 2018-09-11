import {searchFullName, getContractFromStorage, getContactFromStorage} from './WalletAddressRegistry.js';
import {etherToFiat, watchTransactionStatus} from './WalletUtils.js';

export function loadStoredTransactions(networkId, account, transactions, refreshCallback) {
  return fetch(`/portal/rest/wallet/api/account/getTransactions?networkId=${networkId}&address=${account}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(transactionHashes => {
      if (transactionHashes && transactionHashes.length) {
        transactionHashes.forEach(transactionHash => {
          if (!transactionHash || !transactionHash.length) {
            return;
          }
          let transaction,receipt;
          window.localWeb3.eth.getTransaction(transactionHash)
            .then(transactionTmp => {
              transaction = transactionTmp;
              return window.localWeb3.eth.getTransactionReceipt(transactionHash);
            })
            .then(receiptTmp => {
              receipt = receiptTmp;
              return window.localWeb3.eth.getBlock(transaction.blockNumber, false);
            })
            .then(block => {
              addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000);
              if (refreshCallback) {
                refreshCallback();
              }
            })
            .catch(error => {
              throw error;
            });
        });
      }
    })
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

export function loadPendingTransactions(networkId, account, transactions, refreshCallback) {
  const pendingTransactions = getPendingTransactionFromStorage(networkId, account);
  if (!pendingTransactions || !Object.keys(pendingTransactions).length) {
    return Promise.resolve({});
  }
  return Promise.resolve(pendingTransactions)
    .then(pendingTransactions => {
      Object.keys(pendingTransactions).forEach(transactionHash => {
        addTransaction(networkId, account, transactions, pendingTransactions[transactionHash], null, null, refreshCallback);
      });
      return pendingTransactions;
    })
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

export function loadTransactions(networkId, account, transactions, fromBlockNumber, toBlockNumber, maxBlocks, progressionCallback) {
  // Retrive transactions from 1000 previous blocks (at maximum)
  // and display transactions sent/received by the current account
  return getFromBlock(fromBlockNumber, toBlockNumber, maxBlocks)
    .then(fromBlockTmp => fromBlockNumber = fromBlockTmp)
    .then(() => getToBlock(fromBlockNumber, toBlockNumber, maxBlocks))
    .then(toBlockTmp => toBlockNumber = toBlockTmp)
    .then(() => window.localWeb3.eth.getBlock(toBlockNumber, true))
    .then(lastBlock => addBlockTransactions(networkId, account, transactions, lastBlock, fromBlockNumber, 0, progressionCallback))
    .then(() => { 
      return {
        toBlock: toBlockNumber,
        fromBlock: fromBlockNumber,
        maxBlocks: maxBlocks
      }
    })
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

export function addTransaction(networkId, account, transactions, transaction, receipt, timestamp, watchLoadSuccess, watchLoadError) {
  const gasUsed = receipt && receipt.gasUsed ? receipt.gasUsed : transaction.gas;

  const status = receipt ? receipt.status : true;
  // Calculate Transaction fees
  const transactionFeeInWei = gasUsed * transaction.gasPrice;
  const transactionFeeInEth = window.localWeb3.utils.fromWei(transactionFeeInWei.toString(), 'ether');
  const transactionFeeInFiat = etherToFiat(transactionFeeInEth);

  const isReceiver = transaction.to && transaction.to.toLowerCase() === account.toLowerCase();

  // Calculate sent/received amount
  const amount = window.localWeb3.utils.fromWei(transaction.value, 'ether');
  const amountFiat = etherToFiat(amount);
  const isFeeTransaction = parseFloat(amount) === 0;

  let displayAddress = isReceiver ? transaction.from : transaction.to;

  let isContractCreationTransaction = false;

  let contractAddress = null;
  if (!displayAddress && receipt && receipt.contractAddress) {
    contractAddress = displayAddress = receipt.contractAddress;
    isContractCreationTransaction = true;
  }

  // Retrieve user or space display name, avatar and id from sessionStorage
  const contactDetails = getContactFromStorage(displayAddress, 'user', 'space');

  const transactionDetails = {
    hash: transaction.hash,
    titlePrefix: isReceiver ? 'Received from': isContractCreationTransaction ? 'Transaction spent on Contract creation ' : isFeeTransaction ? 'Transaction spent on' : 'Sent to',
    displayAddress: displayAddress,
    displayName: contactDetails.name ? contactDetails.name : displayAddress,
    isContractName: contactDetails.name ? true : false,
    avatar: contactDetails.avatar,
    name: null,
    status: status,
    color: isReceiver ? 'green' : 'red',
    icon: isFeeTransaction ? 'fa-undo' : 'fa-exchange-alt',
    amount: amount,
    amountFiat: amountFiat,
    gas: transaction.gas,
    gasUsed: gasUsed,
    gasPrice: transaction.gasPrice,
    fee: transactionFeeInEth,
    feeFiat: transactionFeeInFiat,
    isContractCreation: contractAddress,
    date: timestamp ? new Date(timestamp) : transaction.timestamp,
    pending: transaction.pending
  };

  // If user/space details wasn't found on sessionStorage,
  // then display the transaction details and in // load name and avatar with a promise
  // From eXo Platform Server
  transactions[transactionDetails.hash] = transactionDetails;

  if (transaction.pending) {
    addPendingTransactionToStorage(networkId, account, transaction);

    let loadedBlock = null;
    let loadedReceipt = null;
    watchTransactionStatus(transaction.hash, (receipt, block) => {
      window.localWeb3.eth.getTransaction(transaction.hash)
        .then(tx => {
          transaction = tx;
          addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000, watchLoadSuccess, watchLoadError);
          removePendingTransactionFromStorage(networkId, account, transaction.hash);
          if (watchLoadSuccess) {
            watchLoadSuccess();
          }
        })
        .catch(error => {
          console.debug("watchTransactionStatus method - error", error);
          if (watchLoadError) {
            watchLoadError(error);
          }
        });
    });
  }

  if (!contactDetails || !contactDetails.name) {
    // Test if address corresponds to a contract
    getContractFromStorage(account, displayAddress)
      .then(contractDetails => {
        if (contractDetails) {
          transactionDetails.displayName = `Contract ${contractDetails.symbol}`;
          transactionDetails.isContractName = true;
          transactionDetails.name = contractDetails.address;
          transactionDetails.avatar = contractDetails.avatar;
          // don't continue searching
          return false;
        }
        // continue searching
        return true;
      })
      .then(continueSearch => {
        if(continueSearch) {
          // The address is not of type contract, so search correspondin user/space display name
          return searchFullName(displayAddress);
        }
      })
      .then(item => {
        if (item && item.name && item.name.length) {
          transactionDetails.displayName = item.name;
          transactionDetails.avatar = item.avatar;
          transactionDetails.name = item.id;
        }
      });
  }
  return transactions;
}

function addBlockTransactions(networkId, account, transactions, block, untilBlockNumber, loadedBlocks, progressionCallback) {
  if (!block) {
    throw new Error("Block not found");
  }

  loadedBlocks++;

  progressionCallback(loadedBlocks);

  // If we :
  //  * already searched inside 1000 block
  //  * or we reached the genesis block
  //  * or we already displayed 10 transactions
  // then stop searching
  if (block.number === 0 || block.number <= untilBlockNumber) {
    return false;
  }
  if (block.transactions && block.transactions.length) {
    // Iterate over transactions from retrieved from block
    block.transactions.forEach(transaction => {
      // Make sure to not display transaction that hasn't a 'to' or 'from' address
      if (transaction.to && transaction.to.toLowerCase() === account.toLowerCase()
          || transaction.from && transaction.from.toLowerCase() === account.toLowerCase()) {
        window.localWeb3.eth.getTransactionReceipt(transaction.hash)
          .then(receipt => {
            addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000);
          });
      }
    });
  }

  // Continue searching in previous block
  return window.localWeb3.eth.getBlock(block.parentHash, true)
    .then(blockTmp => addBlockTransactions(networkId, account, transactions, blockTmp, untilBlockNumber, loadedBlocks, progressionCallback))
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

export function getFromBlock(fromBlock, toBlock, maxBlocks) {
  if (fromBlock != null) {
    return Promise.resolve(fromBlock);
  } else if (toBlock != null && maxBlocks != null) {
    fromBlock = toBlock - maxBlocks;
    if (fromBlock < 0) {
      fromBlock = 0;
    }
    return Promise.resolve(fromBlock);
  } else if (maxBlocks != null) {
    return window.localWeb3.eth.getBlockNumber()
      .then(lastBlock => {
        fromBlock = lastBlock - maxBlocks;
        if (fromBlock < 0) {
          fromBlock = 0;
        }
        return fromBlock;
      });
  } else {
    return Promise.resolve(0);
  }
}

export function getToBlock(fromBlock, toBlock, maxBlocks) {
  if (toBlock != null) {
    return Promise.resolve(toBlock);
  } else {
    let lastReturnedBlock = 0;

    return window.localWeb3.eth.getBlockNumber()
      .then(lastBlock => {
        const toBlockTmp = fromBlock + maxBlocks;
        if (toBlockTmp < lastBlock) {
          toBlock = toBlockTmp;
        } else {
          toBlock = lastBlock;
        }
        return toBlock;
      })
      .then(toBlock => {
        lastReturnedBlock = toBlock;
        return window.localWeb3.eth.getBlock(lastReturnedBlock, false);
      })
      .then(block => {
        if (block) {
          return lastReturnedBlock;
        } else {
          console.debug("Error getting last block, the block before will be used", lastReturnedBlock);
          // Sometimes returned last block isn't yet readable, so return the block before
          return lastReturnedBlock - 1;
        }
      });
  }
}

function addPendingTransactionToStorage(networkId, account, transaction) {
  const STORAGE_KEY = `exo-wallet-ether-transactions-progress-${networkId}-${account}`;
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    storageValue = {};
  } else {
    storageValue = JSON.parse(storageValue);
  }

  if (!storageValue[transaction.hash]) {
    storageValue[transaction.hash] = transaction;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(storageValue));
  }
}

function removePendingTransactionFromStorage(networkId, account, transactionHash) {
  const STORAGE_KEY = `exo-wallet-ether-transactions-progress-${networkId}-${account}`;
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

function getPendingTransactionFromStorage(networkId, account) {
  const STORAGE_KEY = `exo-wallet-ether-transactions-progress-${networkId}-${account}`;
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}
