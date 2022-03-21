package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.validator.SupplementaryDataUpdateRequestValidator;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getevents.GetEventsOperation;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.SupplementaryDataUpdateOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("LinkedCaseController")
public class LinkedCaseControllerTest {

    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "Grant";
    private static final Boolean IGNORE_WARNING = true;
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();

    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private CreateEventOperation createEventOperation;
    @Mock
    private CreateCaseOperation createCaseOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private GetEventsOperation getEventsOperation;

    @Mock
    private SupplementaryDataUpdateOperation supplementaryDataUpdateOperation;

    @Mock
    private SupplementaryDataUpdateRequestValidator requestValidator;

    @InjectMocks
    private LinkedCaseController linkedCaseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDetails.getReference()).thenReturn(new Long(CASE_REFERENCE));

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));
        when(createEventOperation.createCaseEvent(CASE_REFERENCE, CASE_DATA_CONTENT)).thenReturn(caseDetails);
        when(createCaseOperation.createCaseDetails(CASE_TYPE_ID, CASE_DATA_CONTENT, IGNORE_WARNING))
            .thenReturn(caseDetails);
        List<AuditEvent> auditEvents = Lists.newArrayList(new AuditEvent(), new AuditEvent());
        when(getEventsOperation.getEvents(CASE_REFERENCE)).thenReturn(auditEvents);
    }

    @Nested
    @DisplayName("GET /getLinkedCase/{caseReference}")
    class GetCategoriesAndDocuments {

        @Test
        @DisplayName("should return 200 when case found")
        void linkedCaseFound() {
            // WHEN
            final ResponseEntity<Void> response = linkedCaseController.getLinkedCase(CASE_REFERENCE, null, null);

            // THEN
            assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("should return 200 when case found")
        void linkedCaseFoundWithOptionalParameters() {
            // WHEN
            final ResponseEntity<Void> response = linkedCaseController.getLinkedCase(CASE_REFERENCE, "1", "1");

            // THEN
            assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("should propagate CaseNotFoundException when case NOT found")
        void linkedCaseNotFound() {
            // GIVEN
            doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

            // WHEN
            final Throwable thrown = catchThrowable(() -> linkedCaseController.getLinkedCase(CASE_REFERENCE, "1", "1"));

            // THEN
            Assertions.assertThat(thrown)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage(String.format("No case found for reference: %s", CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void linkedCaseReferenceNotValid() {
            // GIVEN
            doReturn(FALSE).when(caseReferenceService).validateUID(CASE_REFERENCE);

            // WHEN
            final Throwable thrown = catchThrowable(() -> linkedCaseController.getLinkedCase(CASE_REFERENCE, "1", "1"));

            // THEN
            Assertions.assertThat(thrown)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Case ID is not valid");
        }

        @Test
        @DisplayName("should propagate BadRequestException when Start Record Number is Non Numeric")
        void linkedCaseStartRecordNumberIsNonNumeric() {
            // GIVEN
            doReturn(FALSE).when(caseReferenceService).validateUID(CASE_REFERENCE);

            // WHEN
            final Throwable thrown = catchThrowable(() -> linkedCaseController.getLinkedCase(CASE_REFERENCE, "A", null));

            // THEN
            Assertions.assertThat(thrown)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Case ID is not valid");
        }

        @Test
        @DisplayName("should propagate BadRequestException when Max Return Record Count is not valid")
        void linkedCaseMaxReturnRecordCountIsNonNumeric() {
            // GIVEN
            doReturn(FALSE).when(caseReferenceService).validateUID(CASE_REFERENCE);

            // WHEN
            final Throwable thrown = catchThrowable(() -> linkedCaseController.getLinkedCase(CASE_REFERENCE, null, "A"));

            // THEN
            Assertions.assertThat(thrown)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Case ID is not valid");
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            // GIVEN
            doThrow(RuntimeException.class).when(getCaseOperation).execute(CASE_REFERENCE);

            // WHEN
            final Throwable thrown = catchThrowable(() -> linkedCaseController.getLinkedCase(CASE_REFERENCE, "1", "1"));

            // THEN
            Assertions.assertThat(thrown)
                .isInstanceOf(Exception.class);
        }
    }
}
