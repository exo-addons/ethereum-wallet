<template>
  <v-flex width="100%">
    <div v-if="displayNotSameNetworkWarning" class="alert alert-warning">
      <i class="uiIconWarning"></i>
      Please switch Metamask to <strong>{{ networkLabel }}</strong>
    </div>

    <div v-if="displaySpaceMetamaskEnableHelp" class="alert alert-info">
      <i class="uiIconInfo"></i>
      Please enable/install Metamask to be able to add a new space account
    </div>

    <!-- Ethereum address association -->
    <div v-else-if="newAddressDetected" class="alert alert-info">
      <i class="uiIconInfo"></i>
      <span>A new wallet has been detected!</span>
      <br />
      <button class="btn" @click.stop="addressAssociationDialog = true">
        See details
      </button>
      <v-dialog v-model="addressAssociationDialog" content-class="uiPopup" width="400" max-width="100wv" @keydown.esc="addressAssociationDialog = false">
        <v-card>
          <div class="popupHeader ClearFix">
            <a class="uiIconClose pull-right" aria-hidden="true" @click="addressAssociationDialog = false"></a>
            <span class="PopupTitle popupTitle">Configure my wallet address</span>
          </div>
          <v-card-text>
            <div v-if="displaySpaceAccountCreationHelp">
              Current space doesn't have a wallet yet ? If you are manager of the space, you can create a new account using Metamask.
            </div>
            <span class="hidden">{{ currentAccountAlreadyInUse }}</span>

            <div v-if="currentAccountAlreadyInUse">
              Currently selected account in Metamask is already in use, you can't use it in this wallet.
            </div>
            <div v-else-if="displaySpaceAccountAssociationHelp">
              Would you like to use the current address <wallet-address :value="newAccountAddress" /> in Space Wallet ?
            </div>
            <div v-else-if="displayUserAccountAssociationHelp">
              Would you like to use the current address <wallet-address :value="newAccountAddress" /> in your Wallet ?
            </div>
            <div v-else-if="displayUserAccountChangeHelp">
              Would you like to replace your wallet address <wallet-address :value="oldAccountAddress" /> by the current address <wallet-address :value="newAccountAddress" /> ?
            </div>
          </v-card-text>
    
          <v-card-actions v-if="displayAccountHelpActions" class="text-xs-center">
            <v-spacer></v-spacer>
            <button class="btn btn-primary mr-2" @click="$emit('save-address-to-account')">
              Yes
            </button>
            <button class="btn" @click="addressAssociationDialog = false">
              No
            </button>
            <v-spacer></v-spacer>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </div>
    
    <!-- error alerts -->
    <div v-if="displayErrors">
      <div v-if="!metamaskEnabled" class="alert alert-info">
        <i class="uiIconInfo"></i>
        <span>Please install Metamask in your browser</span>
        <br />
        <button class="btn" @click.stop="installInstructionDialog = true">
          See help
        </button>
        <v-dialog v-model="installInstructionDialog" content-class="uiPopup" width="400" max-width="100wv" @keydown.esc="installInstructionDialog = false">
          <v-card>
            <div class="popupHeader ClearFix">
              <a class="uiIconClose pull-right" aria-hidden="true" @click="installInstructionDialog = false"></a>
              <span class="PopupTitle popupTitle">Enable wallet application</span>
            </div>
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
              <button class="btn" @click="installInstructionDialog = false">
                Close
              </button>
              <v-spacer></v-spacer>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </div>

      <div v-else-if="!metamaskConnected" class="alert alert-warning">
        <i class="uiIconWarning"></i>
        Please connect Metamask to {{ networkLabel && networkLabel.length ? networkLabel : 'an online network' }}
      </div>
      <div v-else-if="!account || !account.length" class="alert alert-warning">
        <i class="uiIconWarning"></i>
        Please select a valid account using Metamask
      </div>
      <div v-else-if="errorMessage" class="alert alert-error">
        <i class="uiIconError"></i>
        {{ errorMessage }}
      </div>
    </div>
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';

export default {
  components: {
    WalletAddress
  },
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