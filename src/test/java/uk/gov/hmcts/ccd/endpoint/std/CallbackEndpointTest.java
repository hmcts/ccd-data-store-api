package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.stdapi.PrintableDocumentListOperation;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CallbackEndpointTest {
    private static final String JURISDICTION_ID = "Test";
    private static final String CASE_TYPE_ID = "Basic";
    private static final Long CASE_REFERENCE = 1111222233334444L;

    private PrintableDocumentListOperation documentOperation;
    private CallbackEndpoint callbackEndpoint;

    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        documentOperation = mock(PrintableDocumentListOperation.class);
        callbackEndpoint = new CallbackEndpoint(documentOperation);

        caseDetails = new CaseDetails();
        caseDetails.setReference(CASE_REFERENCE);
    }

    @Test
    @DisplayName("should return the Document list retrieved by PrintableDocumentListOperation")
    void getPrintableDocumentList() {
        final Document document = new Document();
        final List<Document> documents = Collections.singletonList(document);
        doReturn(documents).when(documentOperation).getPrintableDocumentList(JURISDICTION_ID, CASE_TYPE_ID,
            caseDetails);
        final List<Document> returnedDocuments = callbackEndpoint.getPrintableDocuments(JURISDICTION_ID, CASE_TYPE_ID,
            caseDetails);

        assertAll(
            () -> assertThat(returnedDocuments, sameInstance(documents)),
            () -> verify(documentOperation).getPrintableDocumentList(JURISDICTION_ID, CASE_TYPE_ID, caseDetails)
        );
    }
}
