export function getKudosBudget() {
  return fetch('/portal/rest/wallet/api/kudos/getKudosBudget', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.text())
  .then(value => Number(value));
}

export function getKudosContract() {
  return fetch('/portal/rest/wallet/api/kudos/getKudosContract', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.text());
}

export function saveKudosTotalBudget(budget) {
  return fetch('/portal/rest/wallet/api/kudos/saveKudosTotalBudget', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: $.param({budget: Number(budget)})
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving parameter");
    }
  });
}

export function saveKudosContract(kudosContractAddress) {
  return fetch('/portal/rest/wallet/api/kudos/saveKudosContract', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: $.param({kudosContract: kudosContractAddress})
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving parameter");
    }
  });
}

export function savePeriodKudosTransactions(transactions) {
  return fetch('/portal/rest/wallet/api/kudos/savePeriodKudosTransactions', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(transactions)
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving kudos transactions");
    }
  });
}

export function savePeriodKudosTransaction(transaction) {
  return fetch('/portal/rest/wallet/api/kudos/savePeriodKudosTransaction', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(transaction)
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving kudos transaction");
    }
  });
}

export function getPeriodTransactions(networkId, periodType, startDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/kudos/getPeriodTransactions?networkId=${networkId}&periodType=${periodType}&startDateInSeconds=${startDateInSeconds}`, {
    method: 'GET',
    credentials: 'include'
  }).then(resp => {
    if(resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error("Error saving kudos transactions");
    }
  });
}
