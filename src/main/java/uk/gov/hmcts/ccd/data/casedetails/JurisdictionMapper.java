package uk.gov.hmcts.ccd.data.casedetails;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.aggregated.lite.CaseTypeLite;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.lite.JurisdictionDisplayPropertiesLite;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;

import java.util.ArrayList;
import java.util.List;

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

    public JurisdictionDisplayPropertiesLite toLiteResponse(JurisdictionDefinition jurisdictionDefinition) {
        JurisdictionDisplayPropertiesLite result = new JurisdictionDisplayPropertiesLite();
        result.setId(jurisdictionDefinition.getId());
        result.setName(jurisdictionDefinition.getName());
        result.setDescription(jurisdictionDefinition.getDescription());
        result.setCaseTypeLiteDefinitions(toCaseTypeResponse(jurisdictionDefinition.getCaseTypeDefinitions()));
        return result;
    }

    public List<CaseTypeLite> toCaseTypeResponse(List<CaseTypeDefinition> caseTypeDefinitions) {
        List<CaseTypeLite> list = new ArrayList<>();
        if (caseTypeDefinitions != null) {
            return caseTypeDefinitions.stream()
                .map(CaseTypeLite::new)
                .toList();
        }
        return list;
    }
}
