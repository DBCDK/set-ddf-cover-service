package dk.dbc.setddfcover;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpPut;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Stateless
public class SolrDocStoreDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrDocStoreDAO.class);
    private static final JSONBContext jsonbContext = new JSONBContext();

    private static final String API_PATH = "/api/resource/hasDDFCoverUrl/%s:%s";

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 404 || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    private FailSafeHttpClient failSafeHttpClient;

    // Mandatory empty constructor as there is defined a non-empty constructor
    public SolrDocStoreDAO() {

    }

    // Constructor used for testing
    public SolrDocStoreDAO(String solrDocStoreUrl) {
        this.solrDocStoreUrl = solrDocStoreUrl;

        final Client client = HttpClient.newClient();
        failSafeHttpClient = FailSafeHttpClient.create(client, RETRY_POLICY);
    }

    @Inject
    @ConfigProperty(name = "SOLR_DOC_STORE_URL")
    private String solrDocStoreUrl;

    @PostConstruct
    public void postConstruct() {
        final Client client = HttpClient.newClient();
        failSafeHttpClient = FailSafeHttpClient.create(client, RETRY_POLICY);
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    public void setHasDDFCover(String bibliographicRecordId, String agencyId) throws JSONBException, SolrDocStoreException {
        final HasDDFCoverDTO dto = new HasDDFCoverDTO();

        final HttpPut httpPost = new HttpPut(failSafeHttpClient)
                .withBaseUrl(solrDocStoreUrl)
                .withPathElements(String.format(API_PATH, agencyId, bibliographicRecordId))
                .withData(jsonbContext.marshall(dto), "application/json")
                .withHeader("Accept", "application/json")
                .withHeader("Content-type", "application/json");

        final Response response = httpPost.execute();
        final Response.Status actualStatus =
                Response.Status.fromStatusCode(response.getStatus());

        if (actualStatus != Response.Status.OK) {
            LOGGER.error("Got status code {} from solr-doc-store when calling PUT {} with message '{}'",
                    actualStatus, API_PATH, response.readEntity(String.class));
            throw new SolrDocStoreException("Got response status " + actualStatus.getStatusCode() + " from SolrDocStore");
        }
    }

    public void removeHasDDFCover(String bibliographicRecordId, String agencyId) throws SolrDocStoreException {
        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(solrDocStoreUrl)
                .withPathElements(String.format(API_PATH, agencyId, bibliographicRecordId))
                .withHeader("Accept", "application/json");

        final Response response = httpDelete.execute();
        final Response.Status actualStatus =
                Response.Status.fromStatusCode(response.getStatus());

        if (actualStatus != Response.Status.OK) {
            LOGGER.error("Got status code {} from solr-doc-store when calling DELETE {} with message '{}'",
                    actualStatus, API_PATH, response.readEntity(String.class));
            throw new SolrDocStoreException("Got response status " + actualStatus.getStatusCode() + " from SolrDocStore");
        }
    }

    // PUT requires this body
    private static class HasDDFCoverDTO {
        public boolean isHas() {
            return true;
        }
    }

}
