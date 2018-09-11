<template>
  <v-flex id="walletMetamaskSetup">
    <div v-if="!metamaskEnabled" class="alert alert-info">
      <i class="uiIconInfo"></i>
      <span v-if="walletAddress">Current wallet is in readonly mode.</span>
      <br v-if="walletAddress"/>
      <span>Please install or enable Metamask extension in your browser</span>
      <br />
      <button class="btn" @click.stop="installInstructionDialog = true">
        See help
      </button>
      <div>Or</div>
      <button class="btn" @click.stop="disableMetamaskUsage">
        Switch to use browser wallet
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
      Please connect to Metamask
    </div>
    <div v-else-if="errorMessage" class="alert alert-error">
      <i class="uiIconError"></i>
      {{ errorMessage }}
    </div>
    <div v-else-if="displaySpaceMetamaskEnableHelp" class="alert alert-info">
      <i class="uiIconInfo"></i>
      Please enable/install Metamask to be able to add a new space account
    </div>
    <div v-else-if="displayNotSameNetworkWarning" class="alert alert-warning">
      <i class="uiIconWarning"></i>
      Please switch Metamask to <strong>{{ networkLabel }}</strong>
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
            <div v-if="currentAccountAlreadyInUse">
              Currently selected account in Metamask is already in use, you can't use it in this wallet.
            </div>
            <div v-else-if="displaySpaceAccountAssociationHelp">
              Would you like to use the current address <wallet-address :value="detectedMetamaskAccount" /> in Space Wallet ?
            </div>
            <div v-else-if="displayUserAccountAssociationHelp">
              Would you like to use the current address <wallet-address :value="detectedMetamaskAccount" /> in your Wallet ?
            </div>
            <div v-else-if="displayUserAccountChangeHelp">
              Would you like to replace your wallet address <wallet-address :value="associatedWalletAddress" /> by the current address <wallet-address :value="detectedMetamaskAccount" /> ?
            </div>
          </v-card-text>
    
          <v-card-actions v-if="displayAccountHelpActions" class="text-xs-center">
            <v-spacer></v-spacer>
            <button class="btn btn-primary mr-2" @click="saveNewAddressInWallet()">
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
  </v-flex>
</template>

<script>
import WalletAddress from './WalletAddress.vue';
import * as constants from '../WalletConstants.js';

import {disableMetamask} from '../WalletUtils.js';
import {searchFullName, searchUserOrSpaceObject, saveNewAddress} from '../WalletAddressRegistry.js';

export default {
  components: {
    WalletAddress
  },
  props: {
    isReadOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    isSpace: {
      type: Boolean,
      default: function() {
        return false;
      }
    }
  },
  data() {
    return {
      addressAssociationDialog: false,
      installInstructionDialog: false,
      networkLabel: null,
      sameConfiguredNetwork: true,
      associatedWalletAddress: null,
      detectedMetamaskAccount: null,
      isSpaceAdministrator: false,
      currentAccountAlreadyInUse: false,
      metamaskEnabled: false
    };
  },
  computed: {
    displayNotSameNetworkWarning() {
      return !this.sameConfiguredNetwork && (!this.isSpace || !this.associatedWalletAddress);
    },
    displaySpaceAccountAssociationHelp() {
      return this.isSpace && this.sameConfiguredNetwork && !this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayUserAccountAssociationHelp() {
      return !this.isSpace && this.sameConfiguredNetwork && !this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayUserAccountChangeHelp() {
      return !this.isSpace && this.sameConfiguredNetwork && this.associatedWalletAddress && this.detectedMetamaskAccount;
    },
    displayAccountHelpActions() {
      return this.sameConfiguredNetwork && !this.currentAccountAlreadyInUse && (this.displayUserAccountChangeHelp || this.displayUserAccountAssociationHelp || this.displaySpaceAccountAssociationHelp);
    },
    displaySpaceMetamaskEnableHelp() {
      return this.isSpace && !this.associatedWalletAddress && !this.detectedMetamaskAccount;
    },
    newAddressDetected() {
      return this.sameConfiguredNetwork
        && this.detectedMetamaskAccount
        && this.associatedWalletAddress !== this.detectedMetamaskAccount;
    }
  },
  methods: {
    init() {
      this.metamaskEnabled = window.web3 && window.web3.currentProvider && window.web3.currentProvider.isMetaMask;
      this.metamaskConnected = this.metamaskEnabled && window.walletSettings.metamaskConnected;

      if (window.walletSettings) {
        this.associatedWalletAddress = window.walletSettings.userPreferences.walletAddress;
      }

      if (this.metamaskEnabled && this.metamaskConnected) {
        if (window.localWeb3.eth.defaultAccount) {
          this.detectedMetamaskAccount = window.localWeb3.eth.defaultAccount.toLowerCase();
        }

        this.sameConfiguredNetwork = window.walletSettings.defaultNetworkId === window.walletSettings.currentNetworkId;
        this.networkLabel = constants.NETWORK_NAMES[window.walletSettings.defaultNetworkId];
        if (!this.networkLabel) {
          this.networkLabel = window.walletSettings.providerURL;
        }

        // compute detected account associated user/space
        if (this.detectedMetamaskAccount !== this.associatedWalletAddress) {
          if (this.isSpace) {
            return this.initSpaceAccount().then(this.initAccount);
          } else {
            return this.initAccount();
          }
        }
      }
    },
    initSpaceAccount() {
      return searchUserOrSpaceObject(eXo.env.portal.spaceGroup, 'space')
        .then((spaceObject, error) => {
          if (error) {
            throw error;
          }

          if(spaceObject && spaceObject.managers && spaceObject.managers.length
              && spaceObject.managers.indexOf(eXo.env.portal.userName) > -1) {
            return this.isSpaceAdministrator = true;
          }
          return false;
        });
    },
    initAccount() {
      return searchFullName(this.detectedMetamaskAccount)
        .then((item, error) => {
          if (error) {
            throw error;
          }
          if (item && item.id && item.id.length) {
            this.currentAccountAlreadyInUse = true;
          }
          return item;
        })
        .catch(e => {
          console.debug("searchAddress method - error", e);
        });
    },
    saveNewAddressInWallet() {
      this.$emit("loading");
      return saveNewAddress(
        this.isSpace ? eXo.env.portal.spaceGroup : eXo.env.portal.userName,
        this.isSpace ? 'space' : 'user',
        this.detectedMetamaskAccount)

        .then((resp, error) => {
          if (error) {
            throw error;
          }
          if (resp && resp.ok) {
            this.associatedWalletAddress = this.detectedMetamaskAccount;
            this.displaySpaceAccountCreationHelp = false;
            if (this.isSpace) {
              this.init();
            }
          } else {
            this.errorMessage = 'Error saving new Wallet address';
          }
          this.$emit("end-loading");
        })
        .catch(e => {
          console.debug("saveNewAddress method - error", e);
          this.$emit("error", `Error saving new Wallet address: ${e}`);
        });
    },
    disableMetamaskUsage() {
      disableMetamask();
      this.$emit("refresh");
    }
  }
};
</script>