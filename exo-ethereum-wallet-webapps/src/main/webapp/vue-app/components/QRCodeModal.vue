<template>
  <v-dialog v-model="show" width="300px" max-width="100vw">
    <v-card class="elevation-12">
      <v-toolbar dark color="primary">
        <v-toolbar-title>{{ title }}</v-toolbar-title>
        <v-spacer />
        <v-btn icon dark @click.native="show = false">
          <v-icon>close</v-icon>
        </v-btn>
      </v-toolbar>
      <v-card-text>
        <div id="addressQRCode" class="text-xs-center"></div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    title: {
      type: String,
      default: function() {
        return null;
      }
    },
    open: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    amount: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    from: {
      type: String,
      default: function() {
        return null;
      }
    },
    to: {
      type: String,
      default: function() {
        return null;
      }
    },

    isContract: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    functionName: {
      type: String,
      default: function() {
        return null;
      }
    },
    functionPayable: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    argsNames: {
      type: Array,
      default: function() {
        return [];
      }
    },
    argsTypes: {
      type: Array,
      default: function() {
        return [];
      }
    },
    argsValues: {
      type: Array,
      default: function() {
        return [];
      }
    }
  },
  data () {
    return {
      show: false,
      netId: null
    };
  },
  watch: {
    open() {
      if (this.open) {
        this.show = true;
        this.computeCanvas();
      }
    },
    show() {
      if(!this.show) {
        this.$emit("close");
      }
    }
  },
  methods: {
    computeCanvas() {
      window.localWeb3.eth.net.getId()
        .then(netId => {
          // This promise is triggered multiple times
          if (this.to && netId !== this.netId) {
            this.netId = netId;
            const qr = new window.EthereumQRPlugin();
            const options = {
              chainId: netId,
              to: this.to
            };
  
            if (this.from) {
              options.from = this.from;
            }
  
            if (this.amount && !this.isContract) {
              options.value = window.localWeb3.utils.toWei(this.amount.toString(), 'ether');
            }
  
            if (window.walletSettings.userDefaultGas) {
              options.gas = window.walletSettings.userDefaultGas;
            }
  
            if (this.isContract) {
              options.mode = "contract_function";
              options.functionSignature = {};
              options.functionSignature.name = this.functionName;
              options.functionSignature.payable = this.functionPayable;
              if (this.argsNames.length === this.argsTypes.length && this.argsTypes.length === this.argsValues.length) {
                options.functionSignature.args = [];
                options.argsDefaults = [];
  
                
                for (let i = 0; i < this.argsNames.length; i++) {
                  const argsName = this.argsNames[i];
                  const argsType = this.argsTypes[i];
                  const argsValue = this.argsValues[i];

                  console.log(argsName);
                  console.log(argsType);
                  console.log(argsValue);

                  options.functionSignature.args.push({
                    "name": argsName,
                    "type": argsType
                  });
                  options.argsDefaults.push({
                    "name": argsName,
                    "value": argsValue
                  });
                }
              }
            }
  
            const qrCode = qr.toCanvas(options, {
              selector: '#addressQRCode'
            });
          }
        });
    }
  }
};
</script>

