import {searchFullName, getContractFromStorage} from './WalletAddressRegistry.js';
import {etherToFiat, watchTransactionStatus, convertTokenAmountReceived} from './WalletUtils.js';
import {getContractInstance} from './WalletToken.js';

export function loadPendingTransactions(networkId, account, contractDetails, transactions, refreshCallback, removeIfNotFound) {
  const pendingTransactions = getPendingTransactionFromStorage(networkId, account, contractDetails);
  if (!pendingTransactions || !Object.keys(pendingTransactions).length) {
    return Promise.resolve({});
  }

  const loadingPromises = [];

  Object.keys(pendingTransactions).forEach(transactionHash => {
    let transaction;
    loadingPromises.push(window.localWeb3.eth.getTransaction(transactionHash)
      .then(transactionTmp => {
        transaction = transactionTmp;
        if (transactionTmp) {
          transaction.label = pendingTransactions[transactionHash].label;
          transaction.message = pendingTransactions[transactionHash].message;
          return window.localWeb3.eth.getTransactionReceipt(transactionHash)
        } else {
          if (removeIfNotFound) {
            removePendingTransactionFromStorage(networkId, account, contractDetails, transactionHash);
          }
          throw new Error("Invalid transaction hash, it will be removed", transactionHash);
        }
      })
      .then(receipt => {
        if (receipt) {
          transaction.loadedFromPending = true;
          return window.localWeb3.eth.getBlock(transaction.blockNumber, false)
            .then(block => addTransaction(networkId, account, contractDetails, transactions, transaction, receipt, block && block.timestamp * 1000))
            .then(transactionDetails => {
              refreshCallback(transactionDetails);
              return transactionDetails;
            });
        } else {
          return addTransaction(networkId, account, contractDetails, transactions, pendingTransactions[transactionHash], null, null, refreshCallback);
        }
      })
      .catch(error => {
        console.debug("Error while retrieving transaction details", transactionHash, error);
      })
    );
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
    .then(transactionDetails => {
      if (transactionDetails && transactionDetails.length) {
        const loadingPromises = [];

        transactionDetails.forEach(transactionDetail => {
          if (!transactionDetail || !transactionDetail.hash || !transactionDetail.hash.length) {
            console.debug("Can't parse transaction detail", transactionDetail);
          } else {
            let transaction,receipt;

            const transactionHash = transactionDetail.hash;
            const loadingPromise = window.localWeb3.eth.getTransaction(transactionHash)
              .then(transactionTmp => {
                transaction = transactionTmp;

                // if this is about loading a contract transactions, ignore other transactions
                if (!transaction || (contractDetails.isContract && (!transaction.to || transaction.to.toLowerCase() !== contractDetails.address))) {
                  transaction.ignore = true;
                } else {
                  transaction.label = transactionDetail.label;
                  transaction.message = transactionDetail.message;
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

                return addTransaction(networkId, account, contractDetails, transactions, transaction, receipt, block && block.timestamp * 1000);
              })
              .then(transactionDetails => {
                if (transaction.ignore) {
                  return;
                }

                if (transactionDetails && refreshCallback) {
                  return refreshCallback(transactionDetails);
                }

                return transactionDetails;
              })
              .catch(error => {
                throw error;
              });
            loadingPromises.push(loadingPromise);
          }
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

export function loadContractTransactions(networkId, account, contractDetails, transactions, progressionCallback) {
  account = account.toLowerCase();
  const isLoadingAll = contractDetails.address && contractDetails.address.toLowerCase() === account;
  // Load Transfer events for user as sender
  return contractDetails.contract.getPastEvents("Transfer", {
    fromBlock: 0,
    toBlock: 'latest',
    filter: {
      isError: 0,
      txreceipt_status: 1
    },
    topics: [
      window.localWeb3.utils.sha3("Transfer(address,address,uint256)"),
      isLoadingAll ? null : window.localWeb3.utils.padLeft(account, 64),
      null
    ]
  })
    .then(events => events && addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_from", "_to", progressionCallback))
    // Load Transfer events for user as receiver
    .then(() => !isLoadingAll && contractDetails.contract.getPastEvents("Transfer", {
        fromBlock: 0,
        toBlock: 'latest',
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
    .then(events => events && addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_from", "_to", progressionCallback))
    // Load Approval events for user as receiver
    .then(() => contractDetails.contract.getPastEvents("Approval", {
      fromBlock: 0,
      toBlock: 'latest',
      filter: {
        isError: 0,
        txreceipt_status: 1
      },
      topics: [
        window.localWeb3.utils.sha3("Approval(address,address,uint256)"),
        isLoadingAll ? null : window.localWeb3.utils.padLeft(account, 64),
        null
      ]
    }))
    .then(events => events && addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_owner", "_spender", progressionCallback))
    // Load Approval events for user as sender
    .then(() => !isLoadingAll && contractDetails.contract.getPastEvents("Approval", {
      fromBlock: 0,
      toBlock: 'latest',
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
    .then(events => events && addEventsToTransactions(networkId, account, contractDetails, transactions, events, "_owner", "_spender", progressionCallback))
    .then(() => transactions)
    .catch(e => {
      console.debug("Error occurred while retrieving contract transactions", e);
      throw e;
    });
}

export function addTransaction(networkId, account, accountDetails, transactions, transaction, receipt, timestamp, watchLoadSuccess, watchLoadError) {
  if(!transaction || !networkId || !account) {
    console.debug("Wrong paramters for addTransaction method", networkId, account, transaction);
    return;
  }

  const status = receipt ? receipt.status : true;
  const gasUsed = receipt && receipt.gasUsed ? receipt.gasUsed : transaction.gas;
  let transactionFeeInEth = 0;
  if (receipt) {
    // Calculate Transaction fees
    const transactionFeeInWei = gasUsed * transaction.gasPrice;
    transactionFeeInEth = window.localWeb3.utils.fromWei(transactionFeeInWei.toString(), 'ether');
  } else if(transaction.fee) {
    transactionFeeInEth = transaction.fee || 0;
  }

  const transactionFeeInFiat = transactionFeeInEth && etherToFiat(transactionFeeInEth);

  const fromAddress = transaction.from && transaction.from.toLowerCase();
  const toAddress = transaction.to && transaction.to.toLowerCase();
  const byAddress = transaction.by && transaction.by.toLowerCase();

  account = account.toLowerCase();

  const isReceiver = transaction.isSender ? false : toAddress === account;

  // Calculate sent/received amount
  const amount = transaction.value ? parseFloat(window.localWeb3.utils.fromWei(String(transaction.value), 'ether')) : 0;
  const amountFiat = amount ? etherToFiat(amount) : 0;
  const isFeeTransaction = amount === 0;

  // Retrieve user or space display name, avatar and id from sessionStorage
  const transactionDetails = {
    hash: transaction.hash,
    status: status,
    label: transaction.label,
    message: transaction.message,
    type: isFeeTransaction ? 'contract' : 'ether',
    isReceiver: isReceiver,
    isContractCreation: !toAddress && receipt && receipt.contractAddress,
    contractDetails: null,
    contractAddress: transaction.contractAddress,
    contractName: null,
    contractSymbol: transaction.contractSymbol,
    contractAmountLabel: transaction.contractAmountLabel,
    contractMethodName: transaction.contractMethodName,
    contractAmount: transaction.contractAmount,
    byAddress: byAddress,
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
    feeToken: transaction.feeToken,
    date: timestamp ? new Date(timestamp) : transaction.timestamp,
    pending: transaction.pending,
    adminIcon: transaction.adminIcon
  };

  if (transaction.pending) {
    addPendingTransactionToStorage(networkId, account, accountDetails, transaction);

    let loadedBlock = null;
    let loadedReceipt = null;
    watchTransactionStatus(transaction.hash, (receipt, block) => {
      window.localWeb3.eth.getTransaction(transaction.hash)
        .then(tx => {
          tx.label = transaction.label;
          tx.message = transaction.message;
          transaction = tx;
          transaction.loadedFromPending = true;

          return addTransaction(networkId, account, accountDetails, transactions, transaction, receipt, block && block.timestamp * 1000, watchLoadSuccess, watchLoadError);
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
  } else if(!transaction.loadedFromPending) {
    removePendingTransactionFromStorage(networkId, account, accountDetails, transaction.hash);
  }

  // Test if address corresponds to a contract
  if (!transactionDetails.contractAddress) {
    transactionDetails.contractAddress = transactionDetails.isContractCreation ? receipt.contractAddress : toAddress;
  }

  if (!transactionDetails.contractAddress) {
    transactionDetails.type = 'ether';
  }

  let isLoadingAll = false;
  return getContractFromStorage(transactionDetails.contractAddress)
    .then(contractDetails => {
      if (contractDetails) {
        isLoadingAll = contractDetails.address && contractDetails.address.toLowerCase() === account;
        transactionDetails.type = 'contract';
        transactionDetails.contractName = contractDetails.name;
        transactionDetails.contractAddress = contractDetails.address;
        if (!transactionDetails.contractAmountLabel) {
          transactionDetails.contractAmountLabel = 'Amount';
        }
        if (!transactionDetails.contractSymbol) {
          transactionDetails.contractSymbol = contractDetails.symbol;
        }
        if (transactionDetails.contractMethodName === 'addAdmin') {
          transactionDetails.contractSymbol = 'Level';
          transactionDetails.contractAmountLabel = 'Level';
        }
        if (transactionDetails.contractMethodName === 'setSellPrice') {
          transactionDetails.contractSymbol = 'eth';
        }

        transactionDetails.contractDecimals = contractDetails.decimals || 0;
        try {
          if (transactionDetails.isContractCreation) {
            return false;
          } else if (receipt && receipt.logs) {
            if (!abiDecoder.getABIs() || !abiDecoder.getABIs().length) {
              abiDecoder.addABI(window.walletSettings.contractAbi);
            }
            const method = abiDecoder.decodeMethod(transaction.input);
            const decodedLogs = abiDecoder.decodeLogs(receipt.logs);
            transactionDetails.contractMethodName = method && method.name;
            if (transactionDetails.contractMethodName !== 'transfer'
                && transactionDetails.contractMethodName !== 'transferFrom'
                && transactionDetails.contractMethodName !== 'approve') {
              transactionDetails.adminIcon = true;
              transactionDetails.isReceiver = false;
            }
            if (transactionDetails.contractMethodName) {
              if (method.name === 'transfer' || method.name === 'approve') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && (decodedLog.name == 'Transfer' || decodedLog.name == 'Approval'));
                if (methodLog) {
                  transactionDetails.fromAddress = methodLog.events[0].value.toLowerCase();
                  transactionDetails.toAddress = methodLog.events[1].value.toLowerCase();
                  transactionDetails.contractAmount = convertTokenAmountReceived(methodLog.events[2].value, transactionDetails.contractDecimals);
                  transactionDetails.isReceiver = transactionDetails.toAddress === account;
                }
              } else if (method.name === 'transferFrom') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'Transfer');
                if (methodLog) {
                  transactionDetails.fromAddress = methodLog.events[0].value.toLowerCase();
                  transactionDetails.toAddress = methodLog.events[1].value.toLowerCase();
                  transactionDetails.byAddress = transaction.from.toLowerCase();
                  transactionDetails.contractAmount = convertTokenAmountReceived(methodLog.events[2].value, transactionDetails.contractDecimals);
                  transactionDetails.isReceiver = false;
                }
              } else if (method.name === 'approveAccount') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'ApprovedAccount');
                if (methodLog) {
                  transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
                } else {
                  transactionDetails.toDisplayName = 'a previously approved';
                }
              } else if (method.name === 'disapproveAccount') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'DisapprovedAccount');
                if (methodLog) {
                  transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
                } else {
                  transactionDetails.toDisplayName = 'a previously disapproved';
                }
              } else if (method.name === 'addAdmin') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'AddedAdmin');
                if (methodLog) {
                  transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
                  transactionDetails.contractAmount = methodLog.events[1].value;
                  transactionDetails.contractSymbol = 'Level';
                  transactionDetails.contractAmountLabel = 'Level';
                }
              } else if (method.name === 'removeAdmin') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'RemovedAdmin');
                if (methodLog) {
                  transactionDetails.toAddress = methodLog.events[0].value.toLowerCase();
                } else {
                  transactionDetails.toDisplayName = 'a not admin account';
                }
              } else if (method.name === 'setSellPrice') {
                const methodLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'TokenPriceChanged');
                if (methodLog) {
                  transactionDetails.contractAmount = methodLog.events[0].value.toLowerCase();
                  if (transactionDetails.contractAmount) {
                    transactionDetails.contractAmount = window.localWeb3.utils.fromWei(String(transactionDetails.contractAmount), 'ether');
                  }
                  transactionDetails.contractSymbol = 'eth';
                }
              }
              const transactionFeeLog = decodedLogs && decodedLogs.find(decodedLog => decodedLog && decodedLog.name == 'TransactionFee');
              if (transactionFeeLog) {
                transactionDetails.feeToken = convertTokenAmountReceived(transactionFeeLog.events[1].value, transactionDetails.contractDecimals);
              }
            }
          }
        } catch(e) {
          console.debug('Error resolving Contract transaction status', e);
        }
      } else if(accountDetails && accountDetails.isContract) {
        console.debug("Can't find contract details from storage", transactionDetails.contractAddress);
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
          || isLoadingAll
          || transaction.from.toLowerCase() === account.toLowerCase()
          || (transaction.to && transaction.to.toLowerCase() === account.toLowerCase())
          || transactionDetails.fromAddress.toLowerCase() === account.toLowerCase()
          || transactionDetails.toAddress.toLowerCase() === account.toLowerCase()) {
        // If user/space details wasn't found on sessionStorage,
        // then display the transaction details and in // load name and avatar with a promise
        // From eXo Platform Server
        return transactions[transactionDetails.hash] = transactionDetails;
      } else {
        console.warn("It seems that the transaction is added into list by error, skipping.", account, transactionDetails);
      }
      return null;
    })
    .then(transactionDetails => {
      if (transaction.blockNumber && transactionDetails.status && !transactionDetails.pending) {
        return window.localWeb3.eth.getBalance(account, transaction.blockNumber - 1)
          .then(balanceAtDate => {
            if (balanceAtDate) {
              transactionDetails.balanceAtDate = window.localWeb3.utils.fromWei(String(balanceAtDate), 'ether');
              transactionDetails.balanceAtDateFiat = etherToFiat(transactionDetails.balanceAtDate);
            }
            return transactionDetails;
          });
      }
      return transactionDetails;
    })
    .catch(error => {
      console.debug('Error retrieving transaction details', transaction, error);
      return null;
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

  if (transaction.addLoadingToRecipient && transaction.to && transaction.to !== account) {
    addPendingTransactionToStorage(networkId, transaction.to, contractDetails, transaction);

    // Add to general token transactions list
    if (contractDetails && contractDetails.isContract && contractDetails.address != account) {
      addPendingTransactionToStorage(networkId, contractDetails.address, contractDetails, transaction);
    }
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

export function saveTransactionMessage(transactionHash, transactionMessage, transactionLabel, address) {
  if (transactionHash && (transactionMessage || transactionLabel)) {
    transactionLabel = transactionLabel || '';
    transactionMessage = transactionMessage || '';

    fetch('/portal/rest/wallet/api/account/saveTransactionMessage', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        hash: transactionHash,
        label: transactionLabel,
        message: transactionMessage,
        sender: address
      })
    });
  }
}

function addEventsToTransactions(networkId, account, contractDetails, transactions, events, fieldFrom, fieldTo, progressionCallback) {
  const isLoadingAll = contractDetails.address && contractDetails.address.toLowerCase() === account;
  if (events && events.length) {
    const promises = [];
    for (let i = 0; i < events.length; i++) {
      const event = events[i];

      if (event.returnValues && event.returnValues[fieldFrom] && event.returnValues[fieldTo]) {
        const from = event.returnValues[fieldFrom].toLowerCase();
        const to = event.returnValues[fieldTo].toLowerCase();

        if (isLoadingAll || to === account || from === account) {
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
                  transaction.timestamp = block && block.timestamp * 1000;
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