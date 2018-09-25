package uk.gov.hmcts.ccd.v2.external.controller;

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
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("CaseController")
class CaseControllerTest {
    private static final String CASE_REFERENCE = "1234123412341238";

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseDetails caseDetails;

    @InjectMocks
    private CaseController caseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(caseDetails.getReference()).thenReturn(new Long(CASE_REFERENCE));

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));
    }

    @Nested
    @DisplayName("GET /cases/{caseId}")
    class GetCaseForId {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<CaseResource> response = caseController.getCase(CASE_REFERENCE);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertThat(response.getBody().getReference(), is(CASE_REFERENCE))
            );
        }

        @Test
        @DisplayName("should return 404 when case NOT found")
        void caseNotFound() {
            when(getCaseOperation.execute(CASE_REFERENCE)).thenReturn(Optional.empty());

            assertThrows(CaseNotFoundException.class,
                         () -> caseController.getCase(CASE_REFERENCE));
        }

        @Test
        @DisplayName("should return 400 when case reference not valid")
        void caseReferenceNotValid() {
            when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

            assertThrows(BadRequestException.class,
                         () -> caseController.getCase(CASE_REFERENCE));
        }
    }
}
