package com.devsquad.shared.web;

import io.smallrye.health.SmallRyeHealthReporter;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/actuator/health/readiness")
public class ReadinessCompatibilityResource {

  @Inject SmallRyeHealthReporter healthReporter;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response readiness() {
    var readiness = healthReporter.getReadiness();
    var status = readiness.isDown() ? Response.Status.SERVICE_UNAVAILABLE : Response.Status.OK;
    return Response.status(status).entity(readiness.getPayload().toString()).build();
  }
}
