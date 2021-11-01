package dk.dbc.setddfcover;

import dk.dbc.setddfcover.model.ServiceError;
import dk.dbc.setddfcover.model.UpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Stateless
@Path("api")
public class CoverResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverResource.class);

    @Inject
    @SetDDFCoverEntityManager
    EntityManager entityManager;

    @POST
    @Path("v1/events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"authenticated-user"})
    public Response events(UpdateEvent updateEvent) {
        try {
            LOGGER.debug("Received request: {}", updateEvent);

            if (updateEvent.getBibliographicRecordId() == null) {
                final ServiceError serviceError = new ServiceError();
                serviceError.setCause("Invalid request");

                return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
            }

            final TypedQuery<CoverEntity> query =
                    entityManager.createNamedQuery(CoverEntity.SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_NAME, CoverEntity.class);
            query.setParameter("bibliographicRecordId", updateEvent.getBibliographicRecordId());

            CoverEntity coverEntity;
            // getSingleResult could be used here, however that function throws exception if nothing is found
            final List<CoverEntity> coverEntities = query.getResultList();
            if (coverEntities.isEmpty()) {
                coverEntity = new CoverEntity();
                coverEntity.setBibliographicRecordId(updateEvent.getBibliographicRecordId());
                coverEntity.setCoverExists(updateEvent.isCoverExists());

                entityManager.persist(coverEntity);
            } else if (coverEntities.size() == 1) {
                coverEntity = coverEntities.get(0);
                coverEntity.setCoverExists(updateEvent.isCoverExists());

            } else {
                throw new Exception("More than one hit is not possible");
            }

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e.getCause());
            final ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        }
    }

}
