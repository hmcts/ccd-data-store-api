package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataRepository {

    SupplementaryData upsert(String caseId, SupplementaryData supplementaryData);
}
