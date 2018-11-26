<template>
  <v-flex>
    <v-menu
      ref="selectedDateMenu"
      v-model="selectedDateMenu"
      transition="scale-transition"
      lazy
      offset-y
      xs12 lg6>
      <v-text-field
        slot="activator"
        v-model="dateFormatted"
        label="Date"
        persistent-hint
        prepend-icon="event"
        @blur="date = parseDate(dateFormatted)" />
      <v-date-picker
        v-model="selectedMonth"
        type="month"
        @input="selectedDateMenu = false" />
    </v-menu>
    <v-data-table
      :headers="kudosIdentitiesHeaders"
      :items="kudosIdentitiesList"
      :loading="loading"
      :sortable="true"
      :pagination.sync="pagination"
      class="elevation-1 mr-3 ml-3 mt-2 mb-2"
      hide-actions>
      <template slot="items" slot-scope="props">
        <tr>
          <td>
            <v-avatar size="36px">
              <img
                :src="props.item.avatar"
                onerror="this.src = '/eXoSkin/skin/images/system/SpaceAvtDefault.png'">
            </v-avatar>
          </td>
          <td class="text-xs-left">
            <a v-if="props.item.address" :href="props.item.url" rel="nofollow" target="_blank">{{ props.item.name }}</a>
            <div v-else>
              <del>
                <a :href="props.item.url" rel="nofollow" target="_blank" class="red--text">{{ props.item.name }}</a>
              </del>
              (No address)
            </div>
          </td>
          <td v-html="props.item.address">
          </td>
          <td v-html="props.item.received">
          </td>
          <td v-html="props.item.sent">
          </td>
        </tr>
      </template>
    </v-data-table>
    <send-kudos-modal
      :account="walletAddress"
      :contract-details="contractDetails"
      :recipients="kudosIdentitiesList"
      @sent="newTransactionPending"
      @error="error = $event" />
  </v-flex>
</template>

<script>
import SendKudosModal from './SendKudosModal.vue';

export default {
  components: {
    SendKudosModal
  },
  props: {
    wallets: {
      type: Array,
      default: function() {
        return [];
      }
    },
    contractDetails: {
      type: Object,
      default: function() {
        return {};
      }
    }
  },
  data: vm => {
    return {
      loading: false,
      error: null,
      selectedMonth: new Date().toISOString().substr(0, 7),
      dateFormatted: vm.formatDate(new Date().toISOString().substr(0, 7)),
      selectedDateMenu: false,
      kudosIdentitiesList: [],
      pagination: {
        descending: true
      },
      kudosIdentitiesHeaders: [
        {
          text: '',
          align: 'right',
          sortable: false,
          value: 'avatar',
          width: '36px'
        },
        {
          text: 'Name',
          align: 'left',
          sortable: false,
          value: 'name'
        },
        {
          text: 'Address',
          align: 'center',
          sortable: true,
          value: 'address'
        },
        {
          text: 'Received',
          align: 'center',
          sortable: true,
          value: 'received'
        },
        {
          text: 'Sent',
          align: 'center',
          sortable: true,
          value: 'sent'
        }
      ]
    };
  },
  computed: {
    displayButton() {
      return this.contractDetails && this.kudosIdentitiesList && this.kudosIdentitiesList.length;
    }
  },
  watch: {
    selectedMonth () {
      this.dateFormatted = this.formatDate(this.selectedMonth);
      document.dispatchEvent(new CustomEvent('exo-kudo-get-kudos-list', {'detail' : {'month' : new Date(this.selectedMonth)}}));
    }
  },
  created() {
    document.addEventListener('exo-kudo-get-kudos-list-loading', () => this.loading = true);
    document.addEventListener('exo-kudo-get-kudos-list-result', this.loadKudosList);
    this.$nextTick(() => {
      document.dispatchEvent(new CustomEvent('exo-kudo-get-kudos-list', {'detail' : {'month' : new Date(this.selectedMonth)}}));
    });
  },
  methods: {
    loadKudosList(event) {
      this.$emit("list-retrieved");
      this.error = null;
      this.kudosIdentitiesList = [];
      if(!event || !event.detail) {
        this.error = 'Empty kudos list is retrieved';
      } else if(event.detail.error) {
        console.debug(event.detail.error);
        this.error = event.detail.error;
      } else if(event.detail.list) {
        this.kudosIdentitiesList = event.detail.list;
        this.kudosIdentitiesList.forEach(walletKudos => {
          const wallet = this.wallets && this.wallets.find(wallet => wallet && wallet.id && wallet.id === walletKudos.id && wallet.type === walletKudos.type);
          if(wallet && wallet.address) {
            this.$set(walletKudos, "address", wallet.address);
          }
        });
      } else {
        this.error = 'Empty kudos list is retrieved';
      }
      this.loading = false;
    },
    formatDate (date) {
      if (!date){
        return null;
      }
      const [year, month] = date.split('-');
      return `${month}/${year}`;
    },
    parseDate (date) {
      if (!date) {
        return null;
      }
      const [month, year] = date.split('/');
      return `${year}-${month.padStart(2, '0')}`;
    }
  }
};
</script>