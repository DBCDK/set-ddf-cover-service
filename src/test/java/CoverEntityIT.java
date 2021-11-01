import dk.dbc.setddfcover.CoverEntity;
import dk.dbc.setddfcover.model.UpdateEvent;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

class CoverEntityIT extends AbstractContainerTest {
    private static final String ENDPOINT = "/api/v1/events";

    @Test
    void testInvalidRequests() throws Exception{
        UpdateEvent dto = new UpdateEvent();

        // Empty dto
        assertThat("status code", postResponse(ENDPOINT, dto, "valid-token").getStatus(), is(400));

        dto.setBibliographicRecordId("11111111");
        dto.setCoverExists(true);
        Response response = postResponse(ENDPOINT, dto, "valid-token");
        assertThat("status code", response.getStatus(), is(200));

        List<CoverEntity> covers = getCovers("11111111");
        assertThat("Amount of found covers", covers.size(), is(1));

        final CoverEntity cover = covers.get(0);
        assertThat("bibliographicRecordId is the same", cover.getBibliographicRecordId(), is("11111111"));
        assertThat("coverExists is the same", cover.isCoverExists(), is(true));
        assertThat("id has value", cover.getId(), is(notNullValue()));
        assertThat("modified has value", cover.getModified(), is(notNullValue()));
    }

    @Test
    void testUpdateExistingCover() throws Exception{
        final String bibliographicRecordId = "22222222";

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
        assertThat("modified has value", cover2.getModified(), is(notNullValue()));
    }

    @Test
    void testUnauthorized() {
        final String bibliographicRecordId = "33333333";

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
