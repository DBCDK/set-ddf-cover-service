package dk.dbc.setddfcover;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class SetDDFCoverResource {

    // Simple dummy endpoint to being with
    @GET
    @Path("v1/hello")
    @Produces(MediaType.APPLICATION_JSON)
    public Response helloWorld() {
        return Response.ok().build();
    }

}
