package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentIdValidationServiceTest {

    private DocumentIdValidationService documentIdValidationService = new DocumentIdValidationService();

    @Test
    public void shouldValidateDocumentID() {
        String documentId = "a780ee98-3136-4be9-bf56-a46f8da1bc97";

        assertTrue(documentIdValidationService.validateDocumentUUID(documentId));
    }

    @Test
    public void shouldReturnFalseForInvalidDocumentId() {
        String documentId = "abcdefghijklmnop";

        assertFalse(documentIdValidationService.validateDocumentUUID(documentId));
    }

    @Test
    public void shouldReturnFalseForMalformedDocumentId() {
        String documentId = "a780ee98-3136-4be9-bf56-a46f8da1bc9@";

        assertFalse(documentIdValidationService.validateDocumentUUID(documentId));
    }

    @Test
    public void shouldReturnFalseForNullDocumentId() {
        String documentId = null;

        assertFalse(documentIdValidationService.validateDocumentUUID(documentId));
    }

    @Test
    public void shouldReturnFalseForWrongDocumentId() {
        String documentId = "a780ee98-3136-4b19-bf56-a46f8da1bc9";

        assertFalse(documentIdValidationService.validateDocumentUUID(documentId));
    }
}
