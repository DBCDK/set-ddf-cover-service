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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
@Path("api")
public class CoverResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverResource.class);
    private static final Pattern PID_PATTERN = Pattern.compile("(\\d{6})[-]([a-z]+)[:](.*)");

    @Inject
    @SetDDFCoverEntityManager
    EntityManager entityManager;

    @Inject
    SolrDocStoreDAO solrDocStoreDAO;

    @POST
    @Path("v1/events")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"authenticated-user"})
    public Response events(UpdateEvent updateEvent) {
        try {
            LOGGER.debug("Received request: {}", updateEvent);

            if (updateEvent.getPid() == null) {
                final ServiceError serviceError = new ServiceError();
                serviceError.setCause("Invalid request - missing pid value");

                return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
            }

            final String pid = updateEvent.getPid();
            final boolean coverExists = updateEvent.isCoverExists();

            final Matcher matcher = PID_PATTERN.matcher(pid);

            if (!matcher.find()) {
                final ServiceError serviceError = new ServiceError();
                serviceError.setCause("Invalid request - wrong pid format");

                return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
            }

            final String bibliographicRecordId =  matcher.group(3);
            final String agencyId = matcher.group(1);

            final TypedQuery<CoverEntity> query =
                    entityManager.createNamedQuery(CoverEntity.SELECT_FROM_COVER_BY_PID_NAME, CoverEntity.class);
            query.setParameter("pid", pid);

            // getSingleResult could be used here, however that function throws exception if nothing is found
            final List<CoverEntity> coverEntities = query.getResultList();
            if (coverEntities.isEmpty()) {
                final CoverEntity coverEntity = new CoverEntity();
                coverEntity.setPid(pid);
                coverEntity.setCoverExists(coverExists);

                entityManager.persist(coverEntity);

                if (coverExists) {
                    solrDocStoreDAO.setHasDDFCover(bibliographicRecordId, agencyId);
                }
            } else {
                final CoverEntity coverEntity = coverEntities.get(0);
                coverEntity.setCoverExists(updateEvent.isCoverExists());

                if (updateEvent.isCoverExists()) {
                    solrDocStoreDAO.setHasDDFCover(bibliographicRecordId, agencyId);
                } else {
                    solrDocStoreDAO.removeHasDDFCover(bibliographicRecordId, agencyId);
                }
            }

            return Response.ok().build();
        } catch (SolrDocStoreException e) {
            LOGGER.error("Got error from solr-doc-store: {}", e.getMessage(), e.getCause());
            final ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e.getCause());
            final ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        }
    }

}
