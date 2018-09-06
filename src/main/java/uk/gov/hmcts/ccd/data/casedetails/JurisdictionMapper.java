package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.stream.Collectors;

@Named
@Singleton
public class JurisdictionMapper {

    public JurisdictionDisplayProperties toResponse(Jurisdiction jurisdiction) {
        JurisdictionDisplayProperties result = new JurisdictionDisplayProperties();
        result.setId(jurisdiction.getId());
        result.setName(jurisdiction.getName());
        result.setDescription(jurisdiction.getDescription());
        result.setCaseTypes(jurisdiction.getCaseTypes().stream().map(CaseTypeMapper::toResponse).collect(Collectors.toList()));
        return result;
    }
}
