package dk.dbc.setddfcover;

import dk.dbc.rawrepo.record.RecordServiceConnector;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("api")
public class CoverResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverResource.class);

    @Inject
    RecordServiceConnector recordServiceConnector;

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

            final String bibliographicRecordId = updateEvent.getBibliographicRecordId();

            final TypedQuery<CoverEntity> query =
                    entityManager.createNamedQuery(CoverEntity.SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_NAME, CoverEntity.class);
            query.setParameter("bibliographicRecordId", bibliographicRecordId);

            // getSingleResult could be used here, however that function throws exception if nothing is found
            final List<CoverEntity> coverEntities = query.getResultList();
            if (coverEntities.isEmpty()) {
                // New cover
                final Integer[] agenciesForRecordArray = recordServiceConnector.getAllAgenciesForBibliographicRecordId(bibliographicRecordId);
                final List<Integer> agenciesForRecordList = Arrays.stream(agenciesForRecordArray).collect(Collectors.toList());

                if (agenciesForRecordList.isEmpty()) {
                    // We don't have a record with the given faust. Return status code 400.
                    final String message = String.format("No record found with bibliographicRecordId %s", bibliographicRecordId);
                    LOGGER.error(message);

                    final ServiceError serviceError = new ServiceError();
                    serviceError.setCause(message);

                    return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
                }

                // If there is a 870970 record then the cover must be associated with that record
                if (agenciesForRecordList.contains(870970)) {
                    final String pid = String.format("870970-basis:%s", bibliographicRecordId);

                    addCover(bibliographicRecordId, updateEvent.isCoverExists(), pid);
                } else {
                    // If no 870970 record then set cover on all agencies which has that record
                    for (Integer agencyId : agenciesForRecordList) {
                        final String pid = String.format("%s-katalog:%s", agencyId, bibliographicRecordId);

                        addCover(bibliographicRecordId, updateEvent.isCoverExists(), pid);
                    }
                }
            } else {
                for (CoverEntity coverEntity : coverEntities) {
                    coverEntity.setCoverExists(updateEvent.isCoverExists());

                    // TODO Call opensearch
                }
            }

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e.getCause());
            final ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        }
    }

    private void addCover(String bibliographicRecordId, boolean coverExists, String pid) {
        final CoverEntity coverEntity = new CoverEntity();
        coverEntity.setBibliographicRecordId(bibliographicRecordId);
        coverEntity.setCoverExists(coverExists);
        coverEntity.setPid(pid);

        entityManager.persist(coverEntity);

        // TODO Call opensearch
    }

}
