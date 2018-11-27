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
package org.exoplatform.addon.ethereum.wallet.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.exoplatform.addon.ethereum.wallet.service.ExtendedWalletService;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * This class provide a REST endpoint to save/load extra Wallet data related to
 * other addons
 */
@Path("/wallet/api/ext")
public class ExtraWalletSettingsREST implements ResourceContainer {

  private ExtendedWalletService extendedWalletService;

  public ExtraWalletSettingsREST(ExtendedWalletService extendedWalletService) {
    this.extendedWalletService = extendedWalletService;
  }

  /**
   * Get tokens amount per Kudos
   * 
   * @return
   */
  @Path("getTokensPerKudos")
  @GET
  @RolesAllowed("administrators")
  public Response getTokensPerKudos() {
    return Response.ok(String.valueOf(extendedWalletService.getTokensPerKudos())).build();
  }

  /**
   * Save tokens amount per Kudos setting
   * 
   * @param tokensPerKudos
   */
  @POST
  @Path("saveTokensPerKudos")
  @RolesAllowed("administrators")
  public Response saveTokensPerKudos(@FormParam("tokensPerKudos") double tokensPerKudos) {
    extendedWalletService.saveTokensPerKudos(tokensPerKudos);
    return Response.ok().build();
  }
}
