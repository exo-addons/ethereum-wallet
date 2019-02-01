import {getWalletApp, initApp, expectObjectValueEqual} from '../TestUtils.js';

import InformationBubble from '../../main/webapp/vue-app/components/InformationBubble.vue';

import {mount} from '@vue/test-utils';

jest.setTimeout(30000);

describe('informationBubble.test.js', () => {
  const app = getWalletApp();

  beforeAll(() => {
    return initApp(app);
  });

  const defaultAttributesValues = {
    title: false,
    dialog: false,
  };

  it('informationBubble default data', () => {
    console.log('-- informationBubble default data');

    const informationBubble = mount(InformationBubble, {
      attachToDocument: true,
    });

    expectObjectValueEqual(informationBubble.vm, defaultAttributesValues, 'informationBubble default data');
  });
});
