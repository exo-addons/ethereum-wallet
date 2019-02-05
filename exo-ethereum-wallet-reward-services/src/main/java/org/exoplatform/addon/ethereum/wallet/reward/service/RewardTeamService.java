/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.addon.ethereum.wallet.reward.service;

import static org.exoplatform.addon.ethereum.wallet.reward.service.utils.RewardUtils.fromDTO;
import static org.exoplatform.addon.ethereum.wallet.reward.service.utils.RewardUtils.toDTO;

import java.util.List;
import java.util.stream.Collectors;

import org.exoplatform.addon.ethereum.wallet.reward.dao.RewardTeamDAO;
import org.exoplatform.addon.ethereum.wallet.reward.entity.RewardTeamEntity;
import org.exoplatform.addon.ethereum.wallet.reward.model.RewardTeam;

/**
 * A storage service to save/load reward teams
 */
public class RewardTeamService {

  private RewardTeamDAO rewardTeamDAO;

  public RewardTeamService(RewardTeamDAO rewardTeamDAO) {
    this.rewardTeamDAO = rewardTeamDAO;
  }

  public List<RewardTeam> getTeams() {
    List<RewardTeamEntity> teamEntities = rewardTeamDAO.findAll();
    return teamEntities.stream().map(teamEntity -> toDTO(teamEntity)).collect(Collectors.toList());
  }

  public RewardTeam saveTeam(RewardTeam rewardTeam) {
    if (rewardTeam == null) {
      throw new IllegalArgumentException("Empty team to save");
    }
    RewardTeamEntity teamEntity = fromDTO(rewardTeam);
    if (teamEntity.getId() == null || teamEntity.getId() == 0) {
      teamEntity = rewardTeamDAO.create(teamEntity);
    } else {
      teamEntity = rewardTeamDAO.update(teamEntity);
    }
    return toDTO(rewardTeamDAO.find(teamEntity.getId()));
  }

  /**
   * Remove a reward Team/Pool by id
   * 
   * @param id
   * @return
   */
  public RewardTeam removeTeam(Long id) {
    if (id == null || id == 0) {
      throw new IllegalArgumentException("Team id is required");
    }
    RewardTeamEntity entity = rewardTeamDAO.find(id);
    if (entity != null) {
      rewardTeamDAO.delete(entity);
    }
    return toDTO(entity);
  }
}
