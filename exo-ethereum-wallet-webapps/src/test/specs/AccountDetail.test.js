import { shallow } from 'vue-test-utils';

import AccountDetail from '../../main/webapp/vue-app/components/AccountDetail';

describe('AccountDetail.test.js', () => {
  let app;
  const userSettings = {
    username: 'root',
    fullName: 'Root Root',
    isOnline: true,
    status: 'available'
  };
  const roomsData = {
    unreadOffline: '0',
    unreadOnline: '0',
    unreadSpaces: '0',
    unreadTeams: '0',
    rooms: [
      {
        fullName: 'room1',
        user: 'team-a11192fa4a461dc023ac8b6d1cd85951a385d418',
        room: 'a11192fa4a461dc023ac8b6d1cd85951a385d418'
      },
      {
        fullName: 'room2',
        user: 'team-a11192fa4a461dc023ac8b6d1cd85951a385d419',
        room: 'a11192fa4a461dc023ac8b6d1cd85951a385d419'
      }
    ]
  };

  beforeEach(() => {
    app = shallow(ExoChatApp, {
      stubs: {
        'exo-modal': ExoModal,
        'exo-chat-contact': ExoChatContact,
        'exo-chat-contact-list': ExoChatContactList,
        'exo-chat-global-notification-modal': ExoChatGlobalNotificationModal
      },
      mocks: {
        $t: () => {},
        $constants : chatConstants
      },
      attachToDocument: true
    });
    
  });

  it('chat-application has offline class and change to online when chat server connected', () => {
    expect(app.find('#chat-application').classes()).toContain('offline');
    expect(app.find('#chat-application').classes()).not.toContain('online');
    app.trigger(chatConstants.EVENT_CONNECTED);
    expect(app.find('#chat-application').classes()).toContain('online');
    expect(app.find('#chat-application').classes()).not.toContain('offline');
  });

  it('contact label contain user full name', () => {
    expect(app.find('.contactDetail .contactLabel span').text()).toBe('');
    app.vm.initSettings(userSettings);
    app.update();
    expect(app.find('.contactDetail .contactLabel span').text()).toBe(userSettings.fullName);
  });


  it('init rooms data', () => {
    app.vm.initChatRooms(roomsData);
    expect(app.vm.contactList[0]).toEqual(roomsData.rooms[0]);
  });


  it('settings modal should be hidden and opned on click on settings icon', () => {
    expect(app.find('#chatPreferences').element.style.display).toBe('none');
    app.find('.chat-user-settings').trigger('click');
    expect(app.find('#chatPreferences').element.style.display).not.toBe('none');
  });

  it('side menu should exist only on mobile', () => {
    expect(app.find('.chat-side-menu').exists()).toBe(false);
    app.vm.mq = 'mobile';
    app.update();
    expect(app.find('.chat-side-menu').exists()).toBe(true);
  });


  
});

