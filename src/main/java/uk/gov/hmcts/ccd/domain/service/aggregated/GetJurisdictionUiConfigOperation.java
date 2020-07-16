package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;

public interface GetJurisdictionUiConfigOperation {
    List<JurisdictionUiConfigDefinition> execute(List<String> jurisdictionReferences);
}
