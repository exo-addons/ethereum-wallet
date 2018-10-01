<template>
  <a v-if="profileId" :id="id" :title="profileId" :href="`/portal/g/:spaces:${profileId}/`" rel="nofollow" target="_blank">
    <span v-html="displayName"></span>
  </a>
  <wallet-address v-else :value="address" />
</template>

<script>
import WalletAddress from './WalletAddress.vue';

export default {
  components: {
    WalletAddress
  },
  props: {
    profileId: {
      type: String,
      default: function() {
        return null;
      }
    },
    profileTechnicalId: {
      type: String,
      default: function() {
        return null;
      }
    },
    profileType: {
      type: String,
      default: function() {
        return null;
      }
    },
    avatar: {
      type: String,
      default: function() {
        return null;
      }
    },
    displayName: {
      type: String,
      default: function() {
        return null;
      }
    },
    address: {
      type: String,
      default: function() {
        return null;
      }
    }
  },
  data () {
    return {
      id: `chip${parseInt(Math.random() * 10000).toString().toString()}`,
      labels: {
        CancelRequest: "Cancel Request",
        Confirm: "Confirm",
        Connect: "Connect",
        Ignore: "Ignore",
        RemoveConnection: "Remove Connection",
        StatusTitle: "Loading...",
        join: "Join",
        leave: "Leave",
        members : "Members"
      }
    };
  },
  watch: {
    profileId(oldValue, newValue) {
      if (this.profileId) {
        // TODO disable tiptip because of high CPU usage using its code
        this.initTiptip();
      }
    }
  },
  created() {
    if (this.profileId) {
      // TODO disable tiptip because of high CPU usage using its code
      this.initTiptip();
    }
  },
  methods: {
    initTiptip() {
      if (this.profileType === 'space') {
        this.$nextTick(() => {
          $(`#${this.id}`).spacePopup({
            userName: eXo.env.portal.userName,
            spaceID: this.profileTechnicalId,
            restURL: '/portal/rest/v1/social/spaces/{0}',
            membersRestURL: '/portal/rest/v1/social/spaces/{0}/users?returnSize=true',
            managerRestUrl: '/portal/rest/v1/social/spaces/{0}/users?role=manager&returnSize=true',
            membershipRestUrl : '/portal/rest/v1/social/spacesMemberships?space={0}&returnSize=true',
            defaultAvatarUrl : `/portal/rest/v1/social/spaces/${this.profileId}/avatar`,
            deleteMembershipRestUrl : '/portal/rest/v1/social/spacesMemberships/{0}:{1}:{2}',
            labels: this.labels,
            content: false,
            keepAlive: true,
            defaultPosition: "left",
            maxWidth: "240px"
          });
        });
      } else {
        this.$nextTick(() => {
          $(`#${this.id}`).userPopup({
            restURL: '/portal/rest/social/people/getPeopleInfo/{0}.json',
            userId: this.profileId,
            labels: this.labels,
            content: false,
            keepAlive: true,
            defaultPosition: "left",
            maxWidth: "240px"
          });
        });
      }
    }
  }
};
</script>