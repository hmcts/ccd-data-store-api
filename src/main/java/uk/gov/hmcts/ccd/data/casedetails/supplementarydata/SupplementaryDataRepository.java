package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    List<String> findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(String caseType, LocalDateTime from,
                                                                                    Optional<LocalDateTime> to,
                                                                                    Integer limit);
}
