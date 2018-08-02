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
 *  "name": display name of space of user,
 *  "id": Id of space of user,
 *  "address": Ethereum account address,
 *  "avatar": avatar URL/URI,
 *  "type": 'user' or 'space'
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
          sessionStorage.setItem(`exo-wallet-address-${type}-${id}`.toLowerCase(), data.address);
        }
        return data.address;
      } else {
        sessionStorage.removeItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
        return null;
      }
    })
    .catch(() => null);
}

/*
 * Searches Full name (Space or user) by usin the provided address
 * Return {
 *  "name": display name of space of user,
 *  "id": Id of space of user,
 *  "address": Ethereum account address,
 *  "avatar": avatar URL/URI,
 *  "type": 'user' or 'space'
 * }
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

/*
 * Retrieves User or Space details from sessionStorage
 * 
 * Return {
 *  "name": display name of space of user,
 *  "id": Id of space of user,
 *  "address": Ethereum account address,
 *  "avatar": avatar URL/URI,
 *  "type": 'user' or 'space'
 * }
 */
export function getContactFromStorage(id, ...types) {
  if (types && types.length) {
    let i = 0;
    do {
      const type = types[i];
      const value = sessionStorage.getItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
      if (value && value.length) {
        return JSON.parse(value);
      }
      i++;
    } while(i < types.length);
  }
  return {};
}

/*
 * Retrieves Contract details from localStorage
 * 
 * Return {
 *  symbol: Contract symbol,
 *  name: Contract name,
 *  address: Ethereum contract address
 * }
 */
export function getContractFromStorage(account, address) {
  let contractDetails = localStorage.getItem(`exo-wallet-contract-${account}-${address}`.toLowerCase().toLowerCase());
  if (contractDetails) {
    contractDetails = JSON.parse(contractDetails);
    return Promise.resolve(contractDetails);
  }
  return Promise.resolve(null);
}

/*
 * Search users from eXo Platform, used for suggester
 */
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

/*
 * Search spaces from eXo Platform, used for suggester
 */
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

/*
 * Determins whether the suggested users should belong to a specific space or not
 */
function isOnlySpaceMembers() {
  return window.walletSettings.spaceURL && window.walletSettings.spaceURL.length;
}

/*
 * Determins the specific space from where the users could be suggested
 */
function getSpaceURL() {
  return window.walletSettings.spaceURL;
}