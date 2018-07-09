package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.search.CreatorSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetailsBuilder.aCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
public class SearchQueryOperation {
    private final MergeDataToSearchResultOperation mergeDataToSearchResultOperation;
    private final GetCaseTypesOperation getCaseTypesOperation;
    private final SearchOperation searchOperation;
    private final GetDraftsOperation getDraftsOperation;
    private final CaseTypeService caseTypeService;

    @Autowired
    public SearchQueryOperation(@Qualifier(CreatorSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                                final MergeDataToSearchResultOperation mergeDataToSearchResultOperation,
                                @Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER) final GetCaseTypesOperation
                                        getCaseTypesOperation,
                                @Qualifier(DefaultGetDraftsOperation.QUALIFIER) GetDraftsOperation getDraftsOperation,
                                final CaseTypeService caseTypeService) {
        this.searchOperation = searchOperation;
        this.mergeDataToSearchResultOperation = mergeDataToSearchResultOperation;
        this.getCaseTypesOperation = getCaseTypesOperation;
        this.getDraftsOperation = getDraftsOperation;
        this.caseTypeService = caseTypeService;
    }

    public SearchResultView execute(final String view,
                                    final MetaData metadata,
                                    final Map<String, String> queryParameters) {
        CaseType validCaseType = caseTypeService.getCaseTypeForJurisdiction(metadata.getCaseTypeId(),
            metadata.getJurisdiction());
        Optional<CaseType> caseType = this.getCaseTypesOperation.execute(metadata.getJurisdiction(), CAN_READ)
            .stream()
            .filter(ct -> ct.getId().equalsIgnoreCase(validCaseType.getId()))
            .findFirst();

        if (!caseType.isPresent()) {
            return new SearchResultView(new SearchResultViewColumn[0], new SearchResultViewItem[0]);
        }
        final List<CaseDetails> caseData = searchOperation.execute(metadata, queryParameters);
        List<Draft> caseDrafts = filterCaseTypeDrafts(metadata, getDraftsOperation.execute());
        List<CaseDetails> caseDataFromDrafts = buildCaseDataFromDrafts(caseDrafts);
        caseDataFromDrafts.addAll(caseData);
        return mergeDataToSearchResultOperation.execute(caseType.get(), caseDataFromDrafts, view);
    }

    private List<CaseDetails> buildCaseDataFromDrafts(List<Draft> drafts) {
        return drafts.stream()
            .map(d -> {
                CaseDraft document = d.getDocument();
                return aCaseDetails()
                    .withId(Long.valueOf(d.getId()))
                    .withCaseTypeId(document.getCaseTypeId())
                    .withJurisdiction(document.getJurisdictionId())
                    .withData(document.getCaseDataContent().getData())
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<Draft> filterCaseTypeDrafts(MetaData metadata, List<Draft> draftData) {
        return draftData.stream().filter(draft -> draft.getDocument().getCaseTypeId().equals(metadata.getCaseTypeId())).collect(Collectors.toList());
    }
}
