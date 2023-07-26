package uk.gov.hmcts.ccd.domain.service.createevent;

import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.Set;

@Data
public class CreateCaseEventDetails {

    private CaseDetails caseDetails;

    private Set<AccessProfile> accessProfiles;

    private CaseTypeDefinition caseTypeDefinition;

}
