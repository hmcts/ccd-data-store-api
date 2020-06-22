package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import java.util.List;

public interface SupplementaryDataUserRoleValidator {

    boolean canUpdateSupplementaryData(List<String> userIds);
}
