package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class CaseAuditEventMapperTest {
    private static final Long CASE_DATA_ID = 101111L;
    private static final String CASE_TYPE_ID = "121212";
    private static final String EVENT_NAME = "eventName";
    private static final String STATE_NAME = "stateName";
    private static final Integer CASE_TYPE_VERSION = 111;
    private static final String FIRST_NAME = "ALI";
    private static final String LAST_NAME = "ALIF";
    private static final String URL = "http://www.yahoo.com";
    private static final String DESCRIPTION = "description";
    private static final String USER_ID = "USER_ID";
    private static final String USER_ID_PROXY_BY = "USER_ID_PROXY_BY";
    private static final String USER_ID_PROXY_BY_LAST_NAME = "USER_ID_PROXY_BY_LAST_NAME";
    private static final String USER_ID_PROXY_BY_FIRST_NAME = "USER_ID_PROXY_BY_FIRST_NAME";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();

    @Test
    @DisplayName("Should map model to entity ")
    public void modelTo() {
        CaseAuditEventMapper caseAuditEventMapper = new CaseAuditEventMapper();
        AuditEvent auditEvent = getAuditEvent();
        CaseAuditEventEntity result = caseAuditEventMapper.modelToEntity(auditEvent);

        assertEquals(USER_ID, result.getUserId());
        assertEquals(CASE_DATA_ID, result.getCaseDataId());
        assertEquals(CASE_TYPE_ID, result.getCaseTypeId());
        assertEquals(EVENT_NAME, result.getEventName());
        assertEquals(STATE_NAME, result.getStateName());
        assertEquals(CASE_TYPE_VERSION, result.getCaseTypeVersion());
        assertEquals(DATE_TIME, result.getCreatedDate());
        assertEquals(LAST_NAME, result.getUserLastName());
        assertEquals(FIRST_NAME, result.getUserFirstName());
        assertEquals(DESCRIPTION, result.getSignificantItemEntity().getDescription());
        assertEquals(URL, result.getSignificantItemEntity().getUrl().toString());
        assertEquals(SignificantItemType.DOCUMENT, result.getSignificantItemEntity().getType());
    }

    @Test
    @DisplayName("Should map model to entity with proxy by user details")
    public void modelToProxyBy() {
        CaseAuditEventMapper caseAuditEventMapper = new CaseAuditEventMapper();
        AuditEvent auditEvent = getAuditEventProxyUserDetails();
        CaseAuditEventEntity result = caseAuditEventMapper.modelToEntity(auditEvent);

        assertEquals(USER_ID, result.getUserId());
        assertEquals(CASE_DATA_ID, result.getCaseDataId());
        assertEquals(CASE_TYPE_ID, result.getCaseTypeId());
        assertEquals(EVENT_NAME, result.getEventName());
        assertEquals(STATE_NAME, result.getStateName());
        assertEquals(CASE_TYPE_VERSION, result.getCaseTypeVersion());
        assertEquals(DATE_TIME, result.getCreatedDate());
        assertEquals(LAST_NAME, result.getUserLastName());
        assertEquals(FIRST_NAME, result.getUserFirstName());
        assertEquals(DESCRIPTION, result.getSignificantItemEntity().getDescription());
        assertEquals(URL, result.getSignificantItemEntity().getUrl().toString());
        assertEquals(SignificantItemType.DOCUMENT, result.getSignificantItemEntity().getType());

        assertEquals(USER_ID_PROXY_BY, result.getProxiedBy());
        assertEquals(USER_ID_PROXY_BY_FIRST_NAME, result.getProxiedByFirstName());
        assertEquals(USER_ID_PROXY_BY_LAST_NAME, result.getProxiedByLastName());
    }

    private AuditEvent getAuditEvent() {
        final AuditEvent auditEvent = new AuditEvent();
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setUrl(URL);
        auditEvent.setUserId(USER_ID);
        significantItem.setDescription(DESCRIPTION);
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        auditEvent.setSignificantItem(significantItem);
        auditEvent.setSecurityClassification(SecurityClassification.PUBLIC);
        auditEvent.setCaseDataId(CASE_DATA_ID.toString());
        auditEvent.setCaseTypeId(CASE_TYPE_ID);
        auditEvent.setEventName(EVENT_NAME);
        auditEvent.setStateName(STATE_NAME);
        auditEvent.setCaseTypeVersion(CASE_TYPE_VERSION);
        auditEvent.setCreatedDate(DATE_TIME);
        auditEvent.setUserFirstName(FIRST_NAME);
        auditEvent.setUserLastName(LAST_NAME);

        return auditEvent;
    }

    private AuditEvent getAuditEventProxyUserDetails() {
        final AuditEvent auditEvent = getAuditEvent();
        auditEvent.setProxiedBy(USER_ID_PROXY_BY);
        auditEvent.setProxiedByFirstName(USER_ID_PROXY_BY_FIRST_NAME);
        auditEvent.setProxiedByLastName(USER_ID_PROXY_BY_LAST_NAME);

        return auditEvent;
    }
}
