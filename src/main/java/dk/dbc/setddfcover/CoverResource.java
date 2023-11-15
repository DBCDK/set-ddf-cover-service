package dk.dbc.setddfcover;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.setddfcover.model.ServiceError;
import dk.dbc.setddfcover.model.UpdateEvent;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
@Path("api")
public class CoverResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverResource.class);
    private static final Pattern PID_PATTERN = Pattern.compile("(\\d{6})-([a-z]+):(.*)");
    @PersistenceContext(unitName = "pg_set_ddf_cover_PU")
    EntityManager entityManager;

    @Resource
    SessionContext ctx;

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
                ServiceError serviceError = new ServiceError();
                serviceError.setCause("Invalid request - missing pid value");
                return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
            }
            return updateCover(updateEvent);
        } finally {
            LOGGER.info("/api/v1/events with params {\"pid\": \"{}\", \"coverExists\": {}}", updateEvent.getPid(), updateEvent.isCoverExists());
        }
    }

    private Response updateCover(UpdateEvent updateEvent) {
        String pid = updateEvent.getPid();
        boolean coverExists = updateEvent.isCoverExists();
        try {
            Matcher matcher = PID_PATTERN.matcher(pid);
            if (!matcher.find()) {
                ServiceError serviceError = new ServiceError();
                serviceError.setCause("Invalid request - wrong pid format");
                return Response.status(Response.Status.BAD_REQUEST).entity(serviceError).build();
            }

            String bibliographicRecordId = matcher.group(3);
            String agencyId = matcher.group(1);

            TypedQuery<CoverEntity> query = entityManager.createNamedQuery(CoverEntity.SELECT_FROM_COVER_BY_PID_NAME, CoverEntity.class);
            query.setParameter("pid", pid);
            List<CoverEntity> coverEntities = query.getResultList();
            if (coverEntities.isEmpty()) {
                try {
                    ctx.getBusinessObject(CoverResource.class).createCover(pid, coverExists, bibliographicRecordId, agencyId);
                    return Response.ok().build();
                } catch (Exception e) {
                    LOGGER.info("Got an exception while creation cover for pid {} will try to look up an existing cover a second time", pid);
                    coverEntities = query.getResultList();
                    if(coverEntities.isEmpty()) throw new IllegalStateException("Unable to create cover for pid " + pid, e);
                }
            }
            CoverEntity coverEntity = coverEntities.get(0);
            coverEntity.setCoverExists(updateEvent.isCoverExists());
            if (updateEvent.isCoverExists()) {
                solrDocStoreDAO.setHasDDFCover(bibliographicRecordId, agencyId);
            } else {
                solrDocStoreDAO.removeHasDDFCover(bibliographicRecordId, agencyId);
            }
            return Response.ok().build();
        } catch (SolrDocStoreException e) {
            LOGGER.error("Got error from solr-doc-store while updating pid {}", updateEvent.getPid(), e);
            ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        } catch (Exception e) {
            LOGGER.error("Got an exception while updating pid: {}", updateEvent.getPid(), e);
            ServiceError serviceError = new ServiceError();
            serviceError.setCause("Internal error");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serviceError).build();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createCover(String pid, boolean coverExists, String bibliographicRecordId, String agencyId) throws
            SolrDocStoreException, JSONBException {
        CoverEntity coverEntity = new CoverEntity();
        entityManager.persist(coverEntity);
        coverEntity.setPid(pid);
        coverEntity.setCoverExists(coverExists);
        if (coverExists) {
            solrDocStoreDAO.setHasDDFCover(bibliographicRecordId, agencyId);
        }
    }

}
