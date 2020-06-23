package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

public interface SupplementaryDataUserRoleValidator {

    boolean canUpdateSupplementaryData(String caseReference);
}
