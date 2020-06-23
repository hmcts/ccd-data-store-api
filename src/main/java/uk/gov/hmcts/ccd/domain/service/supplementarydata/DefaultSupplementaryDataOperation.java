package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SupplementaryDataRepository;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Service
@Qualifier("default")
public class DefaultSupplementaryDataOperation implements SupplementaryDataOperation {

    private final SupplementaryDataRepository supplementaryDataRepository;

    public DefaultSupplementaryDataOperation(final @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    @Override
    public SupplementaryData updateSupplementaryData(String caseReference, SupplementaryData supplementaryData) {
        return this.supplementaryDataRepository.upsert(caseReference, supplementaryData);
    }
}
