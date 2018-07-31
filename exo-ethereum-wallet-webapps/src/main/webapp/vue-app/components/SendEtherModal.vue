<template>
  <v-dialog v-model="dialog" width="300px" max-width="100vh">
    <v-btn slot="activator" color="primary" dark ripple>Send Ether</v-btn>
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>Send Ether</v-toolbar-title>
      </v-toolbar>
      <v-card-text>
        <v-alert :value="error" type="error" class="v-content">
          {{ error }}
        </v-alert>
        <v-form>
          <v-text-field v-model="recipient" name="recipient" label="Recipient" type="text"></v-text-field>
          <v-text-field v-model.number="amount" name="amount" label="Amount"></v-text-field>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="primary" @click="sendEther">Send</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    account: {
      type: String,
      default: function() {
        return {};
      }
    }
  },
  data () {
    return {
      recipient: null,
      amount: null,
      dialog: null,
      error: null
    };
  },
  methods: {
    sendEther() {
      this.error = null;
      if (!window.localWeb3.utils.isAddress(this.recipient)) {
        this.error = "Invalid recipient address";
        return;
      }

      if (!this.amount || isNaN(parseInt(this.amount)) || !isFinite(this.amount) || this.amount <= 0) {
        this.error = "Invalid amount";
        return;
      }

      window.localWeb3.eth.sendTransaction({
        from: this.account.toLowerCase(),
        to: this.recipient,
        value: window.localWeb3.utils.toWei(this.amount.toString(), "ether")
      })
      .on('transactionHash', hash => {
        this.recipient = null;
        this.amount = null;
        this.dialog = false;
        this.$emit("loading");
      })
      .on('confirmation', (confirmationNumber, receipt) => {
        this.$emit("loaded");
      })
      .on('error', (error, receipt) => {
        this.error = `Error sending ether: ${error}`;
        console.error("Error sending ether", error, receipt);
        this.$emit("end-loading");
      });
    }
  }
};
</script>

