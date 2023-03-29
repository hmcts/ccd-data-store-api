package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.domain.InvalidCaseSupplementaryDataItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InvalidSupplementaryDataOperation {
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseDetailsToInvalidCaseSupplementaryDataItemMapper mapper;

    @Autowired
    public InvalidSupplementaryDataOperation(final @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                                     CaseDetailsRepository caseDetailsRepository,
                                             CaseDetailsToInvalidCaseSupplementaryDataItemMapper mapper) {
        this.caseDetailsRepository = caseDetailsRepository;
        this.mapper = mapper;
    }

    public List<InvalidCaseSupplementaryDataItem> getInvalidSupplementaryDataCases(List<String> caseTypes,
                                                                                   LocalDateTime from,
                                                                                   Optional<LocalDateTime> to,
                                                                                   Integer limit) {

        List<InvalidCaseSupplementaryDataItem> invalidCases = new ArrayList<>();

        for (String caseType: caseTypes) {
            List<CaseDetails> casesFound = caseDetailsRepository
                .findCasesWithSupplementaryDataHmctsServiceIdButNoOrgsAssignedUsers(
                    caseType, from, to, limit == null ? 10 : limit);

            invalidCases.addAll(mapper.mapToDataItem(casesFound));
        }

        return invalidCases;
    }
}
