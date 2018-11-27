export function getTokensPerKudos() {
  return fetch('/portal/rest/wallet/api/ext/getTokensPerKudos', {
    method: 'GET',
    credentials: 'include'
  })
  .then(resp => resp && resp.ok && resp.text())
  .then(value => Number(value));
}

export function saveTokensPerKudos(tokensPerKudos) {
  return fetch('/portal/rest/wallet/api/ext/saveTokensPerKudos', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: $.param({tokensPerKudos: Number(tokensPerKudos)})
  }).then(resp => {
    if(resp && resp.ok) {
      return;
    } else {
      throw new Error("Error saving parameter");
    }
  });
}
