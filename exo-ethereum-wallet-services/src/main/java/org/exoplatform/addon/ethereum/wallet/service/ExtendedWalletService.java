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
package org.exoplatform.addon.ethereum.wallet.service;

import static org.exoplatform.addon.ethereum.wallet.service.utils.Utils.*;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;

/**
 * A storage service to save/load extra information used by other addons
 */
public class ExtendedWalletService {

  private SettingService settingService;

  private Double         tokensPerKudos;

  public ExtendedWalletService(SettingService settingService) {
    this.settingService = settingService;
  }

  /**
   * @return the amount of token per kudos
   */
  public double getTokensPerKudos() {
    if (tokensPerKudos == null) {
      SettingValue<?> value = settingService.get(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, EXT_TOKENS_PER_KUDOS_KEY_NAME);
      this.tokensPerKudos = value == null ? 0 : Double.parseDouble(value.getValue().toString());
    }
    return tokensPerKudos;
  }

  /**
   * Sets the amount of tokens per kudos
   * 
   * @param tokensPerKudos
   */
  public void saveTokensPerKudos(double tokensPerKudos) {
    settingService.set(EXT_WALLET_CONTEXT, EXT_WALLET_SCOPE, EXT_TOKENS_PER_KUDOS_KEY_NAME, SettingValue.create(tokensPerKudos));
    this.tokensPerKudos = null;
  }

}
