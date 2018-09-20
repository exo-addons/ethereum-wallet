import {searchFullName, getContractFromStorage} from './WalletAddressRegistry.js';
import {etherToFiat, watchTransactionStatus} from './WalletUtils.js';
import {getContractInstance} from './WalletToken.js';
import {ERC20_COMPLIANT_CONTRACT_ABI} from './WalletConstants.js';

export function loadPendingTransactions(networkId, account, transactions, refreshCallback) {
  const pendingTransactions = getPendingTransactionFromStorage(networkId, account);
  if (!pendingTransactions || !Object.keys(pendingTransactions).length) {
    return Promise.resolve({});
  }

  const loadingPromises = [];

  Object.keys(pendingTransactions).forEach(transactionHash => {
    loadingPromises.push(addTransaction(networkId, account, transactions, pendingTransactions[transactionHash], null, null, refreshCallback));
  });

  return Promise.all(loadingPromises)
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

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
        const loadingPromises = [];

        transactionHashes.forEach(transactionHash => {
          if (!transactionHash || !transactionHash.length) {
            return [];
          }

          let transaction,receipt;

          const loadingPromise = window.localWeb3.eth.getTransaction(transactionHash)
            .then(transactionTmp => {
              transaction = transactionTmp;
              return window.localWeb3.eth.getTransactionReceipt(transactionHash);
            })
            .then(receiptTmp => {
              receipt = receiptTmp;
              return window.localWeb3.eth.getBlock(transaction.blockNumber, false);
            })
            .then(block => {
              return addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000);
            })
            .then(transactionDetails => {
              if (refreshCallback) {
                return refreshCallback();
              }

              return transactionDetails;
            })
            .catch(error => {
              throw error;
            });

          loadingPromises.push(loadingPromise);
        });

        return Promise.all(loadingPromises);
      }
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

  const fromAddress = transaction.from && transaction.from.toLowerCase();
  const toAddress = transaction.to && transaction.to.toLowerCase();

  account = account.toLowerCase();

  const isReceiver = toAddress === account;

  // Calculate sent/received amount
  const amount = parseFloat(window.localWeb3.utils.fromWei(transaction.value, 'ether'));
  const amountFiat = etherToFiat(amount);
  const isFeeTransaction = amount === 0;

  // Retrieve user or space display name, avatar and id from sessionStorage
  const transactionDetails = {
    hash: transaction.hash,
    isContractName: false,
    avatar: null,
    name: null,
    status: status,
    type: isFeeTransaction ? 'contract' : 'ether',
    isReceiver: isReceiver,
    isContractCreation: !toAddress && receipt && receipt.contractAddress && receipt.logs && !receipt.logs.length,
    contractAddress: null,
    contractName: null,
    contractSymbol: null,
    contractMethodName: null,
    contractAmount: null,
    fromAddress: fromAddress,
    fromUsername: null,
    fromAvatar: null,
    fromDisplayName: null,
    toAddress: isFeeTransaction ? null : toAddress,
    toUsername: null,
    toAvatar: null,
    toDisplayName: null,
    balanceAtDateFiat: null,
    balanceAtDate: null,
    amount: amount,
    amountFiat: amountFiat,
    gas: transaction.gas,
    gasUsed: gasUsed,
    gasPrice: transaction.gasPrice,
    fee: transactionFeeInEth,
    feeFiat: transactionFeeInFiat,
    date: timestamp ? new Date(timestamp) : transaction.timestamp,
    pending: transaction.pending
  };

  if (transaction.pending) {
    addPendingTransactionToStorage(networkId, account, transaction);

    let loadedBlock = null;
    let loadedReceipt = null;
    watchTransactionStatus(transaction.hash, (receipt, block) => {
      window.localWeb3.eth.getTransaction(transaction.hash)
        .then(tx => {
          transaction = tx;

          removePendingTransactionFromStorage(networkId, account, transaction.hash);

          return addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000, watchLoadSuccess, watchLoadError);
        })
        .then(() => {
          if (watchLoadSuccess) {
            watchLoadSuccess(transaction);
          }
        })
        .catch(error => {
          console.debug("watchTransactionStatus method - error", error);
          if (watchLoadError) {
            watchLoadError(error, transaction);
          }
        });
    });
  }

  // Test if address corresponds to a contract
  transactionDetails.contractAddress = transactionDetails.isContractCreation ? receipt.contractAddress : toAddress;
  return getContractFromStorage(account, transactionDetails.contractAddress)
    .then(contractDetails => {
      if (contractDetails) {
        transactionDetails.type = 'contract';
        transactionDetails.contractName = contractDetails.name;
        transactionDetails.contractAddress = contractDetails.address;
        transactionDetails.contractSymbol = contractDetails.symbol;

        try {
          if (transactionDetails.isContractCreation) {
            return false;
          } else if (receipt && receipt.logs) {
            if (!abiDecoder.getABIs() || !abiDecoder.getABIs().length) {
              abiDecoder.addABI(ERC20_COMPLIANT_CONTRACT_ABI);
            }

            try {
              const method = abiDecoder.decodeMethod(transaction.input);
              const decodedLogs = abiDecoder.decodeLogs(receipt.logs);

              if (method && method.name) {
                transactionDetails.contractMethodName = method.name;

                if (method.name === 'transfer') {
                  transactionDetails.toAddress = decodedLogs[0].events[1].value.toLowerCase();
                  transactionDetails.contractAmount = decodedLogs[0].events[2].value;
                  return searchFullName(transactionDetails.toAddress)
                    .then(item => {
                      if (item && item.name && item.name.length) {
                        transactionDetails.toDisplayName = item.name;
                        transactionDetails.toAvatar = item.avatar;
                        transactionDetails.toUsername = item.id;
                      }
                      return false;
                    });
                } else if (method.name === 'approve') {
                  transactionDetails.toAddress = decodedLogs[0].events[1].value.toLowerCase();
                  transactionDetails.contractAmount = decodedLogs[0].events[2].value;
                  return searchFullName(transactionDetails.toAddress)
                    .then(item => {
                      if (item && item.name && item.name.length) {
                        transactionDetails.toDisplayName = item.name;
                        transactionDetails.toAvatar = item.avatar;
                        transactionDetails.toUsername = item.id;
                      }
                      return false;
                    });
                } else if (method.name === 'transferFrom') {
                  transactionDetails.fromAddress = decodedLogs[0].events[0].value.toLowerCase();
                  transactionDetails.toAddress = decodedLogs[0].events[1].value.toLowerCase();
                  transactionDetails.contractAmount = decodedLogs[0].events[2].value;

                  return searchFullName(transactionDetails.toAddress)
                    .then(item => {
                      if (item && item.name && item.name.length) {
                        transactionDetails.toDisplayName = item.name;
                        transactionDetails.toAvatar = item.avatar;
                        transactionDetails.toUsername = item.id;
                      }
                      return searchFullName(transactionDetails.fromAddress);
                    })
                    .then(item => {
                      if (item && item.name && item.name.length) {
                        transactionDetails.fromDisplayName = item.name;
                        transactionDetails.fromAvatar = item.avatar;
                        transactionDetails.fromUsername = item.id;
                      }
                      return false;
                    });
                }
              }
            } catch(e) {
              console.debug('error while decoding transaction', transaction ,e);
            }
          } else {
            return false;
          }
        } catch(e) {
          console.debug('Error resolving Contract transaction status', e);
          // don't continue searching
          return false;
        }
      }
      return !transactionDetails.isContractCreation;
    })
    .then(continueSearch => {
      if (continueSearch) {
        transactionDetails.type = 'ether';

        // The address is not of type contract, so search corresponding user/space display name
        return searchFullName(isReceiver ? fromAddress : toAddress)
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        if (isReceiver) {
          transactionDetails.fromDisplayName = item.name;
          transactionDetails.fromAvatar = item.avatar;
          transactionDetails.fromUsername = item.id;
        } else {
          transactionDetails.toDisplayName = item.name;
          transactionDetails.toAvatar = item.avatar;
          transactionDetails.toUsername = item.id;
        }
      }

      // If user/space details wasn't found on sessionStorage,
      // then display the transaction details and in // load name and avatar with a promise
      // From eXo Platform Server
      transactions[transactionDetails.hash] = transactionDetails;
    })
    .then(() => {
      if (transaction.blockNumber && transactionDetails.status && !transactionDetails.pending) {
        return window.localWeb3.eth.getBalance(account, transaction.blockNumber)
          .then(balanceAtDate => {
            if (balanceAtDate) {
              transactionDetails.balanceAtDate = window.localWeb3.utils.fromWei(balanceAtDate, 'ether');
              transactionDetails.balanceAtDateFiat = etherToFiat(transactionDetails.balanceAtDate);
            }
          });
      }

      return transactionDetails;
    })
    .catch(error => {
      console.debug('Error retrieving transaction details', transaction, error);
    });
}

function addBlockTransactions(networkId, account, transactions, block, untilBlockNumber, loadedBlocks, progressionCallback) {
  if (!block) {
    throw new Error("An error occurred while retrieving data from network, this may be fixed by refreshing your wallet");
  }

  loadedBlocks++;

  progressionCallback(loadedBlocks);

  // If we :
  //  * already searched inside 1000 block
  //  * or we reached the genesis block
  //  * or we already displayed 10 transactions
  // then stop searching
  if (block.number === 0 || block.number <= untilBlockNumber || !account) {
    return false;
  }

  account = account.toLowerCase();

  if (block.transactions && block.transactions.length) {
    // Iterate over transactions from retrieved from block
    block.transactions.forEach(transaction => {
      const fromAddress = transaction.from && transaction.from.toLowerCase();
      const toAddress = transaction.to && transaction.to.toLowerCase();

      // Make sure to not display transaction that hasn't a 'to' or 'from' address
      if (toAddress === account || fromAddress === account) {
        window.localWeb3.eth.getTransactionReceipt(transaction.hash)
          .then(receipt => {
            return addTransaction(networkId, account, transactions, transaction, receipt, block.timestamp * 1000);
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
