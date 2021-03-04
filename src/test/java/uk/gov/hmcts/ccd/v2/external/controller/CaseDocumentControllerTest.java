package uk.gov.hmcts.ccd.v2.external.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.GetCaseDocumentOperation;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDocumentResource;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("CaseDocumentController")
class CaseDocumentControllerTest {

    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_NAME = "Sample_document.txt";
    private static final String DOCUMENT_TYPE = "Document";

    @Mock
    private GetCaseDocumentOperation getCaseDocumentOperation;

    @InjectMocks
    private CaseDocumentController caseDocumentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
            .caseId(CASE_REFERENCE)
            .documentPermissions(DocumentPermissions.builder()
                .id(CASE_DOCUMENT_ID)
                .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
                .build())
            .build();
        when(getCaseDocumentOperation.getCaseDocumentMetadata(CASE_REFERENCE,CASE_DOCUMENT_ID))
            .thenReturn(caseDocumentMetadata);
    }

    @Test
    @DisplayName("should return 200 when case found")
    void caseFound() {
        final ResponseEntity<CaseDocumentResource> response = caseDocumentController
            .getCaseDocumentMetadata(CASE_REFERENCE,CASE_DOCUMENT_ID);

        assertAll(
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertThat(response.getBody().getDocumentMetadata().getCaseId(), is(CASE_REFERENCE)),
            () -> assertThat(response.getBody().getDocumentMetadata().getDocumentPermissions(),
                allOf(hasProperty("id", is(CASE_DOCUMENT_ID)))),
            () -> assertThat(response.getBody().getDocumentMetadata().getDocumentPermissions().getPermissions(),
                    hasSize(2)),
            () -> assertThat(response.getBody().getDocumentMetadata().getDocumentPermissions().getPermissions(),
                hasItems(Permission.READ, Permission.UPDATE))
        );
    }


    @Test
    @DisplayName("should propagate exception")
    void shouldPropagateExceptionWhenThrown() {
        when(caseDocumentController.getCaseDocumentMetadata(CASE_REFERENCE,CASE_DOCUMENT_ID))
            .thenThrow(RuntimeException.class);

        assertThrows(
            RuntimeException.class, () -> caseDocumentController
                .getCaseDocumentMetadata(CASE_REFERENCE,CASE_DOCUMENT_ID));
    }

}
