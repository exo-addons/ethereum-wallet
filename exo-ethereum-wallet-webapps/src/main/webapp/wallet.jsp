<!--
  Lack in eXo API, we import CSS here: https://jira.exoplatform.org/browse/PLF-8100
-->
<link href='/exo-ethereum-wallet/css/v0.01/wallet.css' rel="stylesheet">
<script type="text/javascript">
  window.walletAppMaximize = '<%=request.getAttribute("walletAppMaximize") == null || ((String[])request.getAttribute("walletAppMaximize")).length == 0 ? "false" : ((String[])request.getAttribute("walletAppMaximize"))[0]%>';
</script>

<div id="WalletApp"></div>