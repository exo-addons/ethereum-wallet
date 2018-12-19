export function savePeriodRewardTransactions(transactions) {
  return fetch('/portal/rest/wallet/api/reward/savePeriodRewardTransactions', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(transactions),
  }).then((resp) => {
    if (resp && resp.ok) {
      return;
    } else {
      throw new Error('Error saving reward transactions');
    }
  });
}

export function savePeriodRewardTransaction(transaction) {
  return fetch('/portal/rest/wallet/api/reward/savePeriodRewardTransaction', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(transaction),
  }).then((resp) => {
    if (resp && resp.ok) {
      return;
    } else {
      throw new Error('Error saving reward transaction');
    }
  });
}

export function getPeriodRewardTransactions(networkId, periodType, startDateInSeconds, rewardType) {
  return fetch(`/portal/rest/wallet/api/reward/getPeriodRewardTransactions?networkId=${networkId}&periodType=${periodType}&startDateInSeconds=${startDateInSeconds}&rewardType=${rewardType}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error('Error saving reward transactions');
    }
  });
}

export function getPeriodRewardDates(date, periodType) {
  // convert from milliseconds to seconds
  date = parseInt(date.getTime() / 1000);
  return fetch(`/portal/rest/wallet/api/reward/getPeriodRewardDates?dateInSeconds=${date}&periodType=${periodType}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => resp && resp.ok && resp.json());
}
