<template>
  <v-flex width="100%">
    <v-alert :value="displayNotSameNetworkWarning" type="warning">
      Please switch Metamask to <strong>{{ networkLabel }}</strong>
    </v-alert>
    
    <v-alert v-if="displaySpaceMetamaskEnableHelp" :value="displaySpaceMetamaskEnableHelp" type="info">
      Please enable/install Metamask to be able to add a new space account
    </v-alert>
    <!-- Ethereum address association -->
    <v-alert v-else-if="newAddressDetected" :value="newAddressDetected" type="info">
      A new wallet has been detected!
      <v-btn color="primary" dark flat @click.stop="addressAssociationDialog = true">
        See details
      </v-btn>
      <v-dialog v-model="addressAssociationDialog" width="400" max-width="100wv" @keydown.esc="addressAssociationDialog = false">
        <v-card>
          <v-toolbar dark color="primary">
            <v-btn icon dark @click.native="addressAssociationDialog = false">
              <v-icon>close</v-icon>
            </v-btn>
            <v-toolbar-title>Configure my wallet address</v-toolbar-title>
            <v-spacer></v-spacer>
          </v-toolbar>
          <v-card-text>
            <div v-if="displaySpaceAccountCreationHelp">
              Current space doesn't have a wallet yet ? If you are manager of the space, you can create a new account using Metamask.
            </div>
    
            <div v-if="currentAccountAlreadyInUse">
              Currently selected account in Metamask is already in use, you can't use it in this wallet.
            </div>
            <div v-else-if="displaySpaceAccountAssociationHelp">
              Would you like to use the current address <code>{{ newAccountAddress }}</code> in Space Wallet ?
            </div>
            <div v-else-if="displayUserAccountAssociationHelp">
              Would you like to use the current address <code>{{ newAccountAddress }}</code> in your Wallet ?
            </div>
            <div v-else-if="displayUserAccountChangeHelp">
              <!-- A workaround for a UI glitch -->
              {{ currentAccountAlreadyInUse ? '' : '' }}

              Would you like to replace your wallet address <code>{{ oldAccountAddress }}</code> by the current address <code>{{ newAccountAddress }}</code> ?
            </div>
          </v-card-text>
    
          <v-card-actions v-if="displayAccountHelpActions" class="text-xs-center">
            <v-spacer></v-spacer>
            <v-btn color="primary" @click="$emit('save-address-to-account')">
              Yes
            </v-btn>
            <v-btn color="primary" @click="addressAssociationDialog = false">
              No
            </v-btn>
            <v-spacer></v-spacer>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-alert>
    
    <!-- error alerts -->
    <div v-if="displayErrors">
      <v-alert v-if="!metamaskEnabled" :value="!metamaskEnabled" type="info">
        Please install Metamask in your browser
        <v-btn color="primary" dark flat @click.stop="installInstructionDialog = true">
          See help
        </v-btn>
        <v-dialog v-model="installInstructionDialog" width="400" max-width="100wv" @keydown.esc="installInstructionDialog = false">
          <v-card>
            <v-toolbar dark color="primary">
              <v-btn icon dark @click.native="installInstructionDialog = false">
                <v-icon>close</v-icon>
              </v-btn>
              <v-toolbar-title>Enable wallet application</v-toolbar-title>
              <v-spacer></v-spacer>
            </v-toolbar>
            <v-card-text>
              To access your wallet you 'll need to:
              <ol type="1">
                <li>Install/enable <a target="about:blank" href="https://metamask.io/">Metamask</a> in your browser</li>
                <li>Follow setup instructions on Metamask browser plugin</li>
                <li>Connect to Metamask account</li>
                <li v-if="networkLabel && networkLabel.length">Switch Metamask network to <strong>{{ networkLabel }}</strong></li>
                <li>Associate the automatically generated account address from Metamask to your profile (a box will be displayed automaticatty once you enable Metamask on browser)</li>
              </ol>
            </v-card-text>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn color="primary" @click="installInstructionDialog = false">
                Close
              </v-btn>
              <v-spacer></v-spacer>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-alert>
      <v-alert v-else-if="!metamaskConnected" :value="!metamaskConnected" type="warning">
        Please connect Metamask to {{ networkLabel && networkLabel.length ? networkLabel : 'an online network' }}
      </v-alert>
      <v-alert v-else-if="!account || !account.length" :value="!account" type="warning">
        Please select a valid account using Metamask
      </v-alert>
      <v-alert v-else :value="errorMessage" type="error">
        {{ errorMessage }}
      </v-alert>
    </div>
  </v-flex>
</template>

<script>

export default {
  props: {
    displayNotSameNetworkWarning: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    networkLabel: {
      type: String,
      default: function() {
        return null;
      }
    },
    displaySpaceMetamaskEnableHelp: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    newAddressDetected: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displaySpaceAccountCreationHelp: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    currentAccountAlreadyInUse: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displaySpaceAccountAssociationHelp: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displayUserAccountAssociationHelp: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displayUserAccountChangeHelp: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displayAccountHelpActions: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    displayErrors: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    metamaskEnabled: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    metamaskConnected: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    account: {
      type: String,
      default: function() {
        return null;
      }
    },
    newAccountAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    oldAccountAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    errorMessage: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data() {
    return {
      addressAssociationDialog: false,
      installInstructionDialog: false
    };
  }
};
</script>