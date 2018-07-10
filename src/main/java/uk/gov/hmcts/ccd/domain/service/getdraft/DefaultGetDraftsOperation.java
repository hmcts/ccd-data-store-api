package uk.gov.hmcts.ccd.domain.service.getdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import javax.inject.Inject;
import java.util.List;

@Service
@Qualifier(DefaultGetDraftsOperation.QUALIFIER)
public class DefaultGetDraftsOperation implements GetDraftsOperation {
    public static final String QUALIFIER = "default";
    private final DraftGateway draftGateway;

    @Inject
    public DefaultGetDraftsOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway) {
        this.draftGateway = draftGateway;
    }

    @Override
    public List<DraftResponse> execute() {
        return draftGateway.getAll();
    }

}
