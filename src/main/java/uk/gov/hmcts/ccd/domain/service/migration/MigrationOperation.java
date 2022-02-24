package uk.gov.hmcts.ccd.domain.service.migration;

public interface MigrationOperation {

    void backPopulateCaseLinkData(String jurisdiction, String caseTypeId, Long databaseId);

}
