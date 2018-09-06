package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseStateDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class CaseStateMapper {
    public static CaseStateDisplayProperties toResponse(CaseState state) {
        CaseStateDisplayProperties result = new CaseStateDisplayProperties();
        result.setId(state.getId());
        result.setName(state.getName());
        result.setDescription(state.getDescription());
        return result;
    }
}
