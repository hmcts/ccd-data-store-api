package uk.gov.hmcts.ccd.data.casedetails;

import javax.inject.Named;
import javax.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;

@Named
@Singleton
public class JurisdictionMapper {
    public JurisdictionDisplayProperties toResponse(JurisdictionDefinition jurisdictionDefinition) {
        JurisdictionDisplayProperties result = new JurisdictionDisplayProperties();
        result.setId(jurisdictionDefinition.getId());
        result.setName(jurisdictionDefinition.getName());
        result.setDescription(jurisdictionDefinition.getDescription());
        result.setCaseTypeDefinitions(jurisdictionDefinition.getCaseTypeDefinitions());
        return result;
    }
}
