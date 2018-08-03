export function etherToUSD(amount) {
  if (window.walletSettings.usdPrice && amount)  {
    return (window.walletSettings.usdPrice * amount).toFixed(2);
  }
  return 0;
}

export function gasToUSD(amount) {
  if (window.walletSettings.usdPrice && window.walletSettings.gasPriceInEther && amount)  {
    return (window.walletSettings.gasPriceInEther * window.walletSettings.usdPrice * amount).toFixed(2);
  }
  return 0;
}

export function retrieveUSDExchangeRate() {
  // Retrieve USD <=> Ether exchange rate
  return Promise.resolve(sessionStorage.getItem('exo-wallet-exchange-rate'))
    .then(exchangeRate => {
      return exchangeRate ? exchangeRate : fetch('https://api.coinmarketcap.com/v1/ticker/ethereum/', {
        referrerPolicy: "no-referrer",
        headers: {
          'Origin': ''
        }
      });
    })
    .then(resp => {
      if (resp && resp.ok) {
        return resp.json();
      } else if(typeof resp === 'string' && resp.length) {
        return JSON.parse(resp);
      } else {
        return null;
      }
    })
    .then(content => {
      if (content && content.length && content[0].price_usd) {
        sessionStorage.setItem('exo-wallet-exchange-rate', JSON.stringify(content));
  
        window.walletSettings.usdPrice = parseFloat(content[0].price_usd);
        window.walletSettings.usdPriceLastUpdated = new Date(parseInt(content[0].last_updated) * 1000);
        return true;
      }
    })
    .then(usdPriceRetrieved => {return usdPriceRetrieved ? window.localWeb3.eth.getGasPrice(): null;})
    .then(gasPrice => {window.walletSettings.gasPriceInEther = gasPrice ? window.localWeb3.utils.fromWei(gasPrice, 'ether'): 0;});
}