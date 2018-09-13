<template>
  <v-dialog v-model="dialog" content-class="uiPopup" width="450px" max-width="100vw" persistent @keydown.esc="dialog = false">
    <button slot="activator" class="btn btn-primary ml-1">Receive</button>
    <v-card class="elevation-12">
      <div class="popupHeader ClearFix">
        <a class="uiIconClose pull-right" aria-hidden="true" @click="dialog = false"></a>
        <span class="PopupTitle popupTitle">Receive funds</span>
      </div>
      <v-card-text class="text-xs-center">
        <qr-code
          ref="qrCode"
          :to="walletAddress"
          title="Address QR Code"
          information="You can send this Wallet address or QR code to other users to send you ether or tokens" />
        <wallet-address :value="walletAddress" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <button class="btn" @click="dialog = false">Close</button>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import QrCode from './QRCode.vue';

export default {
  components: {
    WalletAddress,
    QrCode
  },
  props: {
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data() {
    return {
      dialog: false
    };
  },
  watch: {
    dialog() {
      if (this.dialog) {
        this.$refs.qrCode.computeCanvas();
      }
    }
  }
};
</script>