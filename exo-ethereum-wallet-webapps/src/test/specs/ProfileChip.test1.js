import {getWalletApp, initApp, expectObjectValueEqual, initiateBrowserWallet} from '../TestUtils.js';

import ProfileChip from '../../main/webapp/vue-app/components/ProfileChip.vue';

import {mount} from '@vue/test-utils';

import flushPromises from 'flush-promises';

jest.setTimeout(30000);

describe('ProfileChip.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    profileId: false,
    profileTechnicalId: false,
    profileType: false,
    avatar: false,
    displayName: false,
    address: false,
    tiptipPosition: false,
    labels: {
      CancelRequest: 'Cancel Request',
      Confirm: 'Confirm',
      Connect: 'Connect',
      Ignore: 'Ignore',
      RemoveConnection: 'Remove Connection',
      StatusTitle: 'Loading...',
      join: 'Join',
      leave: 'Leave',
      members: 'Members',
    },
    url: 'portal/undefined/profile/null',
  };

  it('ProfileChip default data', () => {
    console.log('-- ProfileChip default data');

    const profileChip = mount(ProfileChip, {
      attachToDocument: true,
    });

    expectObjectValueEqual(profileChip.vm, defaultAttributesValues, 'profileChip default data');
  });

  it("profileChip - when it's a wallet of a  simple user", (done) => {
    console.log("--- profileChip - when it's a wallet of a  simple user");

    global.walletAddress = global.walletAddresses[0];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, profileChip;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /* Not space*/ false, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();

        accountDetailCmp.wallet = {
          avatar: '/rest/v1/social/users/testuser1/avatar',
          address: global.defaultWalletAddress,
          name: 'Test User 1',
          type: 'user',
          id: 'testuser1',
          id_type: 'user_testuser1',
        };

        return flushPromises();
      })
      .then(() => {
        profileChip = accountDetailCmp.$refs.profileChip;
        expect(profileChip).toBeTruthy();
        return flushPromises();
      })
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.profileType = 'user';
        expectedData.avatar = '/rest/v1/social/users/testuser1/avatar';
        expectedData.profileId = 'testuser1';
        expectedData.displayName = 'Test User 1';
        expectedData.address = global.defaultWalletAddress;
        expectedData.url = 'portal/undefined/profile/testuser1';
        expectObjectValueEqual(profileChip, expectedData, "profileChip when it's a wallet of a  simple user", null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it("profileChip - when it's a wallet of a space", (done) => {
    console.log("--- profileChip - when it's a wallet of a space");

    global.walletAddress = global.walletAddresses[2];
    global.defaultWalletSettings.defaultPrincipalAccount = global.tokenAddress;
    global.defaultWalletSettings.defaultOverviewAccounts = global.defaultWalletSettings.defaultContractsToDisplay = [global.tokenAddress, 'ether'];

    const app = getWalletApp();
    let accountDetailCmp, contractDetails, profileChip;
    return initiateBrowserWallet(global.walletAddress, 'testpassword', /*space*/ true, /* generated */ true, /* not backedup */ false)
      .then(() => initApp(app))
      .then(() => flushPromises())
      .then(() => {
        contractDetails = app.vm.accountsDetails[global.tokenAddress];
        expect(contractDetails).toBeTruthy();
        app.vm.openAccountDetail(contractDetails);

        accountDetailCmp = app.vm.$refs.accountDetail;
        expect(accountDetailCmp).toBeTruthy();

        accountDetailCmp.wallet = {
          avatar: '/portal/rest/v1/social/spaces/global.eXo.env.portal.spaceGroup/avatar',
          address: global.defaultWalletAddress,
          name: 'Test Space',
          type: 'space',
          id: global.eXo.env.portal.spaceGroup,
        };

        return flushPromises();
      })
      .then(() => {
        profileChip = accountDetailCmp.$refs.profileChip;
        expect(profileChip).toBeTruthy();
        return flushPromises();
      })
      .then(() => {
        const expectedData = Object.assign({}, defaultAttributesValues);

        expectedData.profileType = 'space';
        expectedData.avatar = '/portal/rest/v1/social/spaces/global.eXo.env.portal.spaceGroup/avatar';
        expectedData.profileId = global.eXo.env.portal.spaceGroup;
        expectedData.displayName = 'Test Space';
        expectedData.address = global.defaultWalletAddress;
        expectedData.url = 'portal/g/:spaces:testspace/';
        expectObjectValueEqual(profileChip, expectedData, "profileChip when it's a wallet of a space", null, true);
      })
      .then(() => done())
      .catch((e) => done(e));
  });
});
