<template>
  <v-chip outline class="walletAddressCmp">
    <v-btn
      v-if="allowCopy"
      title="Copy to clipboard"
      class="ml-0 mr-0 mb-0 mt-0"
      icon
      small
      @click="copyToClipboard">
      <v-icon size="12" dark>
        fa-copy
      </v-icon>
    </v-btn>
    <input
      v-if="isEditing && allowEdit"
      v-model="labelDetail.label"
      :disabled="loading"
      name="addressLabel"
      placeholder="Label"
      class="walletAddressLabelInput mr-2"
      autofocus
      @keydown.esc="reset"
      @keyup.enter="save">
    <a
      v-else
      :href="addressEtherscanLink && `${addressEtherscanLink}${value}` || '#'"
      :title="addressEtherscanLink && 'Open on etherscan' || ''"
      target="_blank"
      class="walletAddressLabel ellipsis mr-2">
      <template v-if="labelDetail && labelDetail.label">
        {{ labelDetail.label }}
      </template>
      <template v-else>
        {{ value }}
      </template>
    </a>
    <v-slide-x-reverse-transition v-if="allowEdit" mode="out-in">
      <v-icon
        :key="`icon-${isEditing}`"
        :color="isEditing ? 'success' : 'info'"
        class="walletAddressEdit"
        size="16"
        @click="editOrSave">
        {{ isEditing ? 'fa-check-circle' : 'fa-pencil-alt' }}
      </v-icon>
    </v-slide-x-reverse-transition>

    <input
      v-if="allowCopy"
      ref="clipboardInput"
      v-model="value"
      class="copyToClipboardInput"
      type="text">
  </v-chip>
</template>

<script>
import {saveAddressLabel} from '../WalletAddressRegistry.js';
import {getAddressEtherscanlink} from '../WalletUtils.js';

export default {
  props: {
    value: {
      type: String,
      default: function() {
        return null;
      },
    },
    allowCopy: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
    allowEdit: {
      type: Boolean,
      default: function() {
        return true;
      },
    },
  },
  data() {
    return {
      isEditing: false,
      loading: false,
      isAdmin: null,
      addressEtherscanLink: null,
      labelDetail: {},
      labelDetailToEdit: {},
    };
  },
  created() {
    this.init();
    this.isAdmin = window.walletSettings && window.walletSettings.isAdmin;
  },
  methods: {
    init() {
      this.refresh();
    },
    refresh() {
      if(!this.value) {
        return;
      }
      this.addressEtherscanLink = getAddressEtherscanlink(window.walletSettings && window.walletSettings.defaultNetworkId);

      if (!window.walletSettings.userPreferences
          || !window.walletSettings.userPreferences.addresesLabels) {
        this.labelDetail = {
          address: this.value.toLowerCase(),
        };
        return;
      }
      const walletAddress = this.value.toLowerCase();

      this.labelDetail = window.walletSettings.userPreferences.addresesLabels.find(label => label && label.address.toLowerCase() === walletAddress) || {address: walletAddress};
      this.labelDetail = Object.assign({}, this.labelDetail);
    },
    editOrSave() {
      if(!this.allowEdit) {
        return;
      }
      if(this.isEditing) {
        return this.save();
      }
      this.isEditing = !this.isEditing;
    },
    reset() {
      this.refresh();
      this.isEditing = false;
    },
    save() {
      if(!this.allowEdit) {
        return;
      }

      this.loading = true;
      return saveAddressLabel(this.labelDetail)
        .then((labelDetail) => {
          this.labelDetail = labelDetail;

          if(!window.walletSettings.userPreferences.addresesLabels) {
            window.walletSettings.userPreferences.addresesLabels = [];
          }
          const walletAddress = this.value.toLowerCase();
          const labelDetailToChange = window.walletSettings.userPreferences.addresesLabels.find(label => label && label.address.toLowerCase() === walletAddress);
          if(labelDetailToChange) {
            Object.assign(labelDetailToChange, labelDetail);
          } else {
            window.walletSettings.userPreferences.addresesLabels.push(this.labelDetail);
          }

          this.refresh();
          this.isEditing = false;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    copyToClipboard() {
      this.$refs.clipboardInput.select();
      try {
        document.execCommand('copy');
      } catch(e) {
        console.debug('Error executing document.execCommand', e);
      }
    },
  },
};
</script>
