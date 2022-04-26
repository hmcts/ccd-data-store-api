package uk.gov.hmcts.ccd.domain.service.caselinking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkEntity;
import uk.gov.hmcts.ccd.data.caselinking.CaseLinkRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation.QUALIFIER;

@Service
public class CaseLinkRetrievalService {

    private final CaseLinkRepository caseLinkRepository;
    private final GetCaseOperation getCaseOperation;

    @Autowired
    public CaseLinkRetrievalService(CaseLinkRepository caseLinkRepository,
                                    @Qualifier(QUALIFIER) final GetCaseOperation getCaseOperation) {
        this.caseLinkRepository = caseLinkRepository;
        this.getCaseOperation = getCaseOperation;
    }

    public CaseLinkRetrievalResults getStandardLinkedCases(String caseReference,
                                                           int startRecordNumber,
                                                           int maxNumRecords) {
        final List<Long> linkedStandardCases
            = caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(caseReference),
                CaseLinkEntity.STANDARD_LINK);

        final int adjustedLimit = getAdjustedLimit(maxNumRecords, linkedStandardCases.size());

        final List<Long> paginatedLinkedStandardCases = linkedStandardCases.stream()
            .skip(startRecordNumber - 1L)
            .limit(adjustedLimit)
            .collect(Collectors.toList());

        final List<CaseDetails> caseDetails = paginatedLinkedStandardCases.stream()
            .map(String::valueOf)
            .map(getCaseOperation::execute)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        return CaseLinkRetrievalResults.builder()
            .caseDetails(caseDetails)
            .hasMoreResults(startRecordNumber + adjustedLimit <= linkedStandardCases.size())
            .build();
    }

    private int getAdjustedLimit(int maxNumRecords, int listSize) {
        int adjustedMaxNumRecords = 0;

        if (maxNumRecords > 0) {
            adjustedMaxNumRecords  = (int) Math.round(maxNumRecords * 1.2);
        }

        return adjustedMaxNumRecords > 0 ? adjustedMaxNumRecords : listSize;
    }
}
