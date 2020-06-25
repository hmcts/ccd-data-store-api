package uk.gov.hmcts.ccd.v2.external.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataRequest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SupplementaryDataResourceTest {

    private static final String CASE_REF_ID = "12345667";

    @Test
    @DisplayName("should copy supplementary data")
    public void shouldCopyCaseAssignedUserRoleContent() {
        SupplementaryDataRequest request = new SupplementaryDataRequest();
        SupplementaryData response = new SupplementaryData();
        SupplementaryDataResource resource = new SupplementaryDataResource(CASE_REF_ID, request, response);
        assertAll(
            () -> assertThat(resource.getSupplementaryData(), is(response))
        );
    }

}
