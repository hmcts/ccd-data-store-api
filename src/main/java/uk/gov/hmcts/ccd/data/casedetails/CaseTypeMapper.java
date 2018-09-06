package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseStateDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseTypeDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class CaseTypeMapper {
    public static CaseTypeDisplayProperties toResponse(CaseType caseType) {
        CaseTypeDisplayProperties result = new CaseTypeDisplayProperties();
        result.setId(caseType.getId());
        result.setName(caseType.getName());
        result.setDescription(caseType.getDescription());
        result.setStates(caseType.getStates().stream().map(CaseStateMapper::toResponse).toArray(CaseStateDisplayProperties[]::new));
        return result;
    }
}
