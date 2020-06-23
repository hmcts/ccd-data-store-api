package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator.SupplementaryDataUserRoleValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseRoleAccessException;
import uk.gov.hmcts.ccd.v2.V2;

@Service
@Qualifier("authorised")
public class AuthorisedSupplementaryDataOperation implements SupplementaryDataOperation {

    private SupplementaryDataOperation supplementaryDataOperation;
    private SupplementaryDataUserRoleValidator roleValidator;

    public AuthorisedSupplementaryDataOperation(@Qualifier("default") SupplementaryDataOperation supplementaryDataOperation,
                                                @Qualifier("default") SupplementaryDataUserRoleValidator roleValidator) {
        this.supplementaryDataOperation = supplementaryDataOperation;
        this.roleValidator = roleValidator;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryData supplementaryData) {
        if (this.roleValidator.canUpdateSupplementaryData(caseReference)) {
            return this.supplementaryDataOperation.updateSupplementaryData(caseReference, supplementaryData);
        }
        throw new CaseRoleAccessException(V2.Error.NOT_AUTHORISED_UPDATE_SUPPLEMENTARY_DATA);
    }
}
