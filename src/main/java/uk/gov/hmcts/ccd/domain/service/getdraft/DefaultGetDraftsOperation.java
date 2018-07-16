package uk.gov.hmcts.ccd.domain.service.getdraft;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetailsBuilder.aCaseDetails;

@Service
@Qualifier(DefaultGetDraftsOperation.QUALIFIER)
public class DefaultGetDraftsOperation implements GetDraftsOperation {
    private static final String PAGE_ONE = "1";
    public static final String QUALIFIER = "default";
    private final DraftGateway draftGateway;

    @Inject
    public DefaultGetDraftsOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway) {
        this.draftGateway = draftGateway;
    }

    @Override
    public List<CaseDetails> execute(final MetaData metadata) {
        List<CaseDetails> casesFromDrafts = Lists.newArrayList();
        if (metadata.getPage().isPresent() && metadata.getPage().get().equals(PAGE_ONE)) {

            List<DraftResponse> caseDrafts = draftGateway.getAll()
                .stream()
                .filter(draftResponse -> hasSameJurisdictionAndCaseType(metadata, draftResponse))
                .collect(Collectors.toList());
            casesFromDrafts = buildCaseDataFromDrafts(caseDrafts);
        }
        return casesFromDrafts;
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
