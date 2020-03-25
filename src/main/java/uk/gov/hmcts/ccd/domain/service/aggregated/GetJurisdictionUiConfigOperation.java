package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;

public interface GetJurisdictionUiConfigOperation {
    List<JurisdictionUiConfig> execute(List<String> jurisdictionReferences);
}
