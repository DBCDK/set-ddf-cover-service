package dk.dbc.setddfcover;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SolrDocStoreDAOTest {
    private static WireMockServer wireMockServer;
    private static String wireMockHost;

    static SolrDocStoreDAO dao;

    @BeforeAll
    static void startWireMockServer() {
        wireMockServer = new WireMockServer(options().dynamicPort()
                .dynamicHttpsPort());
        wireMockServer.start();
        wireMockHost = "http://localhost:" + wireMockServer.port();
        configureFor("localhost", wireMockServer.port());
    }

    @BeforeAll
    static void setConnector() {
        dao = new SolrDocStoreDAO(wireMockHost);
    }

    @AfterAll
    static void stopWireMockServer() {
        wireMockServer.stop();
    }

    @Test
    void testSetCover() {
        assertDoesNotThrow(() -> dao.setHasDDFCover("25912233", "710100"));
    }

    @Test
    void testRemoveCover() {
        assertDoesNotThrow(() -> dao.removeHasDDFCover("25912233", "710100"));
    }
}
