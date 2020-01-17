package uk.gov.hmcts.ccd.domain.service.getdraft;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.DraftResponseToCaseDetailsBuilder;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

@Service
@Qualifier(DefaultGetDraftsOperation.QUALIFIER)
public class DefaultGetDraftsOperation implements GetDraftsOperation {
    private static final String PAGE_ONE = "1";
    public static final String QUALIFIER = "default";

    private final DraftGateway draftGateway;
    private DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder;

    @Inject
    public DefaultGetDraftsOperation(@Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                     DraftResponseToCaseDetailsBuilder draftResponseToCaseDetailsBuilder) {
        this.draftGateway = draftGateway;
        this.draftResponseToCaseDetailsBuilder = draftResponseToCaseDetailsBuilder;
    }

    @Override
    public List<CaseDetails> execute(final MetaData metadata) {
        final List<CaseDetails> casesFromDrafts = Lists.newArrayList();
        metadata.getPage().filter(pg -> pg.equals(PAGE_ONE)).ifPresent(pg -> {
            List<DraftResponse> caseDrafts = draftGateway.getAll()
                .stream()
                .filter(draftResponse -> hasSameJurisdictionAndCaseType(metadata, draftResponse))
                .collect(Collectors.toList());
            casesFromDrafts.addAll(buildCaseDataFromDrafts(caseDrafts));
        });
        return casesFromDrafts;
    }

    private boolean hasSameJurisdictionAndCaseType(MetaData metadata, DraftResponse draftResponse) {
        CaseDraft document = draftResponse.getDocument();
        return document.getCaseTypeId().equalsIgnoreCase(metadata.getCaseTypeId())
            && document.getJurisdictionId().equalsIgnoreCase(metadata.getJurisdiction());
    }

    private List<CaseDetails> buildCaseDataFromDrafts(List<DraftResponse> drafts) {
        return drafts.stream()
            .map(draftResponseToCaseDetailsBuilder::build)
            .collect(Collectors.toList());
    }
}
