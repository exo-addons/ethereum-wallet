<template>
  <v-container class="text-xs-center" width="100%" max-width="100%">
    <v-btn v-show="!creating && !created" color="primary" @click="createAccount">Create new account</v-btn>
    <v-progress-circular v-show="creating" indeterminate color="primary"></v-progress-circular>
    <v-alert :value="error" type="error" class="v-content">
      {{ error }}
    </v-alert>
    <v-alert :value="created" type="warning" class="v-content">
      ATTENTION! Please read carefully this message to avoid losing your earns !
    </v-alert>
    <v-alert :value="created" type="info" class="v-content">
      <p>Congratulations! You just created an account that allows you to earn tokens!</p>
      <p>Please copy the corresponding private key to a private and safe place:</p>
      <code>{{ privateKey }}</code>
      <p>Use this private key to import your account into Metamask, this will allows you to exchange/send some tokens and ether.</p>
      <p>Your newly created address is:</p>
      <code>{{ address }}</code>
    </v-alert>
    <v-btn v-show="created" @click="$emit('created', address)">Just copied!</v-btn>
  </v-container>
</template>

<script>
export default {
  data () {
    return {
      creating: false,
      created: false,
      error: null,
      privateKey: null,
      address: null
    };
  },
  methods: {
    createAccount() {
      const password = Math.random().toString();

      const newAccount = window.localWeb3.eth.accounts.create(password);
      this.privateKey = newAccount.privateKey;
      this.address = newAccount.address;

      this.error = null;
      this.creating = true;

      console.log(JSON.stringify(newAccount));

      window.localWeb3.eth.personal.importRawKey(newAccount.privateKey, password)
        .then(address => window.localWeb3.eth.personal.unlockAccount(this.address, password, 9999))
        .then(address => {
          console.log(address);
          this.creating = false;
          this.created = true;
        })
        .catch(e => {
          this.creating = false;
          this.error = `Error encountered: ${e}`;
        });
    }
  }
};
</script>

