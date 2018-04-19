package uk.gov.hmcts.ccd.domain.service.listevents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class DefaultListEventsOperationTest {

    private static final Long CASE_ID = 123L;
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "CaseTypeId";
    private final static String CASE_REFERENCE = "999999";
    private static final List<AuditEvent> EVENTS = new ArrayList<>();
    @Mock
    private CaseAuditEventRepository auditEventRepository;
    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private UIDService uidService;

    private DefaultListEventsOperation listEventsOperation;
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = new CaseDetails();
        caseDetails.setId(CASE_ID);

        doReturn(EVENTS).when(auditEventRepository).findByCase(caseDetails);

        listEventsOperation = new DefaultListEventsOperation(auditEventRepository, getCaseOperation, uidService);
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

    @Test
    @DisplayName("should find case details and retrieve events from repository")
    void shouldFindCaseDetailsAndDelegateCallToRepository() {
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);

        final List<AuditEvent> events = listEventsOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(auditEventRepository).findByCase(caseDetails),
            () -> assertThat(events, sameInstance(EVENTS))
        );
    }

    @Test
    @DisplayName("should return bad request exception when case reference invalid")
    void shouldThrowBadRequestExceptionWhenCaseDetailsCannotBeFound() {
        doReturn(false).when(uidService).validateUID(CASE_REFERENCE);

        assertThrows(BadRequestException.class, () -> listEventsOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE));
    }

    @Test
    @DisplayName("should return resource not found exception when case details cannot be found")
    void shouldReturnErrorWhenCaseDetailsCannotBeFound() {
        doReturn(true).when(uidService).validateUID(CASE_REFERENCE);
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

        assertThrows(ResourceNotFoundException.class, () -> listEventsOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE));
    }

}
