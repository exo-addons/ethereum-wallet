export function getSettings() {
  return fetch('/portal/rest/wallet/api/gamification/settings', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.json())
  .then(settings => window.walletGamificationSettings = settings);
}

export function saveSettings(settings) {
  return fetch('/portal/rest/wallet/api/gamification/saveSettings', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(settings)
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving parameter");
    }
  });
}

export function savePeriodKudosTransactions(transactions) {
  return fetch('/portal/rest/wallet/api/gamification/savePeriodTransactions', {
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
  return fetch('/portal/rest/wallet/api/gamification/savePeriodTransaction', {
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
  return fetch(`/portal/rest/wallet/api/gamification/getPeriodTransactions?networkId=${networkId}&periodType=${periodType}&startDateInSeconds=${startDateInSeconds}`, {
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

export function getPeriodDates(date, periodType) {
  // convert from milliseconds to seconds
  date = parseInt(date.getTime() / 1000);
  return fetch(`/portal/rest/wallet/api/gamification/getPeriodDates?dateInSeconds=${date}&periodType=${periodType}`, {
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(resp => resp && resp.ok && resp.json());
}

export function getGamificationPoints(userId, startDate, endDate) {
  startDate = startDate.toISOString().substring(0, 10) + ' 00:00:00';
  endDate = endDate.toISOString().substring(0, 10) + ' 00:00:00';
  return fetch(`/portal/rest/gamification/api/v1/points/date?userId=${userId}&startDate=${startDate}&endDate=${endDate}`, {
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(resp => resp && resp.ok && resp.json());
}
