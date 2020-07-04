package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Set;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataRepository {

    void setSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    void incrementSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    SupplementaryData findSupplementaryData(String caseReference, Set<String> filterFieldPaths);
}
