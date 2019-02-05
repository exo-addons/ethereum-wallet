export function getRewardSettings() {
  return fetch('/portal/rest/wallet/api/reward/settings', {
    method: 'GET',
    credentials: 'include',
  })
    .then((resp) => resp && resp.ok && resp.json())
    .then((settings) => (window.walletRewardSettings = settings));
}

export function saveRewardSettings(settings) {
  return fetch('/portal/rest/wallet/api/reward/settings/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(settings),
  }).then((resp) => {
    return resp && resp.ok;
  });
}

export function getRewardTeams() {
  return fetch('/portal/rest/wallet/api/reward/team/list', {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => resp && resp.ok && resp.json());
}

export function saveRewardTeam(team) {
  return fetch('/portal/rest/wallet/api/reward/team/save', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(team),
  }).then((resp) => resp && resp.ok && resp.json());
}

export function removeRewardTeam(id) {
  return fetch(`/portal/rest/wallet/api/reward/team/remove?id=${id}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => resp && resp.ok);
}

export function getRewardTransactions(networkId, periodType, startDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/reward/transaction/list?networkId=${networkId}&periodType=${periodType}&startDateInSeconds=${startDateInSeconds}`, {
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

export function getRewardDates(date, periodType) {
  // convert from milliseconds to seconds
  date = parseInt(date.getTime() / 1000);
  return fetch(`/portal/rest/wallet/api/reward/transaction/getDates?dateInSeconds=${date}&periodType=${periodType}`, {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  }).then((resp) => resp && resp.ok && resp.json());
}

export function saveRewardTransaction(transaction) {
  return fetch('/portal/rest/wallet/api/reward/transaction/saveRewardTransaction', {
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

export function saveRewardTransactions(transactions) {
  return fetch('/portal/rest/wallet/api/reward/transaction/saveRewardTransactions', {
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

export function computeRewards(identityIds, periodDateInSeconds) {
  return fetch(`/portal/rest/wallet/api/reward/compute?periodDateInSeconds=${periodDateInSeconds}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(identityIds),
  }).then((resp) => {
    if (resp && resp.ok) {
      return  resp.json();
    } else {
      throw new Error('Error computing rewards');
    }
  });
}
