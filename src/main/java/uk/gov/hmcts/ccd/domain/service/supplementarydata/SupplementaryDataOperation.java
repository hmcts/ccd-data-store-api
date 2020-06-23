package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataOperation {

    SupplementaryData updateSupplementaryData(String caseReference, SupplementaryData supplementaryData);

}
