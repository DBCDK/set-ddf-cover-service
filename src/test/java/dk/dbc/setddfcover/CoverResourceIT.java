package dk.dbc.setddfcover;

import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.setddfcover.model.UpdateEvent;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class CoverResourceIT extends AbstractContainerTest {
    private static final String ENDPOINT = "/api/v1/events";

    @Test
    void testInvalidRequests() throws Exception {
        final String pid = "870970-basis:testInvalidRequests";

        UpdateEvent dto = new UpdateEvent();

        // Empty dto
        Response response = postResponse(ENDPOINT, dto, "valid-token");
        assertThat("status code", response.getStatus(), is(400));
        assertThat("message", response.readEntity(String.class), is("{\"cause\":\"Invalid request - missing pid value\"}"));

        // Invalid pid
        dto.setPid("testInvalidRequests");
        response = postResponse(ENDPOINT, dto, "valid-token");
        assertThat("status code", response.getStatus(), is(400));
        assertThat("message", response.readEntity(String.class), is("{\"cause\":\"Invalid request - wrong pid format\"}"));

        // Acceptable dto
        dto = new UpdateEvent();
        dto.setPid(pid);
        dto.setCoverExists(true);
        response = postResponse(ENDPOINT, dto, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(pid);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("pid is the same", cover.getPid(), is(pid));
        assertThat("coverExists is the same", cover.isCoverExists(), is(true));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));
    }

    @Test
    void testInvalidMimeType() throws Exception {
        final String pid = "870970-basis:testInvalidRequests";

        UpdateEvent dto = new UpdateEvent();

        // Empty dto
        HttpPost httpPost = new HttpPost(httpClient)
                .withBaseUrl(setDDFCoverServiceBaseUrl)
                .withPathElements(ENDPOINT)
                .withHeader("Accept", MediaType.APPLICATION_XML)
                .withData(dto, MediaType.APPLICATION_JSON);
        try (Response response = httpClient.execute(httpPost)) {
            assertThat("status code", response.getStatus(), is(406));
            String body = response.readEntity(String.class);
            assertThat("has expected message", body, containsString("<message>HTTP 406 Not Acceptable</message>"));
            assertThat("contains an errorId", body, containsString("<errorId>"));

        }
    }

    @Test
    void testInvalidUrl() throws Exception {
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(setDDFCoverServiceBaseUrl)
                .withPathElements("/Nonsense")
                .withHeader("Accept", MediaType.APPLICATION_JSON);
        try (Response response = httpClient.execute(httpGet)) {
            assertThat("status code", response.getStatus(), is(404));
            String body = response.readEntity(String.class);
            assertThat("has expected message", body, containsString("\"message\":\"HTTP 404 Not Found\""));
            assertThat("contains an errorId", body, containsString("\"errorId\":"));
        }
    }

    @Test
    void testUpdateExistingCover() throws Exception {
        final String pid = "870970-basis:testUpdateExistingCover";

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setPid(pid);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(pid);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("pid is the same", cover.getPid(), is(pid));
        assertThat("coverExists is true", cover.isCoverExists(), is(true));
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));

        // Update cover entity - set coverExists to false
        final UpdateEvent updateCover = new UpdateEvent();
        updateCover.setPid(pid);
        updateCover.setCoverExists(false);

        Response response2 = postResponse(ENDPOINT, updateCover, "valid-token");
        assertThat("status code", response2.getStatus(), is(200));

        List<CoverEntity> covers2 = getCovers(pid);
        assertThat("Amount of found covers", covers2.size(), is(1));

        final CoverEntity cover2 = covers2.get(0);
        assertThat("pid is the same", cover2.getPid(), is(pid));
        assertThat("coverExists is false", cover2.isCoverExists(), is(false));
        assertThat("id has value", cover2.getId(), is(notNullValue()));
        assertThat("modified has value", cover2.getModified(), is(notNullValue()));
    }

    @Test
    void testUnauthorized() {
        final String pid = "870970-basis:testUnauthorized";

        // Add new cover entity
        final UpdateEvent cover = new UpdateEvent();
        cover.setPid(pid);
        cover.setCoverExists(true);

        Response responseInvalidToken = postResponse(ENDPOINT, cover, "invalid-token");
        assertThat("invalid token", responseInvalidToken.getStatus(), is(401));

        Response responseNoAuthorization = postResponse(ENDPOINT, cover);
        assertThat("no token", responseNoAuthorization.getStatus(), is(401));
    }

}
