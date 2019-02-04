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

import static org.exoplatform.addon.ethereum.wallet.reward.service.utils.RewardUtils.REWARD_CONTEXT;
import static org.exoplatform.addon.ethereum.wallet.reward.service.utils.RewardUtils.REWARD_SCOPE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.reward.model.RewardTransaction;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;

/**
 * A storage service to save/load reward transactions
 */
public class RewardTransactionService {

  private SettingService settingService;

  public RewardTransactionService(SettingService settingService) {
    this.settingService = settingService;
  }

  /**
   * @param networkId
   * @param periodType
   * @param startDateInSeconds
   * @param walletRewardType
   * @return
   */
  public List<JSONObject> getPeriodRewardTransactions(Long networkId,
                                                      String periodType,
                                                      long startDateInSeconds,
                                                      String walletRewardType) {
    String periodTransactionsParamName = getPeriodTransactionsParamName(periodType, startDateInSeconds, walletRewardType);
    SettingValue<?> periodTransactionsValue =
                                            settingService.get(REWARD_CONTEXT, REWARD_SCOPE, periodTransactionsParamName);

    String periodTransactionsString = periodTransactionsValue == null ? "" : periodTransactionsValue.getValue().toString();

    String[] periodTransactionsArray = periodTransactionsString.isEmpty() ? new String[0] : periodTransactionsString.split(",");
    return Arrays.stream(periodTransactionsArray).map(transaction -> {
      RewardTransaction rewardTransaction = RewardTransaction.fromStoredValue(transaction);
      rewardTransaction.setNetworkId(networkId);
      return rewardTransaction.toJSONObject();
    }).collect(Collectors.toList());
  }

  /**
   * Save reward transaction
   * 
   * @param rewardTransaction
   */
  public void savePeriodRewardTransaction(RewardTransaction rewardTransaction) {
    if (rewardTransaction == null) {
      throw new IllegalArgumentException("rewardTransaction parameter is mandatory");
    }
    if (rewardTransaction.getNetworkId() == 0) {
      throw new IllegalArgumentException("transaction NetworkId parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getHash())) {
      throw new IllegalArgumentException("transaction hash parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getPeriodType())) {
      throw new IllegalArgumentException("transaction PeriodType parameter is mandatory");
    }
    if (rewardTransaction.getStartDateInSeconds() == 0) {
      throw new IllegalArgumentException("transaction 'period start date' parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getReceiverType())) {
      throw new IllegalArgumentException("transaction ReceiverType parameter is mandatory");
    }
    if (StringUtils.isBlank(rewardTransaction.getReceiverId())) {
      throw new IllegalArgumentException("transaction ReceiverId parameter is mandatory");
    }

    String rewardPeriodTransactionsParamName = getPeriodTransactionsParamName(rewardTransaction.getPeriodType(),
                                                                              rewardTransaction.getStartDateInSeconds(),
                                                                              rewardTransaction.getWalletRewardType());
    SettingValue<?> periodTransactionsValue = settingService.get(REWARD_CONTEXT,
                                                                 REWARD_SCOPE,
                                                                 rewardPeriodTransactionsParamName);
    String rewardPeriodTransactionsString = periodTransactionsValue == null ? "" : periodTransactionsValue.getValue().toString();

    if (!rewardPeriodTransactionsString.contains(rewardTransaction.getHash())) {
      String contentToPrepend = rewardTransaction.getToStoreValue();
      rewardPeriodTransactionsString = rewardPeriodTransactionsString.isEmpty() ? contentToPrepend
                                                                                : contentToPrepend + ","
                                                                                    + rewardPeriodTransactionsString;
      settingService.set(REWARD_CONTEXT,
                         REWARD_SCOPE,
                         rewardPeriodTransactionsParamName,
                         SettingValue.create(rewardPeriodTransactionsString));
    }
  }

  private String getPeriodTransactionsParamName(String periodType, long startDateInSeconds, String walletRewardType) {
    return walletRewardType + periodType + startDateInSeconds;
  }
}
