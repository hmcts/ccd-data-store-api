package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataOperation implements SupplementaryDataOperation {

    private SupplementaryDataRepository supplementaryDataRepository;

    public DefaultSupplementaryDataOperation(final @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @Override
    public SupplementaryData updateCaseSupplementaryData(String caseId, SupplementaryData supplementaryData) {
        CaseDetails updatedCaseDetails = this.supplementaryDataRepository.set(null);
        // create a mapper to convert supplementary data frm updatedCaseDetails and return
        return null;
    }
}
