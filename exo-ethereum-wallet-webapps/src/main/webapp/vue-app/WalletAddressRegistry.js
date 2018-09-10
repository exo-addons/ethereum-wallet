/*
 * Return an Array of users and spaces that matches the filter (used in suggestion) :
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
    .then(spaces => items = items.concat(spaces))
    .catch((e) => {
      console.debug("searchContact method - error", e);
    });
}

/*
 * Return the address of a user or space
 */
export function saveNewAddress(id, type, address, isBrowserWallet) {
  address = address.toLowerCase();
  return fetch('/portal/rest/wallet/api/account/saveAddress', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      type: type,
      id: id,
      address: address
    })
  })
    .then(resp => {
      if (resp && resp.ok) {
        if (isBrowserWallet) {
          // Save the address as generated using a browser wallet
          localStorage.setItem(`exo-wallet-${type}-${id}`, address);
        }

        // Save user's or space's associated address in local storage
        sessionStorage.setItem(`exo-wallet-address-${type}-${id}`, address);
      }
      return resp;
    });
}

export function isBrowserWallet(id, type, address) {
  address = address.toLowerCase();
  return localStorage.getItem(`exo-wallet-${type}-${id}`) === address;
}

/*
 * Return the address of a user or space
 */
export function searchAddress(id, type) {
  const address = sessionStorage.getItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
  if (address) {
    return Promise.resolve(address);
  }
  return searchUserOrSpaceObject(id, type)
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
    });
}

/*
 * Return the user or space object
 * {
 *  "name": display name of space of user,
 *  "id": Id of space of user,
 *  "address": Ethereum account address,
 *  "avatar": avatar URL/URI,
 *  "type": 'user' or 'space',
 *  "creator": space creator username for space type
 * }
 */
export function searchUserOrSpaceObject(id, type) {
  const address = sessionStorage.getItem(`exo-wallet-address-${type}-${id}`.toLowerCase());
  if (address) {
    return Promise.resolve(address);
  }
  return fetch(`/portal/rest/wallet/api/account/detailsById?id=${id}&type=${type}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    });
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
  address = address.toLowerCase();
  return fetch(`/portal/rest/wallet/api/account/detailsByAddress?address=${address}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(item => {
      if (item && item.name && item.name.length) {
        if (!item.avatar) {
          item.avatar = item.type === 'user' ? `/rest/v1/social/users/${item.id}/avatar`: `/rest/v1/social/spaces/${item.id}/avatar`;
        }
        sessionStorage.setItem(`exo-wallet-address-${item.type}-${address}`.toLowerCase(), JSON.stringify(item));
        return item;
      }
    })
    .catch((e) => {
      console.debug("searchFullName method - error", e);
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
  let contractDetails = localStorage.getItem(`exo-wallet-contract-${account}-${address}`.toLowerCase());
  if (contractDetails) {
    contractDetails = JSON.parse(contractDetails);
    return Promise.resolve(contractDetails);
  }
  return Promise.resolve(null);
}

/*
 * Search users from eXo Platform, used for suggester
 */
export function searchUsers(filter) {
  const params = $.param({
    nameToSearch: filter,
    typeOfRelation: 'mention_activity_stream',
    currentUser: eXo.env.portal.userName,
    spaceURL: isOnlySpaceMembers() ? getAccessPermission() : null
  });
  return fetch(`/portal/rest/social/people/suggest.json?${params}`, {credentials: 'include'})
    .then(resp =>  {
      if (resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(items => {
      if (items) {
        if (items.options) {
          items = items.options;
        }
        items.forEach((item) => {
          if (item.id && item.id.indexOf('@') === 0) {
            item.id = item.id.substring(1);
            item.id_type = `user_${item.id}`;
            if (!item.avatar) {
              item.avatar = item.avatarUrl ? item.avatarUrl : `/rest/v1/social/users/${item.id}/avatar`;
            }
          }
        });
      } else {
        items = [];
      }
      return items;
    });
}

/*
 * Search spaces from eXo Platform, used for suggester
 */
export function searchSpaces(filter) {
  const params = $.param({fields: ["id","prettyName","displayName","avatarUrl"], keyword: filter});
  return fetch(`/portal/rest/space/user/searchSpace?${params}`, {credentials: 'include'})
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
  return window.walletSettings.accessPermission && window.walletSettings.accessPermission.length;
}

/*
 * Determins the specific space from where the users could be suggested
 */
function getAccessPermission() {
  return window.walletSettings.accessPermission;
}