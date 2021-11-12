import dk.dbc.setddfcover.CoverEntity;
import dk.dbc.setddfcover.model.UpdateEvent;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class CoverEntityIT extends AbstractContainerTest {
    private static final String ENDPOINT = "/api/v1/events";

    @Test
    void testInvalidRequests() throws Exception {
        final String bibliographicRecordId = "testInvalidRequests";
        addBibliographicRecords(bibliographicRecordId, "870970", false);

        UpdateEvent dto = new UpdateEvent();

        // Empty dto
        assertThat("status code", postResponse(ENDPOINT, dto, "valid-token").getStatus(), is(400));

        dto.setBibliographicRecordId(bibliographicRecordId);
        dto.setCoverExists(true);
        Response response = postResponse(ENDPOINT, dto, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is the same", cover.isCoverExists(), is(true));
        assertThat("pid has value", cover.getPid(), is("870970-basis:" + bibliographicRecordId));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));
    }

    @Test
    void testUpdateExistingCover() throws Exception {
        final String bibliographicRecordId = "testUpdateExistingCover";
        addBibliographicRecords(bibliographicRecordId, "870970", false);

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setBibliographicRecordId(bibliographicRecordId);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is true", cover.isCoverExists(), is(true));
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("pid has value", cover.getPid(), is("870970-basis:" + bibliographicRecordId));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));

        // Update cover entity - set coverExists to false
        final UpdateEvent updateCover = new UpdateEvent();
        updateCover.setBibliographicRecordId(bibliographicRecordId);
        updateCover.setCoverExists(false);

        Response response2 = postResponse(ENDPOINT, updateCover, "valid-token");
        assertThat("status code", response2.getStatus(), is(200));

        List<CoverEntity> covers2 = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers2.size(), is(1));

        final CoverEntity cover2 = covers2.get(0);
        assertThat("bibliographicRecordId is the same", cover2.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is false", cover2.isCoverExists(), is(false));
        assertThat("id has value", cover2.getId(), is(notNullValue()));
        assertThat("pid has value", cover.getPid(), is("870970-basis:" + bibliographicRecordId));
        assertThat("modified has value", cover2.getModified(), is(notNullValue()));
    }

    @Test
    void testNonExistingBibliographicRecordId() {
        final String bibliographicRecordId = "testNonExistingBibliographicRecordId";

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setBibliographicRecordId(bibliographicRecordId);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        System.out.println(response.getEntity().toString());
        assertThat("status code", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat("message", response.readEntity(String.class), is("{\"cause\":\"No record found with bibliographicRecordId testNonExistingBibliographicRecordId\"}"));
    }

    @Test
    void testSingleLocalRecord() throws Exception {
        final String bibliographicRecordId = "testSingleLocalRecord";
        addBibliographicRecords(bibliographicRecordId, "706050", false);

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setBibliographicRecordId(bibliographicRecordId);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is true", cover.isCoverExists(), is(true));
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("pid has value", cover.getPid(), is("706050-katalog:" + bibliographicRecordId));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));

        // Update cover entity - set coverExists to false
        final UpdateEvent updateCover = new UpdateEvent();
        updateCover.setBibliographicRecordId(bibliographicRecordId);
        updateCover.setCoverExists(false);

        Response response2 = postResponse(ENDPOINT, updateCover, "valid-token");
        assertThat("status code", response2.getStatus(), is(200));

        List<CoverEntity> covers2 = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers2.size(), is(1));

        final CoverEntity cover2 = covers2.get(0);
        assertThat("bibliographicRecordId is the same", cover2.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is false", cover2.isCoverExists(), is(false));
        assertThat("id has value", cover2.getId(), is(notNullValue()));
        assertThat("pid has value", cover.getPid(), is("706050-katalog:" + bibliographicRecordId));
        assertThat("modified has value", cover2.getModified(), is(notNullValue()));
    }

    @Test
    void testMultipleLocalRecords() throws Exception {
        final String bibliographicRecordId = "testMultipleLocalRecords";
        addBibliographicRecords(bibliographicRecordId, "706050", false);
        addBibliographicRecords(bibliographicRecordId, "403020", false);

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setBibliographicRecordId(bibliographicRecordId);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers.size(), is(2));

        for (CoverEntity cover : covers) {
            assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
            assertThat("coverExists is true", cover.isCoverExists(), is(true));
            assertThat("id has value", cover.getId(), is(notNullValue()));
            assertThat("pid has value", cover.getPid(), anyOf(
                    is("706050-katalog:" + bibliographicRecordId),
                    is("403020-katalog:" + bibliographicRecordId)));
            assertThat("modified has value", cover.getModified(), is(notNullValue()));
        }

        // Update cover entity - set coverExists to false
        final UpdateEvent updateCover = new UpdateEvent();
        updateCover.setBibliographicRecordId(bibliographicRecordId);
        updateCover.setCoverExists(false);

        Response response2 = postResponse(ENDPOINT, updateCover, "valid-token");
        assertThat("status code", response2.getStatus(), is(200));

        List<CoverEntity> covers2 = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers2.size(), is(2));

        for (CoverEntity cover : covers2) {
            assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
            assertThat("coverExists is true", cover.isCoverExists(), is(false));
            assertThat("id has value", cover.getId(), is(notNullValue()));
            assertThat("pid has value", cover.getPid(), anyOf(
                    is("706050-katalog:" + bibliographicRecordId),
                    is("403020-katalog:" + bibliographicRecordId)));
            assertThat("modified has value", cover.getModified(), is(notNullValue()));
        }
    }

    @Test
    void testMarcXChangeAndEnrichments() throws Exception {
        final String bibliographicRecordId = "testMarcXChangeAndEnrichments";
        addBibliographicRecords(bibliographicRecordId, "870970", false);
        addBibliographicRecords(bibliographicRecordId, "191919", true);
        addBibliographicRecords(bibliographicRecordId, "706050", true);
        addBibliographicRecords(bibliographicRecordId, "403020", true);

        // Add new cover entity
        final UpdateEvent addCover = new UpdateEvent();
        addCover.setBibliographicRecordId(bibliographicRecordId);
        addCover.setCoverExists(true);

        Response response = postResponse(ENDPOINT, addCover, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers(bibliographicRecordId);
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is(bibliographicRecordId));
        assertThat("coverExists is true", cover.isCoverExists(), is(true));
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("pid has value", cover.getPid(), is("870970-basis:" + bibliographicRecordId));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));
    }

    @Test
    void testUnauthorized() {
        final String bibliographicRecordId = "testUnauthorized";

        // Add new cover entity
        final UpdateEvent cover = new UpdateEvent();
        cover.setBibliographicRecordId(bibliographicRecordId);
        cover.setCoverExists(true);

        Response responseInvalidToken = postResponse(ENDPOINT, cover, "invalid-token");
        assertThat("invalid token", responseInvalidToken.getStatus(), is(401));

        Response responseNoAuthorization = postResponse(ENDPOINT, cover);
        assertThat("no token", responseNoAuthorization.getStatus(), is(401));
    }

}
