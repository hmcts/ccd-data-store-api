package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.UUID;

@Named
@Singleton
public class DocumentIdValidationService {


    /**
     * Validate a UUID string using.
     *
     * @param documentId case document Id
     * @return boolean result for validation
     */

    public boolean validateDocumentUUID(String documentId) {
        if (documentId == null) {
            return false;
        }
        try {
            UUID id = UUID.fromString(documentId);
            if (!id.toString().equals(documentId)) {
                return false;
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return true;
    }
}
