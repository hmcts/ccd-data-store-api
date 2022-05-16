package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;

import java.util.Set;

@Data
public class CreateCaseEventDetails {

    private CaseDetails caseDetails;

    private Set<AccessProfile> accessProfiles;

    private CaseTypeDefinition caseTypeDefinition;

}
