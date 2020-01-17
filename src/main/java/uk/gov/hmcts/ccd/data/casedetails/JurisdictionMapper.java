package uk.gov.hmcts.ccd.data.casedetails;

import javax.inject.Named;
import javax.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

@Named
@Singleton
public class JurisdictionMapper {
    public JurisdictionDisplayProperties toResponse(Jurisdiction jurisdiction) {
        JurisdictionDisplayProperties result = new JurisdictionDisplayProperties();
        result.setId(jurisdiction.getId());
        result.setName(jurisdiction.getName());
        result.setDescription(jurisdiction.getDescription());
        result.setCaseTypes(jurisdiction.getCaseTypes());
        return result;
    }
}
