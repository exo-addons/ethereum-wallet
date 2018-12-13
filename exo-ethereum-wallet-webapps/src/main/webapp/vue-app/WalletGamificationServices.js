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

export function getSettings() {
  return fetch('/portal/rest/wallet/api/gamification/settings', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.json())
  .then(settings => window.walletGamificationSettings = settings);
}

export function getTeams() {
  return fetch('/portal/rest/wallet/api/gamification/teams', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.json());
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
      return resp.json();
    } else {
      throw new Error("Error saving parameter");
    }
  });
}

export function saveTeam(team) {
  return fetch('/portal/rest/wallet/api/gamification/saveTeam', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(team)
  })
  .then(resp => resp && resp.ok && resp.json());
}

export function removeTeam(id) {
  return fetch(`/portal/rest/wallet/api/gamification/removeTeam?id=${id}`, {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok);
}
