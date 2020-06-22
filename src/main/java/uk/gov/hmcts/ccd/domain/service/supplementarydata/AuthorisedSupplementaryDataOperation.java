package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Service
@Qualifier("authorised")
public class AuthorisedSupplementaryDataOperation implements SupplementaryDataOperation {

    private SupplementaryDataOperation supplementaryDataOperation;

    public AuthorisedSupplementaryDataOperation(@Qualifier("default") SupplementaryDataOperation supplementaryDataOperation) {
        this.supplementaryDataOperation = supplementaryDataOperation;
    }

    @Override
    public SupplementaryData updateCaseSupplementaryData(String caseId, SupplementaryData supplementaryData) {
        return this.supplementaryDataOperation.updateCaseSupplementaryData(caseId, supplementaryData);
    }
}
