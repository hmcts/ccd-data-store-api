package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
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
    protected static final String NO_ERROR = null;
    private final MergeDataToSearchResultOperation mergeDataToSearchResultOperation;
    private final GetCaseTypesOperation getCaseTypesOperation;
    private final SearchOperation searchOperation;
    private final GetDraftsOperation getDraftsOperation;
    private final CaseTypeService caseTypeService;

    @Autowired
    public SearchQueryOperation(@Qualifier(CreatorSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                                final MergeDataToSearchResultOperation mergeDataToSearchResultOperation,
                                @Qualifier(AuthorisedGetCaseTypesOperation.QUALIFIER) final GetCaseTypesOperation getCaseTypesOperation,
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
            return new SearchResultView(new SearchResultViewColumn[0], new SearchResultViewItem[0], NO_ERROR);
        }

        final List<CaseDetails> cases = searchOperation.execute(metadata, queryParameters);

        String draftResultError = NO_ERROR;
        List<CaseDetails> drafts = Lists.newArrayList();
        try {
            drafts = getDraftsOperation.execute(metadata);
        } catch (DraftAccessException dae) {
            draftResultError = dae.getMessage();
        }
        drafts.addAll(cases);

        return mergeDataToSearchResultOperation.execute(caseType.get(), drafts, view, draftResultError);
    }

    private boolean hasSameJurisdictionAndCaseType(MetaData metadata, DraftResponse draftResponse) {
        CaseDraft document = draftResponse.getDocument();
        return document.getCaseTypeId().equalsIgnoreCase(metadata.getCaseTypeId())
            && document.getJurisdictionId().equalsIgnoreCase(metadata.getJurisdiction());
    }

    private List<CaseDetails> buildCaseDataFromDrafts(List<DraftResponse> drafts) {
        return drafts.stream()
            .map(d -> {
                CaseDraft document = d.getDocument();
                return aCaseDetails()
                    .withId(d.getId())
                    .withCaseTypeId(document.getCaseTypeId())
                    .withJurisdiction(document.getJurisdictionId())
                    .withData(document.getCaseDataContent().getData())
                    .build();
            })
            .collect(Collectors.toList());
    }

}
