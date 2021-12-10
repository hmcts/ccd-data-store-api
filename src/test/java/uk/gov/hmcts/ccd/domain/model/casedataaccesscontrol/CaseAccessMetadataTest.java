package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.BASIC;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.CHALLENGED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.EXCLUDED;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;

class CaseAccessMetadataTest {

    private CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();

    @Test
    void testGetAccessGrantsString() {
        caseAccessMetadata.setAccessGrants(List.of(CHALLENGED, BASIC, STANDARD, EXCLUDED));

        String expectedSortedValue = String.join(",", BASIC.name(),
            CHALLENGED.name(), EXCLUDED.name(), STANDARD.name());
        assertEquals(expectedSortedValue, caseAccessMetadata.getAccessGrantsString());
    }

    @Test
    void testGetAccessProcessString() {
        caseAccessMetadata.setAccessProcess(AccessProcess.NONE);

        assertEquals("NONE", caseAccessMetadata.getAccessProcessString());
    }
}
