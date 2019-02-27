/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.addon.ethereum.wallet.test.mock;

import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.application.PortletPreferenceRequiredPlugin;
import org.exoplatform.social.core.space.*;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleListener;
import org.exoplatform.social.core.space.spi.SpaceService;

@SuppressWarnings("all")
public class SpaceServiceMock implements SpaceService {

  @Override
  public Space getSpaceByDisplayName(String spaceDisplayName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space getSpaceByPrettyName(String spacePrettyName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space getSpaceByGroupId(String groupId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space getSpaceById(String spaceId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space getSpaceByUrl(String spaceUrl) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getAllSpacesWithListAccess() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getAllSpacesByFilter(SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getMemberSpaces(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getMemberSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getAccessibleSpacesWithListAccess(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getAccessibleSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getSettingableSpaces(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getSettingabledSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getInvitedSpacesWithListAccess(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getInvitedSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getPublicSpacesWithListAccess(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getPublicSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getPendingSpacesWithListAccess(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getPendingSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space createSpace(Space space, String creatorUserId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space updateSpace(Space existingSpace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space updateSpaceAvatar(Space existingSpace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Space updateSpaceBanner(Space existingSpace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteSpace(Space space) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addPendingUser(Space space, String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removePendingUser(Space space, String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPendingUser(Space space, String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addInvitedUser(Space space, String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeInvitedUser(Space space, String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInvitedUser(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void addMember(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeMember(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isMember(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setManager(Space space, String userId, boolean isManager) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isManager(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isOnlyManager(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean hasAccessPermission(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean hasSettingPermission(Space space, String userId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void registerSpaceListenerPlugin(SpaceListenerPlugin spaceListenerPlugin) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void unregisterSpaceListenerPlugin(SpaceListenerPlugin spaceListenerPlugin) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setSpaceApplicationConfigPlugin(SpaceApplicationConfigPlugin spaceApplicationConfigPlugin) {
    throw new UnsupportedOperationException();

  }

  @Override
  public SpaceApplicationConfigPlugin getSpaceApplicationConfigPlugin() {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getAllSpaces() throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public Space getSpaceByName(String spaceName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getSpacesByFirstCharacterOfName(String firstCharacterOfName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getSpacesBySearchCondition(String condition) throws Exception {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getAccessibleSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getVisibleSpaces(String userId, SpaceFilter spaceFilter) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public SpaceListAccess getVisibleSpacesWithListAccess(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();

  }

  @Override
  public SpaceListAccess getUnifiedSearchSpacesWithListAccess(String userId, SpaceFilter spaceFilter) {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getEditableSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getInvitedSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getPublicSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getPendingSpaces(String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public Space createSpace(Space space, String creator, String invitedGroupId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void saveSpace(Space space, boolean isNew) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void renameSpace(Space space, String newDisplayName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void renameSpace(String remoteId, Space space, String newDisplayName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void deleteSpace(String spaceId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void initApp(Space space) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void initApps(Space space) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void deInitApps(Space space) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void addMember(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeMember(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<String> getMembers(Space space) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<String> getMembers(String spaceId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setLeader(Space space, String userId, boolean isLeader) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setLeader(String spaceId, String userId, boolean isLeader) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isLeader(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isLeader(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isOnlyLeader(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean isOnlyLeader(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isMember(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public boolean hasAccessPermission(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEditPermission(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEditPermission(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInvited(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInvited(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPending(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPending(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void installApplication(String spaceId, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void installApplication(Space space, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void activateApplication(Space space, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void activateApplication(String spaceId, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void deactivateApplication(Space space, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void deactivateApplication(String spaceId, String appId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeApplication(Space space, String appId, String appName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeApplication(String spaceId, String appId, String appName) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void updateSpaceAccessed(String remoteId, Space space) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public List<Space> getLastAccessedSpace(String remoteId, String appId, int offset, int limit) throws SpaceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Space> getLastSpaces(int limit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getLastAccessedSpace(String remoteId, String appId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void requestJoin(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void requestJoin(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void revokeRequestJoin(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void revokeRequestJoin(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void inviteMember(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void inviteMember(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void revokeInvitation(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void revokeInvitation(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void acceptInvitation(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void acceptInvitation(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void denyInvitation(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void denyInvitation(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void validateRequest(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void validateRequest(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void declineRequest(Space space, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void declineRequest(String spaceId, String userId) throws SpaceException {
    throw new UnsupportedOperationException();

  }

  @Override
  public void registerSpaceLifeCycleListener(SpaceLifeCycleListener listener) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void unregisterSpaceLifeCycleListener(SpaceLifeCycleListener listener) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void setPortletsPrefsRequired(PortletPreferenceRequiredPlugin portletPrefsRequiredPlugin) {
    throw new UnsupportedOperationException();

  }

  @Override
  public String[] getPortletsPrefsRequired() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Space> getVisitedSpaces(String remoteId, String appId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSuperManager(String userId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MembershipEntry> getSuperManagersMemberships() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addSuperManagersMembership(String permissionExpression) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeSuperManagersMembership(String permissionExpression) {
    throw new UnsupportedOperationException();
  }

}
