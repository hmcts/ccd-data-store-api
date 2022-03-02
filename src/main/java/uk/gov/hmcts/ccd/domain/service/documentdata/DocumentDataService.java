package uk.gov.hmcts.ccd.domain.service.documentdata;

public interface DocumentDataService {

    void updateDocumentCategoryId(String caseReference, Integer caseVersion, String attributePath, String categoryId);
}
