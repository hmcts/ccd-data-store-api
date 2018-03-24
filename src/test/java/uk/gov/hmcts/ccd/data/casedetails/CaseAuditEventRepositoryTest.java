package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;


import javax.inject.Inject;
import javax.sql.DataSource;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Transactional
public class CaseAuditEventRepositoryTest extends BaseTest {

    @Autowired
    CaseAuditEventRepository classUnderTest;

    @Inject
    protected DataSource db;

    private static final Long CASE_DATA_ID = 69L;

    @Test
    @DisplayName("should return the earliest event as the 'create event'")
    public void databaseContainsThreeEventsForCaseDetails_getCreateEventCalled_EarliestEventReturned() {

        setUpCaseData(CASE_DATA_ID);

        String createEventSummary = "The Create Event";

        setUpCaseEvent(CASE_DATA_ID, createEventSummary, "2017-09-28 08:46:16.258");
        setUpCaseEvent(CASE_DATA_ID, "First Event after Create Event", "2017-09-28 08:46:16.259");
        setUpCaseEvent(CASE_DATA_ID, "Second Event after Create Event", "2017-09-28 08:46:16.260");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_DATA_ID);

        Optional<AuditEvent> createEventOptional = classUnderTest.getCreateEvent(caseDetails);

        assertTrue(createEventOptional.isPresent());
        assertEquals(createEventSummary, createEventOptional.get().getSummary());

    }

    @Test
    @DisplayName("should return an empty optional if no events for CaseDetails")
    public void databaseContainsNoEventsForCaseDetails_getCreateEventCalled_EmptyOptionalReturned() {

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(CASE_DATA_ID);

        assertFalse(classUnderTest.getCreateEvent(caseDetails).isPresent());

    }

    private void setUpCaseData(Long caseDataId) {
        new JdbcTemplate(db).update(
            String.format(
                "INSERT INTO CASE_DATA "
                    + "(ID, CASE_TYPE_ID, JURISDICTION, STATE, DATA, REFERENCE, SECURITY_CLASSIFICATION) "
                + "VALUES "
                    + "(%s, 'CASE_TYPE_ID', 'JURISDICTION', 'STATE', '{}', '6969', 'PUBLIC')",
                caseDataId
            )
        );
    }

    private void setUpCaseEvent(Long caseDataId, String summary, String timestamp) {
        new JdbcTemplate(db).update(
            String.format(
                "INSERT INTO CASE_EVENT "
                    + "(EVENT_ID, CREATED_DATE, EVENT_NAME, SUMMARY, USER_ID, USER_FIRST_NAME, USER_LAST_NAME, CASE_DATA_ID, "
                        + "CASE_TYPE_ID, CASE_TYPE_VERSION, STATE_ID, STATE_NAME, DATA, SECURITY_CLASSIFICATION) "
                + "VALUES "
                    + "('EVENT_ID', %s, 'EVENT_NAME', '%s', 696969, 'USER_FIRST_NAME', 'USER_LAST_NAME', %s, "
                        + "'CASE_TYPE_ID', '1', 'STATE_ID', 'STATE_NAME', '{}', 'PUBLIC')",
                String.format("'%s'::timestamp",timestamp),
                summary,
                caseDataId
            )
        );
    }

}
