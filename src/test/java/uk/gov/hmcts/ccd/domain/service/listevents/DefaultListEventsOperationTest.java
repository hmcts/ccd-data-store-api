package uk.gov.hmcts.ccd.domain.service.listevents;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class DefaultListEventsOperationTest {

    private static final Long CASE_ID = 123L;
    private static final List<AuditEvent> EVENTS = new ArrayList<>();
    @Mock
    private CaseAuditEventRepository auditEventRepository;

    private DefaultListEventsOperation listEventsOperation;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        doReturn(EVENTS).when(auditEventRepository).findByCase(caseDetails);

        listEventsOperation = new DefaultListEventsOperation(auditEventRepository);
    }

    @Test
    @DisplayName("should retrieve events from repository")
    void shouldDelegateCallToRepository() {
        final List<AuditEvent> events = listEventsOperation.execute(caseDetails);

        assertAll(
            () -> verify(auditEventRepository).findByCase(caseDetails),
            () -> assertThat(events, sameInstance(EVENTS))
        );
    }

}
