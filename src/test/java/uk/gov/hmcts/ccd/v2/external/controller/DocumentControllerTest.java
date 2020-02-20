package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.resource.DocumentsResource;

import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@DisplayName("DocumentController")
class DocumentControllerTest {

    private static final String CASE_REFERENCE = "1234123412341238";
    private static final CaseDataContent CASE_DATA_CONTENT = newCaseDataContent().build();

    @Mock
    private DocumentsOperation documentsOperation;

    @Mock
    private UIDService caseReferenceService;

    private List<Document> documents;

    @InjectMocks
    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        Document document = new Document();
        document.setName("name1");
        document.setDescription("desc1");
        document.setType("type1");
        document.setUrl("url1");
        documents = Lists.newArrayList(document);

        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(TRUE);
        when(documentsOperation.getPrintableDocumentsForCase(CASE_REFERENCE)).thenReturn(documents);
    }

    @Test
    @DisplayName("should return 200 when case found")
    void caseFound() {
        final ResponseEntity<DocumentsResource> response = documentController.getDocuments(CASE_REFERENCE);

        assertAll(
            () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
            () -> assertThat(response.getBody().getDocumentResources(), hasSize(1)),
            () -> assertThat(response.getBody().getDocumentResources(), hasItems(allOf(hasProperty("name", is("name1")),
                                                                                 hasProperty("description", is("desc1")),
                                                                                 hasProperty("type", is("type1")),
                                                                                 hasProperty("url", is("url1")))))
        );
    }

    @Test
    @DisplayName("should propagate BadRequestException when case reference not valid")
    void caseReferenceNotValid() {
        when(caseReferenceService.validateUID(CASE_REFERENCE)).thenReturn(FALSE);

        assertThrows(BadRequestException.class,
                     () -> documentController.getDocuments(CASE_REFERENCE));
    }

    @Test
    @DisplayName("should propagate exception")
    void shouldPropagateExceptionWhenThrown() {
        when(documentController.getDocuments(CASE_REFERENCE)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class,
                     () -> documentController.getDocuments(CASE_REFERENCE));
    }

}
