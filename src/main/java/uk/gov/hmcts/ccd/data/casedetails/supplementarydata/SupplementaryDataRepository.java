package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

public interface SupplementaryDataRepository {

    void setSupplementaryData(String caseReference, SupplementaryDataUpdateRequest updateRequest);

    void incrementSupplementaryData(String caseReference, SupplementaryDataUpdateRequest updateRequest);

    SupplementaryData findSupplementaryData(String caseReference, SupplementaryDataUpdateRequest updateRequest);
}
