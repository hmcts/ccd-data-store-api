package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Set;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

public interface SupplementaryDataRepository {

    void setSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    void incrementSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

    /**
     * Returns Supplementary Data for the case reference requested.
     * @param caseReference         Case reference
     * @param requestedProperties   if requestedProperties is empty or null then returns complete data from
     *                              supplementary_data column, otherwise returns only the fields passed
     *                              in the requestedProperties
     * @return SupplementaryData
     */
    SupplementaryData findSupplementaryData(String caseReference, Set<String> requestedProperties);
}
