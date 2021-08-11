package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.std.GlobalSearchRequestPayload;

@Service
public interface GlobalSearchService {

    void assembleSearchQuery(GlobalSearchRequestPayload payload);

}
