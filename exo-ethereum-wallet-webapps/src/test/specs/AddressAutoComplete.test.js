import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual, initiateBrowserWallet, sendTokens, sendEther} from '../TestUtils.js';

import AddressAutoComplete from '../../main/webapp/vue-app/components/AddressAutoComplete';

import flushPromises from 'flush-promises'

import {mount} from '@vue/test-utils';

describe('AddressAutoComplete.test.js', () => {
  const app = getWalletApp();

  // To initialize Web3
  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
      inputLabel: false,
      inputPlaceholder: false,
      noDataLabel: false,
      noAddress: false,
      disabled: false,
      bigField: false,
      ignoreItems: [],
      items: [],
      selectedValue: false,
      searchTerm: false,
      address: false,
      isLoadingSuggestions: false,
      addressLoad: false,
      currentUserItem: false,
      error: false,
  };

  it('AddressAutoComplete default data', () => {
    console.log('-- Test AddressAutoComplete default data');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    expectObjectValueEqual(addressAutoComplete.vm, defaultAttributesValues, 'AddressAutoComplete default data: ', null, true);
    expectCountElement(addressAutoComplete, addressAutoComplete.vm.id, 1);
  });

  it('AddressAutoComplete select address value', (done) => {
    console.log('-- Test AddressAutoComplete select address value');
    
    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const testedAddress =global.walletAddresses[3];
    addressAutoComplete.vm.selectedValue = testedAddress;

    return addressAutoComplete.vm.$nextTick(() => {
      try {
        const expectedResult = Object.assign({}, defaultAttributesValues);
        expectedResult.addressLoad = 'success';
        expectedResult.selectedValue = testedAddress;
        expectedResult.currentUserItem = {
          avatar: '/rest/v1/social/users/testuser1/avatar',
          enabled: true,
          address: global.defaultWalletAddress,
          name: 'Test User 1',
          type: 'user',
          id: 'testuser1',
          id_type: 'user_testuser1'
        };
        return flushPromises()
          .then(() => {
            console.log();

            expectObjectValueEqual(addressAutoComplete.vm, expectedResult, 'AddressAutoComplete select address value: ', null, true);

            expect(addressAutoComplete.emitted()['item-selected']).toEqual([[{
              id: 'testuser3',
              type: 'user',
              address: testedAddress,
              id_type: 'user_testuser3',
            }]]);
            done();
          })
          .catch(e => {
            done(e);
          });
      } catch(e) {
        done(e);
      }
    });
  });

});
