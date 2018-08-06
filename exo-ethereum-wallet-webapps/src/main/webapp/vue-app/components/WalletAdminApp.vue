<template>
  <v-app id="WalletAdminApp">
    <main>
      <v-layout row>
        <v-flex xs12 sm6 offset-sm3>
          <v-toolbar dark color="primary">
            <v-toolbar-title>Wallet application administration</v-toolbar-title>
          </v-toolbar>
          <deploy-new-contract />
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>

<script>
import DeployNewContract from './DeployNewContract.vue';

export default {
  components: {
    DeployNewContract
  }
};
</script>