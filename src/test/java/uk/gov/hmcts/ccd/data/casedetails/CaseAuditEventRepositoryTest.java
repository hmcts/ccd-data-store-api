package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class CaseAuditEventRepositoryTest extends BaseTest {

    @Autowired
    CaseAuditEventRepository classUnderTest;

    @Inject
    protected DataSource db;

    private static final Long CASE_DATA_ID = 69L;
    private static final Long CASE_REFERENCE = 6969L;
    private static final String USER_ID = "123456";

    @Test
    @DisplayName("should return the earliest event as the 'create event'")
    public void databaseContainsThreeEventsForCaseDetails_getCreateEventCalled_EarliestEventReturned() {

        setUpCaseData(CASE_DATA_ID, CASE_REFERENCE);

        String createEventSummary = "The Create Event";

        setUpCaseEvent(CASE_DATA_ID, createEventSummary, "2017-09-28 08:46:16.258");
        setUpCaseEvent(CASE_DATA_ID, "First Event after Create Event", "2017-09-28 08:46:16.259");
        setUpCaseEvent(CASE_DATA_ID, "Second Event after Create Event", "2017-09-28 08:46:16.260");

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(CASE_DATA_ID));

        Optional<AuditEvent> createEventOptional = classUnderTest.getCreateEvent(caseDetails);

        assertTrue(createEventOptional.isPresent());
        assertEquals(createEventSummary, createEventOptional.get().getSummary());
    }

    @Test
    @DisplayName("should return case events for a case reference")
    public void shouldReturnCaseEventsForCaseReference() {

        setUpCaseData(CASE_DATA_ID, CASE_REFERENCE);

        String createEventSummary = "The Create Event";

        setUpCaseEvent(CASE_DATA_ID, createEventSummary, "2017-09-28 08:46:16.258");
        setUpCaseEvent(CASE_DATA_ID, "First Event after Create Event", "2017-09-28 08:46:16.259");
        setUpCaseEvent(CASE_DATA_ID, "Second Event after Create Event", "2017-09-28 08:46:16.260");

        List<AuditEvent> result = classUnderTest.findByCaseReference(CASE_REFERENCE, USER_ID, UserAuthorisation.AccessLevel.ALL);
        assertEquals(3, result.size());
        // Events should be ordered with creation event last since oldest.
        assertEquals(createEventSummary, result.get(2).getSummary());
    }

    @Test
    @DisplayName("findByCaseReferenceGranted returns only cases granted to the user")
    public void shouldNotReturnCaseEventsForUnauthorisedUser() {

        setUpCaseData(CASE_DATA_ID, CASE_REFERENCE);
        setUpCaseEvent(CASE_DATA_ID, "create case 1", "2017-09-28 08:46:16.258");

        Long SECOND_CASE_REFERENCE = CASE_REFERENCE + 1;
        setUpCaseData(CASE_DATA_ID + 1, SECOND_CASE_REFERENCE);
        setUpCaseEvent(CASE_DATA_ID + 1, "create case 1", "2017-09-28 08:46:16.258");

        grantUserAccessToCase(USER_ID, CASE_DATA_ID);
        List<AuditEvent> result = classUnderTest.findByCaseReference(CASE_REFERENCE, USER_ID, UserAuthorisation.AccessLevel.GRANTED);
        assertEquals(1, result.size());
        assertEquals(result.get(0).getSummary(), "create case 1");

        result = classUnderTest.findByCaseReference(SECOND_CASE_REFERENCE, USER_ID, UserAuthorisation.AccessLevel.GRANTED);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("should return an empty optional if no events for CaseDetails")
    public void databaseContainsNoEventsForCaseDetails_getCreateEventCalled_EmptyOptionalReturned() {

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setId(String.valueOf(CASE_DATA_ID));

        assertFalse(classUnderTest.getCreateEvent(caseDetails).isPresent());
    }

    @Test
    @DisplayName("should return audit event for the event id")
    public void shouldReturnAuditEventForEventId() {
        String createEventSummary = "The Create Event";

        setUpCaseData(100L, CASE_REFERENCE);
        Long eventId = setUpCaseEvent(100L, "The Create Event", "2017-09-28 08:46:16.258");

        Optional<AuditEvent> createEventOptional = classUnderTest.findByEventId(eventId);

        assertTrue(createEventOptional.isPresent());
        assertEquals(createEventSummary, createEventOptional.get().getSummary());
    }

    @Test
    @DisplayName("should throw exception when audit event is not found")
    public void shouldThrowExceptionWhenAuditEventNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> classUnderTest.findByEventId(10000L));
    }

    private void setUpCaseData(Long caseDataId, Long caseReference) {
        new JdbcTemplate(db).update(
            String.format(
                "INSERT INTO CASE_DATA "
                    + "(ID, CASE_TYPE_ID, JURISDICTION, STATE, DATA, REFERENCE, SECURITY_CLASSIFICATION) "
                + "VALUES "
                    + "(%s, 'CASE_TYPE_ID', 'JURISDICTION', 'STATE', '{}', %s, 'PUBLIC')",
                caseDataId,
                caseReference
            )
        );
    }

    private void grantUserAccessToCase(String userId, Long caseDataId) {
        Map<String, Object> params = new HashMap<>();
        params.put("CASE_DATA_ID", caseDataId);
        params.put("USER_ID", userId);
        params.put("CASE_ROLE", "[CREATOR]");

        new SimpleJdbcInsert(db)
            .withTableName("CASE_USERS")
            .execute(params);
    }

    private Long setUpCaseEvent(Long caseDataId, String summary, String timestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("EVENT_ID", "EVENT_ID");
        params.put("CREATED_DATE", timestamp);
        params.put("EVENT_NAME", "EVENT_NAME");
        params.put("SUMMARY", summary);
        params.put("USER_ID", 696969);
        params.put("USER_FIRST_NAME", "USER_FIRST_NAME");
        params.put("USER_LAST_NAME", "USER_LAST_NAME");
        params.put("CASE_DATA_ID", caseDataId);
        params.put("CASE_TYPE_ID", "CASE_TYPE_ID");
        params.put("CASE_TYPE_VERSION", "1");
        params.put("STATE_ID", "STATE_ID");
        params.put("STATE_NAME", "STATE_NAME");
        params.put("DATA", "{}");
        params.put("SECURITY_CLASSIFICATION", "PUBLIC");

        return new SimpleJdbcInsert(db)
            .withTableName("CASE_EVENT")
            .usingGeneratedKeyColumns("id")
            .executeAndReturnKey(params).longValue();
    }

}
