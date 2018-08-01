/*
 * Return an Array of Objects of type:
 * {
 *  name: Full name,
 *  id: id,
 *  avatar: Avatar URL/URI
 * }
 */
export function searchContact(filter) {
  let items = null;
  return searchUsers(filter)
    .then(users => items = users && users.length ? users : [])
    .then(() => searchSpaces(filter))
    .then(spaces => items.concat(spaces))
    .catch(() => null);
}

/*
 * Return a JSON of format:
 * {
 *  name: Full Name,
 *  id: name,
 *  address: public address of user or space,
 *  avatar: Avatar of the user or space
 * }
 */
export function searchAddress(id, type) {
  const address = sessionStorage.getItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
  if (address) {
    return Promise.resolve(address);
  }
  return fetch(`/portal/rest/wallet/api/account?name=${id}&type=${type}`.toLowerCase())
    .then(resp =>  {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(data => {
      if(data && data.address && data.address.length && data.address.indexOf('0x') === 0) {
        if (sessionStorage) {
          sessionStorage.setItem(`exo-wallet-address-${type}-${name}`.toLowerCase(), data.address);
        }
        return data.address;
      } else {
        sessionStorage.removeItem(`exo-wallet-address-${type}-${name}`.toLowerCase());
        return null;
      }
     })
     .catch(() => null);
}

/*
 * 
 */
export function searchFullName(address) {
  return fetch(`/portal/rest/wallet/api/account?address=${address}`.toLowerCase())
    .then(resp =>  {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        sessionStorage.setItem(`exo-wallet-address-${item.type}-${address}`.toLowerCase(), JSON.stringify(item));
        return item;
      }
    });
}

export function getContactFromStorage(id, ...types) {
  if (types && types.length) {
    types.forEach(type => {
      const value = sessionStorage.getItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
      if (value && value.length) {
        return JSON.parse(value);
      }
    });
  }
  return {};
}

export function getContractFromStorage(account, address) {
  let contractDetails = localStorage.getItem(`exo-wallet-contract-${account}-${address}`.toLowerCase().toLowerCase());
  if (contractDetails) {
    contractDetails = JSON.parse(contractDetails);
    return Promise.resolve(contractDetails);
  }
  return Promise.resolve(null);
}

function searchUsers(filter) {
  if (isOnlySpaceMembers()) {
    const params = $.param({nameToSearch: filter, typeOfRelation: 'member_of_space', spaceURL: getSpaceURL()});
    return fetch(`/portal/rest/social/people/suggest.json?${params}`)
      .then(resp =>  {
        if (resp.ok) {
          return resp.json();
        } else {
          return null;
        }
      })
      .then(items => {
        items.forEach((item) => {
          if (item.id && item.id.indexOf('@') === 0) {
            item.id = item.id.substring(1);
            item.id_type = `user_${item.id}`;
          }
        });
        return items;
      });
  } else {
    const params = $.param({nameToSearch: filter, typeOfRelation: 'mention_activity_stream'});
    return fetch(`/portal/rest/social/people/suggest.json?${params}`)
      .then(resp =>  {
        if (resp.ok) {
          return resp.json();
        } else {
          return null;
        }
      })
      .then(items => {
        items.forEach((item) => {
          if (item.id && item.id.indexOf('@') === 0) {
            item.id = item.id.substring(1);
            item.id_type = `user_${item.id}`;
          }
        });
        return items;
      });
  }
}

function searchSpaces(filter) {
  const params = $.param({fields: ["id","prettyName","displayName","avatarUrl"], keyword: filter});
  return fetch(`/portal/rest/space/user/searchSpace?${params}`)
  .then(resp =>  {
    if (resp.ok) {
      return resp.json();
    } else {
      return null;
    }
  })
  .then(items => {
    const result = [];
    items.forEach((item) => {
      result.push({
        avatar: item.avatarUrl ? item.avatarUrl : `/portal/rest/v1/social/spaces/${item.prettyName}/avatar`,
        name: item.displayName,
        id: item.prettyName,
        id_type: `space_${item.prettyName}`
      });
    });
    return result;
  });
}

function isOnlySpaceMembers() {
  return window.walletSettings.spaceURL && window.walletSettings.spaceURL.length;
}

function getSpaceURL() {
  return window.walletSettings.spaceURL;
}