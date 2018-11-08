<template>
  <v-flex v-if="contractDetails && contractDetails.title" id="accountDetail" class="text-xs-center white">
    <v-card-title class="align-start">
      <v-layout column>
        <v-flex id="accountDetailTitle" class="mt-3">
          <div class="headline title align-start">
            <v-icon class="primary--text accountDetailIcon">{{ contractDetails.icon }}</v-icon>
            Contract Details: {{ contractDetails.title }}
          </div>
          <v-progress-circular v-if="detailsLoading" :width="3" indeterminate color="primary" />
          <h3 v-if="contractDetails.contractBalanceFiat" class="font-weight-light">Balance: {{ contractDetails.contractBalanceFiat }} {{ fiatSymbol }} / {{ contractDetails.contractBalance }} ether</h3>
          <h4 v-if="contractDetails.sellPrice" class="grey--text font-weight-light">Sell price: {{ contractDetails.sellPrice }} ether</h4>
        </v-flex>

        <v-flex v-if="!isDisplayOnly" id="accountDetailActions">
          <!-- Send ether -->
          <send-ether-modal
            v-if="contractDetails.isOwner"
            :account="walletAddress"
            :balance="contractDetails.balance"
            :recipient="contractDetails.address"
            use-navigation
            @success="successSendingEther"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; error = $event" />

          <!-- add/remove admin -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="addAdmin"
            title="Add administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to add"
            input-label="Hibilitation level"
            input-placeholder="Choose a value between 1 and 5"
            @success="detailsLoading = false; $forceUpdate()"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="removeAdmin"
            title="Remove administrator"
            autocomplete-label="Administrator account"
            autocomplete-placeholder="Choose an administrator account to remove"
            @success="detailsLoading = false; $forceUpdate()"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />

          <!-- approve/disapprove account -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 1"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="approveAccount"
            title="Approve account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to approve"
            @success="detailsLoading = false; $forceUpdate()"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 1"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="disapproveAccount"
            title="Disapprove account"
            autocomplete-label="Account"
            autocomplete-placeholder="Choose a user or space to disapprove"
            @success="detailsLoading = false; $forceUpdate()"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />

          <!-- pause/unpause contract -->
          <contract-admin-modal
            v-if="!contractDetails.isPaused && contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="pause"
            title="Pause contract"
            @success="refresh"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />
          <contract-admin-modal
            v-if="contractDetails.isPaused && contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="unPause"
            title="Unpause contract"
            @success="refresh"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />

          <!-- set sell price -->
          <contract-admin-modal
            v-if="contractDetails.adminLevel >= 5"
            :contract-details="contractDetails"
            :wallet-address="walletAddress"
            method-name="setSellPrice"
            title="Set sell price"
            input-label="Token sell price"
            input-placeholder="Token sell price in ether"
            convert-wei
            @success="refresh"
            @sent="detailsLoading = true; $forceUpdate()"
            @error="detailsLoading = false; $forceUpdate(); error = $event" />
        </v-flex>
        <v-btn icon class="rightIcon" @click="$emit('back')">
          <v-icon>close</v-icon>
        </v-btn>
      </v-layout>
    </v-card-title>

    <transactions-list
      id="transactionsList"
      ref="transactionsList"
      :network-id="networkId"
      :account="contractDetails.address"
      :contract-details="contractDetails"
      :fiat-symbol="fiatSymbol"
      :error="error"
      display-full-transaction
      @error="error = $event" />

  </v-flex>
</template>

<script>
import SendEtherModal from './SendEtherModal.vue';
import ContractAdminModal from './ContractAdminModal.vue';
import TransactionsList from './TransactionsList.vue';

import {retrieveContractDetails} from '../WalletToken.js';
import {etherToFiat} from '../WalletUtils.js';

export default {
  components: {
    SendEtherModal,
    ContractAdminModal,
    TransactionsList
  },
  props: {
    isDisplayOnly: {
      type: Boolean,
      default: function() {
        return false;
      }
    },
    networkId: {
      type: Number,
      default: function() {
        return 0;
      }
    },
    walletAddress: {
      type: String,
      default: function() {
        return null;
      }
    },
    fiatSymbol: {
      type: String,
      default: function() {
        return null;
      }
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      error: null
    };
  },
  watch: {
    contractDetails() {
      this.error = null;
    }
  },
  methods: {
    successSendingEther() {
      this.refresh()
        .then(() => {
          this.$emit('success', this.contractDetails);
          this.$forceUpdate();
        });
    },
    refresh() {
      if (this.detailsLoading) {
        return retrieveContractDetails(this.walletAddress, this.contractDetails)
          .then(() => {
            this.detailsLoading = false;
            this.$forceUpdate();
          });
      } else {
        return Promise.resolve(false);
      }
    }
  }
};
</script>