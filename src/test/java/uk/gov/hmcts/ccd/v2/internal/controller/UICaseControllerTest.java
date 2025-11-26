package uk.gov.hmcts.ccd.v2.internal.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseHistoryViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@DisplayName("UICaseController")
class UICaseControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final Long EVENT_ID = 100L;

    @Mock
    private GetCaseViewOperation getCaseViewOperation;

    @Mock
    private GetCaseHistoryViewOperation getCaseHistoryViewOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseView caseView;

    @Mock
    private CaseViewEvent caseViewEvent;

    @Mock
    private CaseHistoryView caseHistoryView;

    @InjectMocks
    private uk.gov.hmcts.ccd.v2.internal.controller.UICaseController caseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(caseView.getCaseId()).thenReturn(CASE_REFERENCE);
        when(caseHistoryView.getCaseId()).thenReturn(CASE_REFERENCE);
        when(caseHistoryView.getEvent()).thenReturn(caseViewEvent);
        when(caseViewEvent.getId()).thenReturn(EVENT_ID);

        when(caseDataAccessControl.generateAccessMetadata(CASE_REFERENCE)).thenReturn(new CaseAccessMetadata());
        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(getCaseViewOperation.execute(CASE_REFERENCE)).thenReturn(caseView);
        when(getCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID)).thenReturn(caseHistoryView);
    }

    @Nested
    @DisplayName("GET /internal/cases/{caseId}")
    class GetCaseForId {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<CaseViewResource> response = caseController.getCaseView(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.getCaseView(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCaseViewOperation.execute(CASE_REFERENCE)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.getCaseView(CASE_REFERENCE));
        }
    }

    @Nested
    @DisplayName("GET /internal/cases/{caseId}/events/{eventId}")
    class GetEventForCaseAndEventId {

        @Test
        @DisplayName("should return 200 when event found")
        void caseFound() {
            final ResponseEntity<CaseHistoryViewResource> response =
                    caseController.getCaseHistoryView(CASE_REFERENCE, EVENT_ID.toString());

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getCaseId(), is(CASE_REFERENCE)),
                () -> assertThat(response.getBody().getEvent().getId(), is(EVENT_ID))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.getCaseHistoryView(CASE_REFERENCE, EVENT_ID.toString()));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCaseHistoryViewOperation.execute(CASE_REFERENCE, EVENT_ID)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.getCaseHistoryView(CASE_REFERENCE, EVENT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("GET /internal/cases/{caseId}/access-metadata")
    class GetCaseAccessMetadataForId {

        @Test
        @DisplayName("should return 200 when metadata found")
        void accessMetadataFound() {
            CaseAccessMetadata caseAccessMetadata = new CaseAccessMetadata();
            caseAccessMetadata.setAccessGrants(List.of(GrantType.STANDARD, GrantType.SPECIFIC, GrantType.CHALLENGED));
            caseAccessMetadata.setAccessProcess(AccessProcess.NONE);
            doReturn(caseAccessMetadata).when(caseDataAccessControl).generateAccessMetadata(CASE_REFERENCE);

            final ResponseEntity<CaseAccessMetadata> response
                = caseController.getCaseAccessMetadata(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getAccessGrants(), hasItem(GrantType.STANDARD)),
                () -> assertThat(response.getBody().getAccessProcess(), is(AccessProcess.NONE))
            );
        }

        @Test
        @DisplayName("should propagate BadRequestException when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                () -> caseController.getCaseAccessMetadata(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(caseDataAccessControl.generateAccessMetadata(CASE_REFERENCE)).thenThrow(RuntimeException.class);

            assertThrows(Exception.class,
                () -> caseController.getCaseAccessMetadata(CASE_REFERENCE));
        }
    }
}
