<template>
  <v-layout row>
    <v-flex xs12 sm6 offset-sm3>
      <v-card>
        <v-card-media class="blue white--text" height="200px">
          <v-layout column fill-height>
            <v-card-title>
              <v-btn dark icon @click="$emit('back')">
                <v-icon>arrow_back</v-icon>
              </v-btn>

              <v-spacer></v-spacer>
              <div class="title">{{ contractDetail.title }}</div>
              <v-spacer></v-spacer>
            </v-card-title>

            <v-card-title class="white--text pl-5 pt-5">
              <v-spacer></v-spacer>
              <div class="display-1">{{ contractDetail.balance }} {{ contractDetail.symbol }}</div>
              <v-spacer></v-spacer>
            </v-card-title>
          </v-layout>
        </v-card-media>

        <div v-if="contractDetail.isContract" class="text-xs-center">
          <send-tokens-modal :contract="contractDetail.contract" @close="dialog = false"></send-tokens-modal>
        </div>
        <v-divider></v-divider>
  
        <token-transactions v-if="contractDetail.isContract" :account="account" :contract="contractDetail.contract"></token-transactions>
        <general-transactions v-else :account="account"></general-transactions>
      </v-card>
    </v-flex>
  </v-layout>
</template>

<script>
import AddContractModal from './AddContractModal.vue';
import GeneralTransactions from './GeneralTransactions.vue';
import TokenTransactions from './TokenTransactions.vue';
import SendTokensModal from './SendTokensModal.vue';

import TruffleContract from 'truffle-contract';
import LocalWeb3 from 'web3';
import {ERC20_COMPLIANT_CONTRACT_ABI} from '../WalletConstants.js';
import {getContractsDetails} from '../WalletToken.js';

export default {
  components: {
    AddContractModal,
    SendTokensModal,
    GeneralTransactions,
    TokenTransactions
  },
  props: {
    account: {
      type: String,
      default: function() {
        return {};
      }
    },
    contractDetail: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data() {
    return {
      dialog: false
    };
  }
};
</script>