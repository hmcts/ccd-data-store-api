package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigResult;

@Service
@Qualifier(DefaultGetJurisdictionUiConfigOperation.QUALIFIER)
public class DefaultGetJurisdictionUiConfigOperation implements GetJurisdictionUiConfigOperation {
    public static final String QUALIFIER = "default";

    private final UIDefinitionRepository repository;

    @Autowired
    public DefaultGetJurisdictionUiConfigOperation(final UIDefinitionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<JurisdictionUiConfigDefinition> execute(List<String> jurisdictionReferences) {
        JurisdictionUiConfigResult jurisdictionUiConfigResult = repository.getJurisdictionUiConfigs(jurisdictionReferences);
        return jurisdictionUiConfigResult.getConfigs();
    }
}
