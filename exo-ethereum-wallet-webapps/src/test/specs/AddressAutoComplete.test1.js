import {getWalletApp, initApp, expectCountElement, expectObjectValueEqual} from '../TestUtils.js';

import AddressAutoComplete from '../../main/webapp/vue-app/components/AddressAutoComplete';

import flushPromises from 'flush-promises';

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

  const expectedResult = Object.assign({}, defaultAttributesValues);
  expectedResult.currentUserItem = {
    avatar: '/rest/v1/social/users/testuser1/avatar',
    enabled: true,
    address: global.defaultWalletAddress,
    name: 'Test User 1',
    type: 'user',
    id: 'testuser1',
    id_type: 'user_testuser1',
  };

  it('AddressAutoComplete default data', () => {
    console.log('-- Test AddressAutoComplete default data');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    expectObjectValueEqual(addressAutoComplete.vm, defaultAttributesValues, 'AddressAutoComplete default data: ', null, true);
    expectCountElement(addressAutoComplete, addressAutoComplete.vm.id, 1);
  });

  it('AddressAutoComplete select known address value', (done) => {
    console.log('-- Test AddressAutoComplete select known address value');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const testedAddress = global.walletAddresses[3];
    addressAutoComplete.vm.selectedValue = testedAddress;

    return flushPromises()
      .then(() => {
        expectedResult.addressLoad = 'success';
        expectedResult.selectedValue = testedAddress;
        expectObjectValueEqual(addressAutoComplete.vm, expectedResult, 'AddressAutoComplete select known address value: ', null, true);
        expect(addressAutoComplete.emitted()['item-selected']).toEqual([
          [
            {
              id: 'testuser3',
              type: 'user',
              address: testedAddress,
              id_type: 'user_testuser3',
            },
          ],
        ]);
      })
      .then(() => done())
      .catch((e) => done(e));
  });

  it('AddressAutoComplete select unknown address value', (done) => {
    console.log('-- Test AddressAutoComplete select unknown address value');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const testedAddress = '0x1111111111111111111111111111111111111111';
    addressAutoComplete.vm.selectedValue = testedAddress;

    return flushPromises()
      .then(() => {
        expectedResult.addressLoad = 'success';
        expectedResult.selectedValue = testedAddress;

        expectObjectValueEqual(addressAutoComplete.vm, expectedResult, 'AddressAutoComplete select unknown address value: ', null, true);
        expect(addressAutoComplete.emitted()['item-selected']).toEqual([
          [
            {
              id: testedAddress,
              type: null,
              address: testedAddress,
            },
          ],
        ]);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete search users', (done) => {
    console.log('-- Test AddressAutoComplete search users');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const searchTerm = 'Test User 1';
    const selectedType = 'user';
    const selectedId = 'testuser1';
    const selectedName = 'Test User 1';
    const selectedAddress = window.walletAddresses[1];
    const selectedIdType = `${selectedType}_${selectedId}`;
    const expectedSelectedItem = {
      id: selectedId,
      type: selectedType,
      address: selectedAddress,
      id_type: selectedIdType,
      name: selectedName,
    };

    // set disabled to not have as workaround to autocomplete status errors (Maximum stack overflow)
    addressAutoComplete.setProps({disabled: true});
    addressAutoComplete.vm.searchTerm = searchTerm;
    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.disabled = true;
        expectedResultClone.addressLoad = false;
        expectedResultClone.selectedValue = false;
        expectedResultClone.items = expectedResultClone.items.slice();
        expectedResultClone.items.push(expectedSelectedItem);

        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete search users: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete search with address', (done) => {
    console.log('-- Test AddressAutoComplete with address');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const searchTerm = '0x1111111111111111111111111111111111111111';
    const selectedId = searchTerm;
    const selectedName = searchTerm;
    const selectedAddress = searchTerm;
    const expectedSelectedItem = {
      id: selectedId,
      address: selectedAddress,
      name: selectedName,
    };

    // set disabled to not have as workaround to autocomplete status errors (Maximum stack overflow)
    addressAutoComplete.setProps({disabled: true});
    addressAutoComplete.vm.searchTerm = searchTerm;
    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.disabled = true;
        expectedResultClone.addressLoad = false;
        expectedResultClone.selectedValue = false;
        expectedResultClone.items = expectedResultClone.items.slice();
        expectedResultClone.items.push(expectedSelectedItem);

        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete search with address: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete search with known address', (done) => {
    console.log('-- Test AddressAutoComplete search users');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const searchTerm = window.walletAddresses[1];
    const selectedAddress = window.walletAddresses[1];
    const expectedSelectedItem = {
      id: selectedAddress,
      address: selectedAddress,
      name: selectedAddress,
    };

    // set disabled to not have as workaround to autocomplete status errors (Maximum stack overflow)
    addressAutoComplete.setProps({disabled: true});
    addressAutoComplete.vm.searchTerm = searchTerm;
    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.disabled = true;
        expectedResultClone.addressLoad = false;
        expectedResultClone.selectedValue = false;
        expectedResultClone.items = expectedResultClone.items.slice();
        expectedResultClone.items.push(expectedSelectedItem);

        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete search users: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete test filter ignored items', (done) => {
    console.log('-- Test AddressAutoComplete test filter ignored items');

    const addressAutoComplete = mount(AddressAutoComplete, {
      propsData: {
        ignoreItems: ['testuser1', 'testuser3'],
      },
      attachToDocument: true,
    });

    const testuser1Details = global.userAddresses['user_testuser1'];
    const testuser3Details = global.userAddresses['user_testuser3'];
    const testuser4Details = global.userAddresses['user_testuser4'];

    try {
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser1Details, 'Test', testuser1Details.name)).toBeFalsy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser3Details, 'Test', testuser3Details.name)).toBeFalsy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser4Details, 'Test', testuser4Details.name)).toBeTruthy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser4Details, '1', testuser4Details.name)).toBeFalsy();
      done();
    } catch (e) {
      done(e);
    }
  });

  it('AddressAutoComplete test filter without ignored items', (done) => {
    console.log('-- Test AddressAutoComplete test filter ignored items');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const testuser1Details = global.userAddresses['user_testuser1'];
    const testuser3Details = global.userAddresses['user_testuser3'];
    const testuser4Details = global.userAddresses['user_testuser4'];

    try {
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser1Details, 'Test', testuser1Details.name)).toBeTruthy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser3Details, 'Test', testuser3Details.name)).toBeTruthy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser4Details, 'Test', testuser4Details.name)).toBeTruthy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser1Details, '1', testuser1Details.name)).toBeTruthy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser3Details, '1', testuser3Details.name)).toBeFalsy();
      expect(addressAutoComplete.vm.filterIgnoredItems(testuser4Details, '1', testuser4Details.name)).toBeFalsy();
      done();
    } catch (e) {
      done(e);
    }
  });

  it('AddressAutoComplete select a user having an address', (done) => {
    console.log('-- Test AddressAutoComplete select a user having an address');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const selectedType = 'user';
    const selectedId = 'testuser3';
    const selectedAddress = window.walletAddresses[3];
    const selectedIdType = `${selectedType}_${selectedId}`;
    addressAutoComplete.vm.selectedValue = selectedIdType;

    return flushPromises()
      .then(() => {
        expectedResult.addressLoad = 'success';
        expectedResult.selectedValue = selectedIdType;

        expectObjectValueEqual(addressAutoComplete.vm, expectedResult, 'AddressAutoComplete select a user having an address: ', null, true);
        expect(addressAutoComplete.emitted()['item-selected']).toEqual([
          [
            {
              id: selectedId,
              type: selectedType,
              address: selectedAddress,
            },
          ],
        ]);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete select a user not having an address', (done) => {
    console.log('-- Test AddressAutoComplete select a user not having an address');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const selectedType = 'user';
    const selectedId = 'testuser25';
    const selectedAddress = null;
    const selectedIdType = `${selectedType}_${selectedId}`;
    addressAutoComplete.vm.selectedValue = selectedIdType;

    return flushPromises()
      .then(() => {
        expectedResult.addressLoad = 'error';
        expectedResult.selectedValue = selectedIdType;
        expectObjectValueEqual(addressAutoComplete.vm, expectedResult, 'AddressAutoComplete select a user not having an address: ', null, true);
        expect(addressAutoComplete.emitted()['item-selected']).toEqual([
          [
            {
              id: selectedId,
              type: selectedType,
              address: selectedAddress,
            },
          ],
        ]);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete selectItem method with a user having an address', (done) => {
    console.log('-- Test AddressAutoComplete selectItem method with a user having an address');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const selectedType = 'user';
    const selectedId = 'testuser3';
    const selectedName = 'Test User 3';
    const selectedAddress = window.walletAddresses[3];
    const selectedIdType = `${selectedType}_${selectedId}`;
    const expectedSelectedItem = {
      id: selectedId,
      type: selectedType,
      address: selectedAddress,
      id_type: selectedIdType,
      name: selectedName,
    };
    expect(addressAutoComplete.vm.canAddItem(expectedSelectedItem)).toBeTruthy();
    addressAutoComplete.vm.selectItem(selectedId, selectedType);

    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.addressLoad = 'success';
        expectedResultClone.selectedValue = selectedIdType;
        expectedResultClone.items = expectedResultClone.items.slice();
        expectedResultClone.items.push(expectedSelectedItem);
        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete selectItem method with a user having an address: ', null, true);

        // Test clear method
        addressAutoComplete.vm.clear();
        const expectedClearedResult = Object.assign({}, defaultAttributesValues);
        expectedClearedResult.currentUserItem = expectedResult.currentUserItem;
        expectedClearedResult.items = [expectedClearedResult.currentUserItem];
        expectObjectValueEqual(addressAutoComplete.vm, expectedClearedResult, 'AddressAutoComplete test clear method: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete selectItem method with a user not having an address', (done) => {
    console.log('-- Test AddressAutoComplete selectItem method with a user not having an address');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const selectedType = 'user';
    const selectedId = 'testuser25';
    addressAutoComplete.vm.selectItem(selectedId, selectedType);

    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.addressLoad = false;
        expectedResultClone.selectedValue = false;
        expectedResultClone.items = expectedResultClone.items.slice();
        const expectedSelectedItem = {
          id_type: false,
        };
        expectedResultClone.items.push(expectedSelectedItem);

        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete selectItem method with a user not having an address: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });

  it('AddressAutoComplete selectItem with id only', (done) => {
    console.log('-- Test AddressAutoComplete selectItem with id only');

    const addressAutoComplete = mount(AddressAutoComplete, {
      attachToDocument: true,
    });

    const selectedType = null;
    const selectedId = 'testuser25';
    const selectedName = selectedId;
    const selectedIdType = selectedId;
    addressAutoComplete.vm.selectItem(selectedId, selectedType);

    return flushPromises()
      .then(() => {
        const expectedResultClone = Object.assign({}, expectedResult);
        expectedResultClone.addressLoad = 'success';
        expectedResultClone.selectedValue = selectedIdType;
        expectedResultClone.items = expectedResultClone.items.slice();
        const expectedSelectedItem = {
          id_type: selectedIdType,
          name: selectedName,
        };
        expectedResultClone.items.push(expectedSelectedItem);
        expectObjectValueEqual(addressAutoComplete.vm, expectedResultClone, 'AddressAutoComplete selectItem with id only: ', null, true);
        done();
      })
      .catch((e) => {
        done(e);
      });
  });
});
