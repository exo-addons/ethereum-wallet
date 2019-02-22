package org.exoplatform.addon.ethereum.wallet.task.rest;

import java.util.Collections;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.addon.ethereum.wallet.task.model.WalletAdminTask;
import org.exoplatform.addon.ethereum.wallet.task.service.WalletTaskService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

/**
 * This class provide a REST endpoint to manage wallet admin tasks
 */
@Path("/wallet/api/task")
@RolesAllowed({ "rewarding", "administrators" })
public class WalletTaskREST implements ResourceContainer {
  private static final Log  LOG = ExoLogger.getLogger(WalletTaskREST.class);

  private WalletTaskService walletTaskService;

  public WalletTaskREST(WalletTaskService walletTaskService) {
    this.walletTaskService = walletTaskService;
  }

  /**
   * @return list of wallet admin tasks in JSON format
   */
  @GET
  @Path("list")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listTasks() {
    ConversationState currentState = ConversationState.getCurrent();
    if (currentState == null || currentState.getIdentity() == null || currentState.getIdentity().getUserId() == null
        || !currentState.getIdentity().getRoles().contains("rewarding")) {
      return Response.ok(Collections.emptyList()).build();
    }
    try {
      Set<WalletAdminTask> tasks = walletTaskService.listTasks(currentState.getIdentity().getUserId());
      return Response.ok(tasks).build();
    } catch (Exception e) {
      LOG.warn("Error getting listing tasks", e);
      JSONObject object = new JSONObject();
      try {
        object.append("error", e.getMessage());
      } catch (JSONException e1) {
        // Nothing to do
      }
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity(object.toString()).build();
    }
  }
}
