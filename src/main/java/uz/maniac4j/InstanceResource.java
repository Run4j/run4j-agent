package uz.maniac4j;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/instance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InstanceResource {

    @Inject
    InstanceService instanceService;

    @POST
    @Path("/start")
    public Response startInstance(StartInstanceRequest request) {
        try {
            var result = instanceService.startInstance(request);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/stop")
    public Response stopInstance(StopInstanceRequest request) {
        try {
            var result = instanceService.stopInstance(request);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/status")
    public Response getStatus(@QueryParam("id") String id) {
        try {
            var result = instanceService.getStatus(id);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
} 