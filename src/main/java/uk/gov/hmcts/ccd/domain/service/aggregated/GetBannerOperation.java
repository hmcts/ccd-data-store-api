package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;

public interface GetBannerOperation {
    List<Banner> execute(List<String> jurisdictionReferences);
}
