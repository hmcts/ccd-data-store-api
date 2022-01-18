package uk.gov.hmcts.ccd;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AuditCaseRemoteConfigurationTest {

    private static final String LAU_REMOTE_CASE_AUDIT_ENABLED_NAME = "lauRemoteCaseAuditEnabled";
    private static final String LAU_REMOTE_CASE_AUDIT_URL_NAME = "lauRemoteCaseAuditUrl";
    private static final String LAU_REMOTE_CASE_AUDIT_ACTION_PATH_NAME = "lauRemoteCaseAuditActionPath";
    private static final String LAU_REMOTE_CASE_AUDIT_SEARCH_PATH_NAME = "lauRemoteCaseAuditSearchPath";

    protected AuditCaseRemoteConfiguration auditCaseRemoteConfiguration = new AuditCaseRemoteConfiguration();

    @Before
    @BeforeEach
    public void initMock() throws IOException {
        ReflectionTestUtils.setField(auditCaseRemoteConfiguration,
            LAU_REMOTE_CASE_AUDIT_ENABLED_NAME, true);
        ReflectionTestUtils.setField(auditCaseRemoteConfiguration,
            LAU_REMOTE_CASE_AUDIT_URL_NAME, "http://localhost:5000");
        ReflectionTestUtils.setField(auditCaseRemoteConfiguration,
            LAU_REMOTE_CASE_AUDIT_ACTION_PATH_NAME, "/testCaseAction");
        ReflectionTestUtils.setField(auditCaseRemoteConfiguration,
            LAU_REMOTE_CASE_AUDIT_SEARCH_PATH_NAME, "/testCaseSearch");
    }

    @Test
    @DisplayName("should get correct value for isEnabled")
    void shouldGetCorrectValueForIsEnabled() {
        assertThat(auditCaseRemoteConfiguration.isEnabled(), is(equalTo(true)));
    }

    @Test
    @DisplayName("should get correct value for getCaseActionAuditUrl")
    void shouldGetCorrectValueForGetCaseActionAuditUrl() {
        assertThat(auditCaseRemoteConfiguration.getCaseActionAuditUrl(),
            is(equalTo("http://localhost:5000/testCaseAction")));
    }

    @Test
    @DisplayName("should get correct value for getCaseSearchAuditUrl")
    void shouldGetCorrectValueForGetCaseSearchAuditUrl() {
        assertThat(auditCaseRemoteConfiguration.getCaseSearchAuditUrl(),
            is(equalTo("http://localhost:5000/testCaseSearch")));
    }

}
