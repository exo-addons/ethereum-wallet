it('test getaccounts', () => {
  return window.testWeb3.eth
    .getAccounts()
    .then((accounts) => {
      return window.testWeb3.eth.getBalance(accounts[accounts.length - 2]);
    })
    .then((balance) => {
      expect(balance.toString()).toBe('100000000000000000000');
    });
});
