package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.supplementarydata.SupplementaryDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvalidSupplementaryDataOperation {
    private final SupplementaryDataRepository supplementaryDataRepository;

    @Autowired
    public InvalidSupplementaryDataOperation(SupplementaryDataRepository supplementaryDataRepository) {
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    public List<String> getInvalidSupplementaryDataCases(LocalDateTime from,
                                                         Optional<LocalDateTime> to,
                                                         Integer limit) {
        return supplementaryDataRepository.findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(
            from, to, limit == null ? 10 : limit);
    }
}
