package uk.gov.hmcts.ccd.data.casedetails;

import java.util.Map;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataRepository {

    void setSupplementaryData(String caseReference, Map<String, Object> supplementaryData);

    void incrementSupplementaryData(String caseReference, Map<String, Object> supplementaryData);

    SupplementaryData findSupplementaryData(String caseReference);
}
