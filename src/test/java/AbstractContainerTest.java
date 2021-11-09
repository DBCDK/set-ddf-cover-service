import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.setddfcover.CoverEntity;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
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


public class AbstractContainerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerTest.class);

    static final WireMockServer wireMockServer;
    protected static final GenericContainer setDDFCoverServiceContainer;
    protected static final GenericContainer recordServiceContainer;
    protected static final DBCPostgreSQLContainer setddfcoverDbContainer;
    protected static final DBCPostgreSQLContainer rawrepoDbContainer;
    protected static final DBCPostgreSQLContainer holdingsItemsDbContainer;

    protected static final String recordServiceBaseUrl;
    protected static final String setDDFCoverServiceBaseUrl;
    protected static final HttpClient httpClient;

    static {
        final Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        final Network network = Network.newNetwork();
        httpClient = HttpClient.create(client);

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());

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

        rawrepoDbContainer = new DBCPostgreSQLContainer("docker-io.dbc.dk/rawrepo-postgres-1.14-snapshot:master-5149");
        rawrepoDbContainer.start();
        rawrepoDbContainer.exposeHostPort();

        holdingsItemsDbContainer = new DBCPostgreSQLContainer("docker-os.dbc.dk/holdings-items-postgres-1.1.4:latest");
        holdingsItemsDbContainer.start();
        holdingsItemsDbContainer.exposeHostPort();

        recordServiceContainer = new GenericContainer("docker-io.dbc.dk/rawrepo-record-service:master-310")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER)).withNetwork(network)
                .withNetworkAliases("recordservice")
                .withEnv("INSTANCE", "it")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("VIPCORE_CACHE_AGE", "0")
                .withEnv("VIPCORE_ENDPOINT", "http://vipcore.iscrum-vip-extern-test.svc.cloud.dbc.dk")
                .withEnv("RAWREPO_URL", rawrepoDbContainer.getPayaraDockerJdbcUrl())
                .withEnv("HOLDINGS_URL", holdingsItemsDbContainer.getPayaraDockerJdbcUrl())
                .withEnv("DUMP_THREAD_COUNT", "8")
                .withEnv("DUMP_SLIZE_SIZE", "1000")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withClasspathResourceMapping(".", "/currentdir", BindMode.READ_ONLY)
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/api/status"))
                .withStartupTimeout(Duration.ofMinutes(2));
        recordServiceContainer.start();
        recordServiceBaseUrl = "http://" + recordServiceContainer.getNetworkAliases().get(0) + ":" +
                recordServiceContainer.getExposedPorts().get(0);

        setDDFCoverServiceContainer = new GenericContainer("docker-io.dbc.dk/set-ddf-cover-service:" + getDockerTag())
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withNetwork(network)
                .withNetworkAliases("ddfcover")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("LOG_FORMAT", "text")
                .withEnv("SET_DDF_COVER_DB", setddfcoverDbContainer.getPayaraDockerJdbcUrl())
                .withEnv("RAWREPO_RECORD_SERVICE_URL", recordServiceBaseUrl)
                .withEnv("OAUTH2_CLIENT_ID", "123456789")
                .withEnv("OAUTH2_CLIENT_SECRET", "abcdef")
                .withEnv("OAUTH2_INTROSPECTION_URL", "http://host.testcontainers.internal:" + wireMockServer.port())
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

    public List<CoverEntity> getCovers(String bibliographicRecordId) throws SQLException {
        final String query = "Select * from cover where bibliographicrecordid=?";
        final List<CoverEntity> result = new ArrayList<>();

        try (Connection connection = setddfcoverDbContainer.createConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, bibliographicRecordId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    CoverEntity coverEntity = new CoverEntity();
                    coverEntity.setId(resultSet.getInt(1));
                    coverEntity.setBibliographicRecordId(resultSet.getString(2));
                    coverEntity.setCoverExists(resultSet.getBoolean(3));
                    coverEntity.setPid(resultSet.getString(4));
                    coverEntity.setModified(resultSet.getDate(5));
                    result.add(coverEntity);
                }
            }
            return result;
        }
    }

    public void addBibliographicRecords(String bibliographicRecordId, String agencyId, boolean enrichment) throws SQLException {
        String mimetype;
        if ("870970".equals(agencyId)) {
            mimetype = "text/marcxchange";
        } else if ("191919".equals(agencyId)) {
            mimetype = "text/enrichment+marcxchange";
        } else {
            mimetype = enrichment ? "text/enrichment+marcxchange" : "text/marcxchange";
        }

        final String query = "INSERT INTO records (bibliographicRecordId, agencyid, deleted, mimetype,content, created, modified) VALUES (?, ?, false, ?, null, now(), now())";

        try (Connection connection = rawrepoDbContainer.createConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, bibliographicRecordId);
            stmt.setInt(2, Integer.parseInt(agencyId));
            stmt.setString(3, mimetype);

            stmt.execute();
        }
    }

}
