import InformationBubble from '../../main/webapp/vue-app/components/InformationBubble.vue';

import {expectObjectValueEqual} from '../TestUtils.js';
import {mount} from '@vue/test-utils';

describe('informationBubble.test.js', () => {
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
