package dk.dbc.setddfcover;

import dk.dbc.setddfcover.model.ServiceError;
import dk.dbc.setddfcover.model.UpdateEvent;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("api")
public class SetDDFCoverResource {

    @POST
    @Path("v1/events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response events(UpdateEvent updateEvent) {
        try {
            return Response.ok().build();
        } catch (Exception e) {
            final ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        }
    }

}
