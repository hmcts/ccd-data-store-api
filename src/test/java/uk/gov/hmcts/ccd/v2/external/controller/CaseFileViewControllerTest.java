package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.documentdata.DocumentDataRequest;
import uk.gov.hmcts.ccd.domain.model.casefileview.CategoriesAndDocuments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casefileview.CategoriesAndDocumentsService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CaseFileViewControllerTest extends TestFixtures {

    @Mock
    private GetCaseOperation getCaseOperation;

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CategoriesAndDocumentsService categoriesAndDocumentsService;

    @InjectMocks
    private CaseFileViewController underTest;

    @Test
    @DisplayName("should return 200 when case found")
    void caseFound() {
        // GIVEN
        final CaseDetails caseDetails = TestFixtures.buildCaseDetails(emptyMap());
        final CategoriesAndDocuments categoriesAndDocuments =
            new CategoriesAndDocuments(VERSION_NUMBER, emptyList(), emptyList());
        doReturn(TRUE).when(caseReferenceService).validateUID(CASE_REFERENCE);
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(categoriesAndDocuments).when(categoriesAndDocumentsService)
            .getCategoriesAndDocuments(anyInt(), anyString(), anyMap());

        // WHEN
        final ResponseEntity<CategoriesAndDocuments> responseEntity =
            underTest.getCategoriesAndDocuments(CASE_REFERENCE);

        // THEN
        assertThat(responseEntity)
            .isNotNull()
            .satisfies(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
            });
    }

    @Test
    @DisplayName("should propagate CaseNotFoundException when case NOT found")
    void caseNotFound() {
        // GIVEN
        doReturn(TRUE).when(caseReferenceService).validateUID(CASE_REFERENCE);
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.getCategoriesAndDocuments(CASE_REFERENCE));

        // THEN
        assertThat(thrown)
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage(String.format("No case found for reference: %s", CASE_REFERENCE));
    }

    @Test
    @DisplayName("should propagate BadRequestException when case reference not valid")
    void caseReferenceNotValid() {
        // GIVEN
        doReturn(FALSE).when(caseReferenceService).validateUID(CASE_REFERENCE);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.getCategoriesAndDocuments(CASE_REFERENCE));

        // THEN
        assertThat(thrown)
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Case ID is not valid");
    }

    @Test
    @DisplayName("should propagate exception")
    void shouldPropagateExceptionWhenThrown() {
        // GIVEN
        doReturn(TRUE).when(caseReferenceService).validateUID(CASE_REFERENCE);
        doThrow(RuntimeException.class).when(getCaseOperation).execute(CASE_REFERENCE);

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.getCategoriesAndDocuments(CASE_REFERENCE));

        // THEN
        assertThat(thrown)
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("update document category")
    void testShouldUpdateDocumentCategory() {
        // GIVEN
        final CaseDetails caseDetails = TestFixtures.buildCaseDetails(emptyMap());
        final CategoriesAndDocuments categoriesAndDocuments =
            new CategoriesAndDocuments(VERSION_NUMBER, emptyList(), emptyList());
        final DocumentDataRequest request = new DocumentDataRequest("path.document", VERSION_NUMBER, "cat-1");

        doReturn(caseDetails).when(createEventOperation)
            .createCaseSystemEvent(eq(CASE_REFERENCE), anyInt(), anyString(), anyString());
        doReturn(categoriesAndDocuments).when(categoriesAndDocumentsService)
            .getCategoriesAndDocuments(anyInt(), anyString(), anyMap());

        // WHEN
        final ResponseEntity<CategoriesAndDocuments> responseEntity =
            underTest.updateDocumentField(CASE_REFERENCE, request);

        // THEN
        assertThat(responseEntity)
            .isNotNull()
            .satisfies(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
            });
    }

}
