package uk.gov.hmcts.ccd.v2.external.resource;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SupplementaryDataResourceTest {

    @Test
    @DisplayName("should copy supplementary data")
    public void shouldCopyCaseAssignedUserRoleContent() {
        Map<String, Object> responseMap = new HashMap<>();
        SupplementaryData response = new SupplementaryData(responseMap);
        SupplementaryDataResource resource = new SupplementaryDataResource(response);
        assertAll(
            () -> assertThat(resource.getResponse(), is(responseMap))
        );
    }

}
