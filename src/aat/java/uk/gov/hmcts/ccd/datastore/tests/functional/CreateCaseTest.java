package uk.gov.hmcts.ccd.datastore.tests.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.datastore.tests.AATHelper;
import uk.gov.hmcts.ccd.datastore.tests.BaseTest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

class CreateCaseTest extends BaseTest {

    private static final String EVENT_CREATE = "CREATE";
    private static final String JURISDICTION = "AUTOTEST1";
    private static final String CASE_TYPE = "AAT";

    protected CreateCaseTest(AATHelper aat) {
        super(aat);
    }

    @Test
    @DisplayName("Create a new empty case")
    void shouldCreateACase() {
        aat.getCcdHelper()
           .createCase(asAutoTestCaseworker(), JURISDICTION, CASE_TYPE, EVENT_CREATE, createEmptyCase())
           .then()
                .statusCode(201);
    }

    private CaseDataContent createEmptyCase() {
        final Event event = new Event();
        event.setEventId(EVENT_CREATE);

        final CaseDataContent caseData = new CaseDataContent();
        caseData.setEvent(event);

        return caseData;
    }

}
