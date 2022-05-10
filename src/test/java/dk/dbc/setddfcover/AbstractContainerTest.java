package dk.dbc.setddfcover;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


public class AbstractContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerTest.class);

    static final WireMockServer wireMockServer;
    protected static final GenericContainer setDDFCoverServiceContainer;
    protected static final DBCPostgreSQLContainer setddfcoverDbContainer;

    protected static final String setDDFCoverServiceBaseUrl;
    protected static final HttpClient httpClient;

    static {
        final Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        httpClient = HttpClient.create(client);

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());

        // solr-doc-store PUT
        wireMockServer.stubFor(put(urlMatching("/api/resource/hasDDFCoverUrl/[0-9]{6}:test.*"))
                .withRequestBody(equalToJson("{\"has\":true}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true,\"text\":\"Success\"}")));
        // solr-doc-store DELETE
        wireMockServer.stubFor(delete(urlMatching("/api/resource/hasDDFCoverUrl/[0-9]{6}:test.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true,\"text\":\"Success\"}")));
        // Add responses for auth introspect endpoint.
        wireMockServer.stubFor(WireMock.requestMatching(request ->
                        MatchResult.of(
                                request.queryParameter("access_token").isPresent() &&
                                        request.queryParameter("access_token").containsValue("valid-token")
                        ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":true}")));
        wireMockServer.stubFor(WireMock.requestMatching(request ->
                        MatchResult.of(
                                request.queryParameter("access_token").isPresent() &&
                                        request.queryParameter("access_token").containsValue("invalid-token")
                        ))
                .willReturn(ResponseDefinitionBuilder
                        .responseDefinition()
                        .withStatus(200)
                        .withBody("{\"active\":false}")));

        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        Testcontainers.exposeHostPorts(wireMockServer.port());
        LOGGER.info("Wiremock server at port:{}", wireMockServer.port());

        setddfcoverDbContainer = new DBCPostgreSQLContainer();
        setddfcoverDbContainer.start();
        setddfcoverDbContainer.exposeHostPort();

        setDDFCoverServiceContainer = new GenericContainer("docker-io.dbc.dk/set-ddf-cover-service:" + getDockerTag())
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("SET_DDF_COVER_DB", setddfcoverDbContainer.getPayaraDockerJdbcUrl())
                .withEnv("OAUTH2_CLIENT_ID", "123456789")
                .withEnv("OAUTH2_CLIENT_SECRET", "abcdef")
                .withEnv("OAUTH2_INTROSPECTION_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("SOLR_DOC_STORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/health/ready"))
                .withStartupTimeout(Duration.ofMinutes(2));
        setDDFCoverServiceContainer.start();
        setDDFCoverServiceBaseUrl = "http://" + setDDFCoverServiceContainer.getContainerIpAddress() +
                ":" + setDDFCoverServiceContainer.getMappedPort(8080);
    }

    static String getDockerTag() {
        final String build_number = System.getenv("BUILD_NUMBER");
        if (build_number != null) {
            return String.join("-", System.getenv("BRANCH_NAME"), build_number);
        }
        return "devel";
    }

    public <T> Response postResponse(String path, T body) {
        return postResponse(path, body, null);
    }

    public <T> Response postResponse(String path, T body, String authToken) {
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(setDDFCoverServiceBaseUrl)
                .withPathElements(path)
                .withData(body, "application/json");
        if (authToken != null) {
            httpPost.getHeaders().put("Authorization", "Bearer " + authToken);
        }
        return httpClient.execute(httpPost);
    }

    public List<CoverEntity> getCovers(String pid) throws SQLException {
        final String query = "Select id, pid, coverexists, modified from cover where pid=?";
        final List<CoverEntity> result = new ArrayList<>();

        try (Connection connection = setddfcoverDbContainer.createConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, pid);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    CoverEntity coverEntity = new CoverEntity();
                    coverEntity.setId(resultSet.getInt(1));
                    coverEntity.setPid(resultSet.getString(2));
                    coverEntity.setCoverExists(resultSet.getBoolean(3));
                    coverEntity.setModified(resultSet.getDate(4));
                    result.add(coverEntity);
                }
            }
            return result;
        }
    }

}
