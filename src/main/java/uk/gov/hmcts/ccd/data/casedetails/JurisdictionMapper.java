package uk.gov.hmcts.ccd.data.casedetails;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseTypeLiteDefinition;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionLiteDisplayProperties;
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

    public JurisdictionLiteDisplayProperties toLiteResponse(JurisdictionDefinition jurisdictionDefinition) {
        JurisdictionLiteDisplayProperties result = new JurisdictionLiteDisplayProperties();
        result.setId(jurisdictionDefinition.getId());
        result.setName(jurisdictionDefinition.getName());
        result.setDescription(jurisdictionDefinition.getDescription());
        result.setCaseTypeDefinitions(toCaseTypeResponse(jurisdictionDefinition.getCaseTypeDefinitions()));
        return result;
    }

    public List<CaseTypeLiteDefinition> toCaseTypeResponse(List<CaseTypeDefinition> caseTypeDefinitions) {
        List<CaseTypeLiteDefinition> list = new ArrayList<>();
        if (caseTypeDefinitions != null) {
            return caseTypeDefinitions.stream()
                .map(CaseTypeLiteDefinition::new)
                .toList();
        }
        return list;
    }
}
