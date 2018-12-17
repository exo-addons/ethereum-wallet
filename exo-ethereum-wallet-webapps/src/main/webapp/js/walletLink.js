$( document ).ready(function() {
  return fetch(`/portal/rest/wallet/api/global-settings`, {credentials: 'include'})
    .then(resp =>  {
      if (resp && resp.ok) {
        return resp.json();
      } else {
        return null;
      }
    })
    .then(settings => {
      if(settings.isWalletEnabled) {
        eXo.social.tiptip = eXo.social.tiptip ? eXo.social.tiptip : {};
        eXo.social.tiptip.extraActions = eXo.social.tiptip.extraActions ? eXo.social.tiptip.extraActions : [];
        eXo.social.tiptip.extraActions.push({
          appendContentTo(divUIAction, ownerId, type) {
            if(!type || type === 'username') {
              divUIAction.append(`<a title="Send Funds" class="btn sendFundsTipTipButton" href="/portal/intranet/wallet?receiver=${ownerId}&receiver_type=user&principal=true">
                  <i aria-hidden="true" class="uiIconSendFunds material-icons">send</i>
              </a>`);
            }
          }
        });
      }
    })
    .catch(e => {
      console.debug("Error while initializing tiptip of Wallet");
    });
});
