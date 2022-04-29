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
import java.util.stream.Stream;

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
        boolean hasMoreResults = false; // default response

        final List<Long> linkedStandardCases
            = caseLinkRepository.findAllByCaseReferenceAndStandardLink(parseLong(caseReference),
                CaseLinkEntity.STANDARD_LINK);

        // add a buffer to the max limit to account for possible cases filtered by AccessControl rules
        final int adjustedLimit = getAdjustedLimit(maxNumRecords, linkedStandardCases.size());

        if (maxNumRecords > 0 && (startRecordNumber + adjustedLimit - 1) < linkedStandardCases.size()) {
            hasMoreResults = true; // i.e. more results left in full list of case references
        }

        final List<Long> paginatedLinkedStandardCases = applyOptionalLimit(adjustedLimit,
            linkedStandardCases.stream().skip(startRecordNumber - 1L)
        );

        // get case details using a service that will apply AccessControl rules
        List<CaseDetails> caseDetails = paginatedLinkedStandardCases.stream()
            .map(String::valueOf)
            .map(getCaseOperation::execute)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        if (maxNumRecords > 0 && caseDetails.size() > maxNumRecords) {
            hasMoreResults = true; // i.e. more results left in buffered list of case details
            // reapply original limit
            caseDetails = applyOptionalLimit(maxNumRecords, caseDetails.stream());
        }

        return CaseLinkRetrievalResults.builder()
            .caseDetails(caseDetails)
            .hasMoreResults(hasMoreResults)
            .build();
    }

    private int getAdjustedLimit(int maxNumRecords, int listSize) {
        int adjustedMaxNumRecords = 0;

        if (maxNumRecords > 0) {
            adjustedMaxNumRecords  = (int) Math.round(maxNumRecords * 1.2);
        }

        return adjustedMaxNumRecords > 0 ? adjustedMaxNumRecords : listSize;
    }

    private <T> List<T> applyOptionalLimit(long maxSize, Stream<T> stream) {
        if (maxSize > 0) {
            return stream.limit(maxSize).collect(Collectors.toList());
        }
        return stream.collect(Collectors.toList());
    }

}
