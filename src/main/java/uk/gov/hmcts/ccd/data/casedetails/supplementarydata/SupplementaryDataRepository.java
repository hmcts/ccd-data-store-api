package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Set;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataRepository {

    void setSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    void incrementSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    /**
     * Returns Supplementary Data for the case reference requested.
     * @param caseReference         Case reference
     * @param filterFieldPaths      if filterFieldPaths is empty or null then returns complete data from
     *                              supplementary_data column, otherwise returns only the fields passed
     *                              in the filterFieldPaths
     * @return SupplementaryData
     */
    SupplementaryData findSupplementaryData(String caseReference, Set<String> filterFieldPaths);
}
