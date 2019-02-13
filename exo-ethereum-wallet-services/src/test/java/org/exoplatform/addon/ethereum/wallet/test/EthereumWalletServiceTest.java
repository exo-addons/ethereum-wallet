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
package org.exoplatform.addon.ethereum.wallet.test;

import static org.exoplatform.addon.ethereum.wallet.utils.Utils.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import org.exoplatform.addon.ethereum.wallet.model.GlobalSettings;
import org.exoplatform.addon.ethereum.wallet.service.EthereumWalletService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;

public class EthereumWalletServiceTest {

  @BeforeClass
  public static void testStart() {
    PortalContainer.getInstance();
  }

  @Test
  public void testGetDefaultSettings() {
    GlobalSettings settings = getService().getSettings();
    assertNotNull("default settings are null", settings);

    assertNotNull("default settings access permissions list is null", settings.getAccessPermission());
    assertEquals("default settings access permission should be empty", settings.getAccessPermission().length(), 0);

    assertNotNull("default settings gas is null", settings.getDefaultGas());
    assertEquals("default settings: wrong value for gas", settings.getDefaultGas().longValue(), 150000L);

    assertNull("default settings: contract ABI should be null", settings.getContractAbi());

    assertNull("default settings: contract BIN should be null", settings.getContractBin());

    assertNull("default settings: funds holder should be null", settings.getFundsHolder());

    assertNotNull("default settings: principal contrat admin name shouldn't be null", settings.getPrincipalContractAdminName());
    assertEquals("default settings: wrong value for principal contrat admin name",
                 settings.getPrincipalContractAdminName(),
                 PRINCIPAL_CONTRACT_ADMIN_NAME);

    assertNull("default settings: principal contrat admin address should be null", settings.getPrincipalContractAdminAddress());

    assertNotNull("default settings: blockchain provider URL shouldn't be null", settings.getProviderURL());

    assertNotNull("default settings: blockchain websocket provider URL shouldn't be null", settings.getWebsocketProviderURL());

    assertNull("default settings: funds holder type should be null", settings.getFundsHolderType());

    assertNull("default settings: initial funds message should be null", settings.getInitialFundsRequestMessage());

    assertNotNull("default settings: max gas price shouldn't be null", settings.getMaxGasPrice());
    assertEquals("default settings: wrong value for max gas price", settings.getMaxGasPrice().longValue(), 15000000000L);

    assertNotNull("default settings: min gas price shouldn't be null", settings.getMinGasPrice());
    assertEquals("default settings: wrong value for min gas price", settings.getMinGasPrice().longValue(), 4000000000L);

    assertNotNull("default settings: normal gas price shouldn't be null", settings.getNormalGasPrice());
    assertEquals("default settings: wrong value for normal gas price", settings.getNormalGasPrice().longValue(), 8000000000L);

    assertNull("default settings: initial funds map should be null", settings.getInitialFunds());

    assertNotNull("default settings: network id shouldn't be null", settings.getDefaultNetworkId());
    assertEquals("default settings: wrong value for network id", settings.getDefaultNetworkId().intValue(), 3);

    assertNull("default settings: overview accounts set should be null", settings.getDefaultOverviewAccounts());

    assertNotNull("default settings: data version shouldn't be null", settings.getDataVersion());
    assertEquals("default settings: wrong value for data version", settings.getDataVersion().intValue(), GLOBAL_DATA_VERSION);
  }

  private EthereumWalletService getService() {
    return CommonsUtils.getService(EthereumWalletService.class);
  }
}
