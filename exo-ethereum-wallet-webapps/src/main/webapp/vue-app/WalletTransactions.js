import {searchFullName, getContractFromStorage} from './WalletAddressRegistry.js';
import {etherToFiat, watchTransactionStatus} from './WalletUtils.js';
import {getContractInstance} from './WalletToken.js';
import {ERC20_COMPLIANT_CONTRACT_ABI} from './WalletConstants.js';

export function loadPendingTransactions(networkId, account, contractDetails, transactions, refreshCallback) {
  const pendingTransactions = getPendingTransactionFromStorage(networkId, account, contractDetails);
  if (!pendingTransactions || !Object.keys(pendingTransactions).length) {
    return Promise.resolve({});
  }

  const loadingPromises = [];

  Object.keys(pendingTransactions).forEach(transactionHash => {
    loadingPromises.push(addTransaction(networkId, account, contractDetails, transactions, pendingTransactions[transactionHash], null, null, refreshCallback));
  });

  return Promise.all(loadingPromises)
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

export function loadStoredTransactions(networkId, account, contractDetails, transactions, refreshCallback) {
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

              // if this is about loading a contract transactions, ignore other transactions
              if (!transaction || (contractDetails.isContract && (!transaction.to || transaction.to.toLowerCase() !== contractDetails.address))) {
                transaction.ignore = true;
              } else {
                return window.localWeb3.eth.getTransactionReceipt(transactionHash);
              }
            })
            .then(receiptTmp => {
              if (transaction.ignore) {
                return;
              }

              receipt = receiptTmp;
              return window.localWeb3.eth.getBlock(transaction.blockNumber, false);
            })
            .then(block => {
              if (transaction.ignore) {
                return;
              }

              return addTransaction(networkId, account, contractDetails, transactions, transaction, receipt, block.timestamp * 1000);
            })
            .then(transactionDetails => {
              if (transaction.ignore) {
                return;
              }

              if (transactionDetails && refreshCallback) {
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

export function loadTransactions(networkId, account, contractDetails, transactions, fromBlockNumber, toBlockNumber, maxBlocks, progressionCallback) {
  account = account.toLowerCase();

  if (contractDetails.isContract) {
    // Load Transfer events for user as sender
    return contractDetails.contract.getPastEvents("Transfer", {
      fromBlock: fromBlockNumber ? fromBlockNumber : 0,
      filter: {
        isError: 0,
        txreceipt_status: 1
      },
      topics: [
        window.localWeb3.utils.sha3("Transfer(address,address,uint256)"),
        window.localWeb3.utils.padLeft(account, 64),
        null
      ]
    })
      .then(events => addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_from", "_to", progressionCallback))
      // Load Transfer events for user as receiver
      .then(() => contractDetails.contract.getPastEvents("Transfer", {
          fromBlock: fromBlockNumber ? fromBlockNumber : 0,
          filter: {
            isError: 0,
            txreceipt_status: 1
          },
          topics: [
            window.localWeb3.utils.sha3("Transfer(address,address,uint256)"),
            null,
            window.localWeb3.utils.padLeft(account, 64)
          ]
      }))
      .then(events => addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_from", "_to", progressionCallback))
      // Load Approval events for user as receiver
      .then(() => contractDetails.contract.getPastEvents("Approval", {
          fromBlock: fromBlockNumber ? fromBlockNumber : 0,
              filter: {
                isError: 0,
                txreceipt_status: 1
              },
              topics: [
                window.localWeb3.utils.sha3("Approval(address,address,uint256)"),
                window.localWeb3.utils.padLeft(account, 64),
                null
              ]
      }))
      .then(events => addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_owner", "_spender", progressionCallback))
      // Load Approval events for user as sender
      .then(() => contractDetails.contract.getPastEvents("Approval", {
          fromBlock: fromBlockNumber ? fromBlockNumber : 0,
              filter: {
                isError: 0,
                txreceipt_status: 1
              },
              topics: [
                window.localWeb3.utils.sha3("Approval(address,address,uint256)"),
                null,
                window.localWeb3.utils.padLeft(account, 64)
              ]
      }))
      .then(events => addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_owner", "_spender", progressionCallback))

      .then(() => transactions)
      .catch(e => {
        console.debug("Error occurred while retrieving contract transactions", e);
        throw e;
      });
  } else {
    // Retrive transactions from previous blocks (at maximum)
    // and display transactions sent/received by the current account
    return getFromBlock(fromBlockNumber, toBlockNumber, maxBlocks)
      .then(fromBlockTmp => fromBlockNumber = fromBlockTmp)
      .then(() => getToBlock(fromBlockNumber, toBlockNumber, maxBlocks))
      .then(toBlockTmp => toBlockNumber = toBlockTmp)
      .then(() => window.localWeb3.eth.getBlock(toBlockNumber, true))
      .then(lastBlock => addBlockTransactions(networkId, account, contractDetails, transactions, lastBlock, fromBlockNumber, 0, progressionCallback))
      .then(() => {
        return {
          toBlock: toBlockNumber,
          fromBlock: fromBlockNumber,
          maxBlocks: maxBlocks
        }
      })
      .catch(error => {
        console.debug("Error occurred while retrieving transactions", error);
        if (`${error}`.indexOf('stopLoading') < 0) {
          throw error;
        }
      });
  }
}

export function addTransaction(networkId, account, contractDetails, transactions, transaction, receipt, timestamp, watchLoadSuccess, watchLoadError) {
  if(!transaction || !networkId || !account) {
    console.debug("Wrong paramters for addTransaction method", networkId, account, transaction);
    return;
  }

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
  const amount = transaction.value ? parseFloat(window.localWeb3.utils.fromWei(transaction.value, 'ether')) : 0;
  const amountFiat = amount ? etherToFiat(amount) : 0;
  const isFeeTransaction = amount === 0;

  // Retrieve user or space display name, avatar and id from sessionStorage
  const transactionDetails = {
    hash: transaction.hash,
    status: status,
    type: isFeeTransaction ? 'contract' : 'ether',
    isReceiver: isReceiver,
    isContractCreation: !toAddress && receipt && receipt.contractAddress,
    contractDetails: null,
    contractAddress: transaction.contractAddress,
    contractName: null,
    contractSymbol: null,
    contractMethodName: transaction.contractMethodName,
    contractAmount: transaction.contractAmount,
    fromAddress: fromAddress,
    fromUsername: null,
    fromType: null,
    fromAvatar: null,
    fromDisplayName: null,
    toAddress: toAddress,
    toUsername: null,
    toType: null,
    toAvatar: null,
    toDisplayName: null,
    balanceAtDateFiat: null,
    balanceAtDate: null,
    amount: isFeeTransaction ? Number(transaction.contractAmount) : amount,
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
    addPendingTransactionToStorage(networkId, account, contractDetails, transaction);

    let loadedBlock = null;
    let loadedReceipt = null;
    watchTransactionStatus(transaction.hash, (receipt, block) => {
      window.localWeb3.eth.getTransaction(transaction.hash)
        .then(tx => {
          transaction = tx;

          removePendingTransactionFromStorage(networkId, account, contractDetails, transaction.hash);

          return addTransaction(networkId, account, contractDetails, transactions, transaction, receipt, block.timestamp * 1000, watchLoadSuccess, watchLoadError);
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
  if (!transactionDetails.contractAddress) {
    transactionDetails.contractAddress = transactionDetails.isContractCreation ? receipt.contractAddress : toAddress;
  }

  if (!transactionDetails.contractAddress) {
    transactionDetails.type = 'ether';
  }

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
            const method = abiDecoder.decodeMethod(transaction.input);
            const decodedLogs = abiDecoder.decodeLogs(receipt.logs);
            if (method && method.name) {
              transactionDetails.contractMethodName = method.name;
              if (method.name === 'transfer' || method.name === 'approve') {
                transactionDetails.fromAddress = decodedLogs[0].events[0].value.toLowerCase();
                transactionDetails.toAddress = decodedLogs[0].events[1].value.toLowerCase();
                transactionDetails.contractAmount = decodedLogs[0].events[2].value;
                transactionDetails.isReceiver = transactionDetails.toAddress === account;
              } else if (method.name === 'transferFrom') {
                transactionDetails.fromAddress = decodedLogs[0].events[0].value.toLowerCase();
                transactionDetails.toAddress = decodedLogs[0].events[1].value.toLowerCase();
                transactionDetails.byAddress = transaction.from.toLowerCase();
                transactionDetails.contractAmount = decodedLogs[0].events[2].value;
                transactionDetails.isReceiver = false;
              }
            }
          }
        } catch(e) {
          console.debug('Error resolving Contract transaction status', e);
        }
      }
    })
    .then(() => {
      if (!transactionDetails.isContractCreation && transactionDetails.fromAddress) {
        return searchFullName(transactionDetails.fromAddress);
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        transactionDetails.fromDisplayName = item.name;
        transactionDetails.fromAvatar = item.avatar;
        transactionDetails.fromUsername = item.id;
        transactionDetails.fromType = item.type;
        transactionDetails.fromTechnicalId = item.technicalId;
      }
    })
    .then(() => {
      if (!transactionDetails.isContractCreation && transactionDetails.toAddress) {
        return searchFullName(transactionDetails.toAddress);
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        transactionDetails.toDisplayName = item.name;
        transactionDetails.toAvatar = item.avatar;
        transactionDetails.toUsername = item.id;
        transactionDetails.toType = item.type;
        transactionDetails.toTechnicalId = item.technicalId;
      }
    })
    .then(() => {
      if (transactionDetails.byAddress) {
        return searchFullName(transactionDetails.byAddress);
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        transactionDetails.byDisplayName = item.name;
        transactionDetails.byAvatar = item.avatar;
        transactionDetails.byUsername = item.id;
        transactionDetails.byType = item.type;
        transactionDetails.byTechnicalId = item.technicalId;
      }
    })
    .then(() => {
      if (transactionDetails.pending
          || transaction.from.toLowerCase() === account
          || (transaction.to && transaction.to.toLowerCase() === account)
          || transactionDetails.fromAddress.toLowerCase() === account
          || transactionDetails.toAddress.toLowerCase() === account) {
        // If user/space details wasn't found on sessionStorage,
        // then display the transaction details and in // load name and avatar with a promise
        // From eXo Platform Server
        return transactions[transactionDetails.hash] = transactionDetails;
      } else {
        console.error("It seems that the transaction is added into list by error, skipping.", transactionDetails);
      }
      return null;
    })
    .then(transactionDetails => {
      if (transaction.blockNumber && transactionDetails.status && !transactionDetails.pending) {
        return window.localWeb3.eth.getBalance(account, transaction.blockNumber - 1)
          .then(balanceAtDate => {
            if (balanceAtDate) {
              transactionDetails.balanceAtDate = window.localWeb3.utils.fromWei(balanceAtDate, 'ether');
              transactionDetails.balanceAtDateFiat = etherToFiat(transactionDetails.balanceAtDate);
            }
            return transactionDetails;
          });
      }
      return transactionDetails;
    })
    .catch(error => {
      console.debug('Error retrieving transaction details', transaction, error);
    });
}

export function formatTransactionsWithDetails(networkId, account, contractDetails, transactionsDetails, transactions, timestamp, progressionCallback) {
  account = account.toLowerCase();

  const loadingPromises = [];

  if (transactions && transactions.length) {
    // Iterate over transactions from retrieved from block
    transactions.forEach(transaction => {
      const fromAddress = transaction.from && transaction.from.toLowerCase();
      const toAddress = transaction.to && transaction.to.toLowerCase();

      // Avoid reloading non pending transactions knowing that it's unchangeable
      if (transactionsDetails
          && transaction.hash
          && transactionsDetails[transaction.hash]
          && !transactionsDetails[transaction.hash].pending) {
        loadingPromises.push(Promise.resolve(transactionsDetails[transaction.hash]));
      }
      // Make sure to not display transaction that hasn't a 'to' or 'from' address
      else if (contractDetails.isContract || toAddress === account || fromAddress === account) {
        loadingPromises.push(window.localWeb3.eth.getTransactionReceipt(transaction.hash)
          .then(receipt => {
            return addTransaction(networkId, account, contractDetails, transactionsDetails, transaction, receipt, timestamp ? timestamp : transaction.timestamp);
          }));
      }
    });
  }

  // Continue searching in previous block
  return Promise.all(loadingPromises);
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

export function addPendingTransactionToStorage(networkId, account, contractDetails, transaction) {
  const contractAddressSuffix = contractDetails && contractDetails.isContract ? `-${contractDetails.address}` : '';
  const STORAGE_KEY = `exo-wallet-transactions-progress-${networkId}-${account}${contractAddressSuffix}`.toLowerCase();

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

  // Add token transactions as pending for ether transactions too
  if (contractDetails && contractDetails.isContract) {
    addPendingTransactionToStorage(networkId, account, null, transaction);
  }
}

export function removePendingTransactionFromStorage(networkId, account, contractDetails, transactionHash) {
  const contractAddressSuffix = contractDetails && contractDetails.isContract ? `-${contractDetails.address}` : '';
  const STORAGE_KEY = `exo-wallet-transactions-progress-${networkId}-${account}${contractAddressSuffix}`.toLowerCase();
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

  // Remove token transactions from pending ether transactions too
  if (contractDetails && contractDetails.isContract) {
    removePendingTransactionFromStorage(networkId, account, null, transactionHash);
  }
}

export function getPendingTransactionFromStorage(networkId, account, contractDetails) {
  const contractAddressSuffix = contractDetails && contractDetails.isContract ? `-${contractDetails.address}` : '';
  const STORAGE_KEY = `exo-wallet-transactions-progress-${networkId}-${account}${contractAddressSuffix}`.toLowerCase();
  let storageValue = localStorage.getItem(STORAGE_KEY);
  if (storageValue === null) {
    return {};
  } else {
    return JSON.parse(storageValue);
  }
}

function addBlockTransactions(networkId, account, contractDetails, transactions, block, untilBlockNumber, loadedBlocks, progressionCallback) {
  if (!block) {
    throw new Error("An error occurred while retrieving data from network, this may be fixed by refreshing your wallet");
  }

  loadedBlocks++;

  if (progressionCallback) {
    try {
      progressionCallback(loadedBlocks);
    } catch(e) {
      if (`${e}`.indexOf("stopLoading") < 0) {
        console.debug("error while calling progressionCallback", e);
      } else {
        // Stop loading command used
        return false;
      }
    }
  }

  // If we :
  //  * already searched inside 1000 block
  //  * or we reached the genesis block
  //  * or we already displayed 10 transactions
  // then stop searching
  if (block.number === 0 || block.number <= untilBlockNumber || !account) {
    return false;
  }

  return formatTransactionsWithDetails(networkId, account, contractDetails, transactions, block.transactions, block.timestamp * 1000, progressionCallback)
    .then(() => window.localWeb3.eth.getBlock(block.parentHash, true))
    .then(blockTmp => addBlockTransactions(networkId, account, contractDetails, transactions, blockTmp, untilBlockNumber, loadedBlocks, progressionCallback))
    .catch(error => {
      if (`${error}`.indexOf('stopLoading') < 0) {
        throw error;
      }
    });
}

function addEventsToTransactions(networkId, account, contractDetails, transactions, events, fieldFrom, fieldTo, progressionCallback) {
  if (events && events.length) {
    const promises = [];
    for (let i = 0; i < events.length; i++) {
      const event = events[i];

      if (event.returnValues && event.returnValues[fieldFrom] && event.returnValues[fieldTo]) {
        const from = event.returnValues[fieldFrom].toLowerCase();
        const to = event.returnValues[fieldTo].toLowerCase();

        if (to === account || from === account) {
          promises.push(window.localWeb3.eth.getTransaction(event.transactionHash));
        }
      }
    }

    return Promise.all(promises)
      .then(loadedTransactions => {
        const promises = [];
        if (loadedTransactions && loadedTransactions.length) {
          loadedTransactions.forEach(transaction => {
            promises.push(
              window.localWeb3.eth.getBlock(transaction.blockNumber)
                .then(block => {
                  transaction.timestamp = block.timestamp * 1000;
                  return transaction;
                })
            );
          });
        }
        return Promise.all(promises);
      })
      .then(loadedTransactions => {
        return formatTransactionsWithDetails(networkId, account, contractDetails, transactions, loadedTransactions, null, progressionCallback);
      })
  } else {
    return [];
  }
}