<template id="WalletSpaceApp">
  <wallet-app :is-space="true" />
</template>

<script>
import WalletApp from './WalletApp.vue';

export default {
  components: {
    WalletApp,
  },
};
</script>
