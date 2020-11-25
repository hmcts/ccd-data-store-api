package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.BannersResult;

@Service
@Qualifier(DefaultGetBannerOperation.QUALIFIER)
public class DefaultGetBannerOperation implements GetBannerOperation {
    public static final String QUALIFIER = "default";

    private final UIDefinitionRepository repository;

    @Autowired
    public DefaultGetBannerOperation(final UIDefinitionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Banner> execute(List<String> jurisdictionReferences) {
        BannersResult bannersResult = repository.getBanners(jurisdictionReferences);
        return bannersResult.getBanners();
    }
}
